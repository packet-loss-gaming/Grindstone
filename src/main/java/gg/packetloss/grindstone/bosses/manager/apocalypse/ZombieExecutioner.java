/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.apocalypse.ApocalypseHelper;
import gg.packetloss.grindstone.bosses.detail.BossBarDetail;
import gg.packetloss.grindstone.bosses.impl.BossBarRebindableBoss;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.manager.apocalypse.instruction.ApocalypseDropTableInstruction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.dropttable.PerformanceKillInfo;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.*;
import gg.packetloss.openboss.util.AttackDamage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.logging.Logger;

public class ZombieExecutioner {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BossBarRebindableBoss<Zombie> zombieExecutioner;

    public static final String BOUND_NAME = "Zombie Executioner";

    private PerformanceDropTable dropTable = new PerformanceDropTable();

    public ZombieExecutioner() {
        zombieExecutioner = new BossBarRebindableBoss<>(BOUND_NAME, Zombie.class, inst, new SimpleInstructionDispatch<>());
        setupDropTable();
        setupAssassinZombie();
        server.getScheduler().runTaskTimer(
            inst,
            () -> {
                Lists.newArrayList(zombieExecutioner.controlled.values()).forEach((ce) -> {
                    zombieExecutioner.process(ce);
                });
            },
            20 * 10,
            20
        );
    }

    public static boolean is(Zombie zombie) {
        String customName = zombie.getCustomName();
        if (customName == null) {
            return false;
        }

        return customName.equals(BOUND_NAME);
    }
    public void bind(Zombie entity) {
        zombieExecutioner.bind(entity);
    }

    private void setupDropTable() {
        dropTable.registerSlicedDrop(
            (info) -> Math.min(5, ChanceUtil.getRandom((int) info.getTotalDamage() / 50)),
            (info, consumer) -> {
                Player player = info.getPlayer();

                // Get the point information
                int points = info.getSlicedPoints();
                PerformanceKillInfo killInfo = info.getKillInfo();
                float percentDamageDone = killInfo.getPercentDamageDone(player).orElseThrow();

                // Calculate and distribute the point slice
                for (int i = (int) (points * percentDamageDone); i > 0; --i) {
                    consumer.accept(new ItemStack(Material.GOLD_INGOT, 10));
                }
            }
        );

        dropTable.registerTakeAllDrop(1000, () -> CustomItemCenter.build(CustomItems.EXECUTIONER_AXE));
    }

    private void setupAssassinZombie() {
        List<BindInstruction<BossBarDetail>> bindInstructions = zombieExecutioner.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<BossBarDetail, BindInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure assassin zombies are never babies
                    ((Zombie) anEntity).setBaby(false);

                    // Ensure assassin zombies cannot pickup items, they are OP enough
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(775);
                    ((LivingEntity) anEntity).setHealth(775);

                    // Set speed and follow range
                    EntityUtil.setMovementSpeed((LivingEntity) anEntity, 0.33);
                    EntityUtil.setFollowRange((LivingEntity) anEntity, 75);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    ItemStack weapon = new ItemStack(Material.GOLDEN_AXE);
                    weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                    equipment.setItemInMainHand(weapon);
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<BossBarDetail>> unbindInstructions = zombieExecutioner.unbindInstructions;
        unbindInstructions.add(new ApocalypseDropTableInstruction<>(dropTable));

        List<DamageInstruction<BossBarDetail>> damageInstructions = zombieExecutioner.damageInstructions;
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<BossBarDetail, DamageInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                EntityUtil.heal(boss, damage.getDamage());
                return null;
            }
        });

        List<DamagedInstruction<BossBarDetail>> damagedInstructions = zombieExecutioner.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());

        List<PassiveInstruction<BossBarDetail>> passiveInstructions = zombieExecutioner.passiveInstructions;
        passiveInstructions.add(new PassiveInstruction<>() {
            @Override
            public InstructionResult<BossBarDetail, PassiveInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                if (boss.isDead()) {
                    return null;
                }

                int totalAllies = 0;
                for (Zombie zombie : boss.getLocation().getNearbyEntitiesByType(Zombie.class, 10)) {
                    if (zombie.isDead()) {
                        continue;
                    }

                    if (zombie == boss) {
                        continue;
                    }

                    if (!ApocalypseHelper.checkEntity(zombie)) {
                        continue;
                    }

                    ++totalAllies;
                }

                int finalCount = totalAllies;
                if (finalCount > 0) {
                    ((Zombie) boss).addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20, finalCount));
                    ((Zombie) boss).addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20, finalCount));
                }

                TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();
                taskBuilder.setNumberOfRuns(20);
                taskBuilder.setAction((times) -> {
                    if (boss.isDead()) {
                        return true;
                    }

                    for (int i = 0; i < finalCount * 2.5; ++i) {
                        Location bossLoc = boss.getLocation();
                        bossLoc.getWorld().spawnParticle(
                            Particle.SMOKE_LARGE,
                            bossLoc.getX() + ChanceUtil.getRangedRandom(-.5, .5),
                            bossLoc.getY(),
                            bossLoc.getZ() + ChanceUtil.getRangedRandom(-.5, .5),
                            0,
                            ChanceUtil.getRangedRandom(-.1, .1),
                            .1,
                            ChanceUtil.getRangedRandom(-.1, .1)
                        );
                    }

                    return true;
                });
                taskBuilder.build();

                return null;
            }
        });
    }
}
