package gg.packetloss.grindstone.bosses;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

public class MercilessZombie {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<GenericDetail> mercilessZombie;

    public static final String BOUND_NAME = "Merciless Zombie";

    public MercilessZombie() {
        mercilessZombie = new BukkitBossDeclaration<>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), BOUND_NAME);
            }
        };
        setupMercilessZombie();
        server.getScheduler().runTaskTimer(
                inst,
                () -> {
                    Lists.newArrayList(mercilessZombie.controlled.values()).forEach((ce) -> mercilessZombie.process(ce));
                },
                20 * 10,
                20 * 2
        );
    }

    public void bind(Damageable entity) {
        mercilessZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
    }

    private boolean isConsumableZombie(Entity entity) {
        String customName = entity.getCustomName();
        if (customName == null) {
            return false;
        }

        if (!(entity instanceof Zombie)) {
            return false;
        }

        if (customName.equals("Apocalyptic Zombie")) {
            return true;
        }

        if (customName.equals("Grave Zombie")) {
            return true;
        }

        return false;
    }

    private void setupMercilessZombie() {
        List<BindInstruction<GenericDetail>> bindInstructions = mercilessZombie.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, BindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure merciless zombies are never babies
                    ((Zombie) anEntity).setBaby(false);

                    // Ensure merciless zombies cannot pickup items, they are OP enough
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(500);
                    ((LivingEntity) anEntity).setHealth(500);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    ItemStack weapon = new ItemStack(Material.GOLD_SWORD);
                    weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                    equipment.setItemInMainHand(weapon);
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<GenericDetail>> unbindInstructions = mercilessZombie.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, UnbindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();

                int maxHealth = (int) ((Zombie) boss).getMaxHealth();
                for (int i = ChanceUtil.getRandom(maxHealth / 64); i > 0; --i) {
                    target.getWorld().dropItem(target, new ItemStack(Material.GOLD_INGOT, 64));
                }

                if (ChanceUtil.getChance(15)) {
                    target.getWorld().dropItem(target, CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
                }

                return null;
            }
        });

        List<DamageInstruction<GenericDetail>> damageInstructions = mercilessZombie.damageInstructions;
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, DamageInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable, LocalEntity entity, AttackDamage damage) {
                final Entity toHit = BukkitUtil.getBukkitEntity(entity);
                ((LivingEntity) toHit).setHealth((int) (((LivingEntity) toHit).getHealth() / 2));
                return null;
            }
        });

        List<DamagedInstruction<GenericDetail>> damagedInstructions = mercilessZombie.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());

        List<PassiveInstruction<GenericDetail>> passiveInstructions = mercilessZombie.passiveInstructions;
        passiveInstructions.add(new PassiveInstruction<GenericDetail>() {
            @Override
            public InstructionResult<GenericDetail, PassiveInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);

                // FIXME: This needs fixed in open boss, this just works around the issue
                if (!boss.isValid()) {
                    mercilessZombie.silentUnbind(controllable);
                    return null;
                }

                double totalHealth = 0;

                for (Entity entity : ((Zombie) boss).getNearbyEntities(4, 4, 4)) {
                    if (isConsumableZombie(entity)) {
                        totalHealth += ((Zombie) entity).getHealth();
                        ((Zombie) entity).setHealth(0);
                    }
                }

                EntityUtil.extendHeal((Zombie) boss, totalHealth);
                return null;
            }
        });
    }
}
