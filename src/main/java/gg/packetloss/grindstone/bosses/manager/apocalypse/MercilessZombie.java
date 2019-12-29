package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import com.skelril.OSBL.util.DamageSource;
import gg.packetloss.grindstone.apocalypse.ApocalypseHelper;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.impl.SimpleRebindableBoss;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

public class MercilessZombie {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private SimpleRebindableBoss<Zombie> mercilessZombie;

    private static final int BASE_MAX_HEALTH = 750;

    public static final String BOUND_NAME = "Merciless Zombie";

    public MercilessZombie() {
        mercilessZombie = new SimpleRebindableBoss<>(BOUND_NAME, Zombie.class, inst, new SimpleInstructionDispatch<>());
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

        if (customName.equals(ThorZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(MercilessZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(StickyZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(ChuckerZombie.BOUND_NAME)) {
            return true;
        }

        return false;
    }

    private static final ParticleBuilder PASSIVE_PARTICLE_EFFECT = new ParticleBuilder(Particle.ENCHANTMENT_TABLE).allPlayers();
    private static final ParticleBuilder REMOVAL_PARTICLE_EFFECT = new ParticleBuilder(Particle.SMOKE_LARGE).allPlayers();

    private void distanceTrap(LivingEntity boss, LivingEntity toHit) {
        toHit.teleport(boss);

        server.getScheduler().runTaskLater(inst, () -> {
            if (boss.isDead()) {
                return;
            }

            if (toHit.getLocation().distanceSquared(boss.getLocation()) > Math.pow(4, 2)) {
                ChatUtil.sendNotice(toHit, "Come back...");
                toHit.damage(1, boss);

                distanceTrap(boss, toHit);
            }
        }, 10);

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
                    ((LivingEntity) anEntity).setMaxHealth(BASE_MAX_HEALTH);
                    ((LivingEntity) anEntity).setHealth(BASE_MAX_HEALTH);

                    // Modify speed
                    try {
                        AttributeBook.setAttribute((LivingEntity) anEntity, AttributeBook.Attribute.MOVEMENT_SPEED, 0.3);
                        AttributeBook.setAttribute((LivingEntity) anEntity, AttributeBook.Attribute.FOLLOW_RANGE, 75);
                    } catch (UnsupportedFeatureException ex) {
                        ex.printStackTrace();
                    }

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
                if (ApocalypseHelper.areDropsSuppressed()) {
                    return null;
                }

                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();

                int maxHealth = (int) ((Zombie) boss).getMaxHealth();
                for (int i = Math.min(60, ChanceUtil.getRandom(maxHealth / 250)); i > 0; --i) {
                    target.getWorld().dropItem(target, new ItemStack(Material.GOLD_INGOT, 64));
                }

                if (ChanceUtil.getChance(10 / (maxHealth / BASE_MAX_HEALTH))) {
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
                ((LivingEntity) toHit).setHealth((int) (((LivingEntity) toHit).getHealth() / 4));
                return null;
            }
        });

        List<DamagedInstruction<GenericDetail>> damagedInstructions = mercilessZombie.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());
        damagedInstructions.add(new DamagedInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, DamagedInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable, DamageSource damageSource, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                LocalEntity localToHit = damageSource.getDamagingEntity();
                if (localToHit == null) return null;
                Entity toHit = BukkitUtil.getBukkitEntity(localToHit);
                if (getEvent(damage).getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
                    ChatUtil.sendWarning(toHit, "Cute.");
                    server.getScheduler().runTaskLater(inst, () -> {
                        distanceTrap((LivingEntity) boss, (LivingEntity) toHit);
                    }, 10);
                }
                return null;
            }
        });

        List<PassiveInstruction<GenericDetail>> passiveInstructions = mercilessZombie.passiveInstructions;
        passiveInstructions.add(new PassiveInstruction<GenericDetail>() {
            @Override
            public InstructionResult<GenericDetail, PassiveInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                if (boss.isDead()) {
                    return null;
                }

                ApocalypseHelper.suppressDrops(() -> {
                    double totalHealth = 5;

                    for (Entity entity : boss.getNearbyEntities(4, 4, 4)) {
                        if (entity == boss) {
                            continue;
                        }

                        if (isConsumableZombie(entity)) {
                            totalHealth += ((Zombie) entity).getHealth();
                            ((Zombie) entity).setHealth(0);
                            continue;
                        }

                        if (entity instanceof Player) {
                            Location pLoc = entity.getLocation();
                            server.getScheduler().runTaskLater(inst, () -> {
                                pLoc.getWorld().strikeLightningEffect(pLoc);
                                for (Player player : pLoc.getNearbyEntitiesByType(Player.class, 2)) {
                                    player.damage(1, boss);
                                }
                            }, 30);
                        }
                    }

                    PASSIVE_PARTICLE_EFFECT
                            .location(boss.getLocation().add(0, 1, 0))
                            .count(15)
                            .spawn();

                    EntityUtil.extendHeal(boss, totalHealth, 15000);
                });

                return null;
            }
        });
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }
}
