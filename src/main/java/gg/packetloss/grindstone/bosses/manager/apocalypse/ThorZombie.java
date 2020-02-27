/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.impl.SimpleRebindableBoss;
import gg.packetloss.grindstone.bosses.instruction.ExplosiveUnbind;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.manager.apocalypse.instruction.ApocalypseDropTableInstruction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

public class ThorZombie {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private SimpleRebindableBoss<Zombie> thorZombie;

    public static final String BOUND_NAME = "Thor Zombie";

    private PerformanceDropTable dropTable = new PerformanceDropTable();

    public ThorZombie() {
        thorZombie = new SimpleRebindableBoss<>(BOUND_NAME, Zombie.class, inst, new SimpleInstructionDispatch<>());
        setupDropTable();
        setupThorZombie();
    }

    public void bind(Damageable entity) {
        thorZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
    }

    private void setupDropTable() {
        dropTable.registerSlicedDrop(
                (info) -> ChanceUtil.getRangedRandom(12, 150),
                (info, consumer) -> {
                    Player player = info.getPlayer();

                    float percentDamage = info.getKillInfo().getPercentDamageDone(player).orElseThrow();
                    int bars = (int) (info.getSlicedPoints() * percentDamage);
                    for (int i = bars; i > 0; --i) {
                        consumer.accept(new ItemStack(Material.GOLD_INGOT));
                    }
                }
        );

        dropTable.registerTakeAllDrop(1000, () -> CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
    }

    private void setupThorZombie() {
        List<BindInstruction<GenericDetail>> bindInstructions = thorZombie.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, BindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure thor zombies are never babies
                    ((Zombie) anEntity).setBaby(false);

                    // Ensure thor zombies cannot pickup items, they are OP enough
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(500);
                    ((LivingEntity) anEntity).setHealth(500);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    ItemStack weapon = new ItemStack(Material.GOLDEN_PICKAXE);
                    weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                    equipment.setItemInMainHand(weapon);
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<GenericDetail>> unbindInstructions = thorZombie.unbindInstructions;
        unbindInstructions.add(new ExplosiveUnbind<>(false, false) {
            @Override
            public float getExplosionStrength(GenericDetail genericDetail) {
                return 4F;
            }
        });
        unbindInstructions.add(new ApocalypseDropTableInstruction<>(dropTable));

        List<DamageInstruction<GenericDetail>> damageInstructions = thorZombie.damageInstructions;
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, DamageInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                final Entity toHit = BukkitUtil.getBukkitEntity(entity);
                toHit.setVelocity(boss.getLocation().getDirection().multiply(2));

                server.getScheduler().runTaskLater(inst, () -> {
                    final Location targetLocation = toHit.getLocation();
                    server.getScheduler().runTaskLater(inst, () -> {
                        targetLocation.getWorld().strikeLightning(targetLocation);
                    }, 15);
                }, 30);
                return null;
            }
        });

        List<DamagedInstruction<GenericDetail>> damagedInstructions = thorZombie.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());
    }
}
