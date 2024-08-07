/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.apocalypse.ApocalypseHelper;
import gg.packetloss.grindstone.bosses.detail.BossBarDetail;
import gg.packetloss.grindstone.bosses.impl.BossBarRebindableBoss;
import gg.packetloss.grindstone.bosses.manager.apocalypse.instruction.ApocalypseDropTableInstruction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.NumericPipeline;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.dropttable.PerformanceKillInfo;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.openboss.bukkit.entity.BukkitEntity;
import gg.packetloss.openboss.bukkit.util.BukkitAttackDamage;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.*;
import gg.packetloss.openboss.util.AttackDamage;
import gg.packetloss.openboss.util.DamageSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class MercilessZombie {
    private BossBarRebindableBoss<Zombie> mercilessZombie;

    public static final String BOUND_NAME = "Merciless Zombie";
    public static final int MIN_HEALTH = 750;
    public static final int MAX_ACHIEVABLE_HEALTH = 15000;

    private PerformanceDropTable dropTable = new PerformanceDropTable();

    public MercilessZombie() {
        mercilessZombie = new BossBarRebindableBoss<>(BOUND_NAME, Zombie.class, CommandBook.inst(), new SimpleInstructionDispatch<>());
        setupDropTable();
        setupMercilessZombie();
        Bukkit.getScheduler().runTaskTimer(
                CommandBook.inst(),
                () -> {
                    Lists.newArrayList(mercilessZombie.controlled.values()).forEach((ce) -> {
                        mercilessZombie.process(ce);
                    });
                },
                20 * 10,
                20 * 2
        );
    }

    public void bind(Zombie entity) {
        mercilessZombie.bind(entity);
    }

    public static boolean is(Zombie zombie) {
        String customName = zombie.getCustomName();
        if (customName == null) {
            return false;
        }

        return customName.equals(BOUND_NAME);
    }

    private boolean isMercilessZombie(Entity entity) {
        String customName = entity.getCustomName();
        return BOUND_NAME.equals(customName);
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

        if (customName.equals(StickyZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(ChuckerZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(ZombieExecutioner.BOUND_NAME)) {
            return true;
        }

        return false;
    }

    private static final ParticleBuilder PASSIVE_PARTICLE_EFFECT = new ParticleBuilder(Particle.ENCHANT).allPlayers();

    private void distanceTrap(LivingEntity boss, LivingEntity toHit) {
        toHit.teleport(boss);

        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            if (boss.isDead()) {
                return;
            }

            if (toHit.getLocation().distanceSquared(boss.getLocation()) > Math.pow(4, 2)) {
                ChatUtil.sendNotice(toHit, "Come back...");
                if (toHit.isValid()) {
                    toHit.damage(1, boss);

                    distanceTrap(boss, toHit);
                }
            }
        }, 10);

    }

    private void setupDropTable() {
        dropTable.registerSlicedDrop(
                (info) -> Math.min(60, ChanceUtil.getRandom((int) info.getTotalDamage() / 250)),
                (info, consumer) -> {
                    Player player = info.getPlayer();

                    // Get the point information
                    int points = info.getSlicedPoints();
                    PerformanceKillInfo killInfo = info.getKillInfo();
                    float percentDamageDone = killInfo.getPercentDamageDone(player).orElseThrow();

                    // Calculate and distribute the point slice
                    for (int i = (int) (points * percentDamageDone); i > 0; --i) {
                        consumer.accept(new ItemStack(Material.GOLD_INGOT, 64));
                    }
                }
        );

        NumericPipeline.Builder<PerformanceKillInfo> healthAffectedChance = NumericPipeline.builder();
        healthAffectedChance.accept((info, chance) -> {
            return chance / (int) (info.getKilled().getMaxHealth() / MIN_HEALTH);
        });

        dropTable.registerTakeAllDrop(
                healthAffectedChance.build(10),
                () -> CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER)
        );
    }

    private void setupMercilessZombie() {
        List<BindInstruction<BossBarDetail>> bindInstructions = mercilessZombie.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<BossBarDetail, BindInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure merciless zombies are never babies
                    ((Zombie) anEntity).setBaby(false);

                    // Ensure merciless zombies cannot pickup items, they are OP enough
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(MIN_HEALTH);
                    ((LivingEntity) anEntity).setHealth(MIN_HEALTH);

                    // Modify speed and follow range
                    EntityUtil.setMovementSpeed((LivingEntity) anEntity, 0.3);
                    EntityUtil.setFollowRange((LivingEntity) anEntity, 75);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    ItemStack weapon = new ItemStack(Material.GOLDEN_SWORD);
                    weapon.addUnsafeEnchantment(Enchantment.SHARPNESS, 1);
                    equipment.setItemInMainHand(weapon);
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<BossBarDetail>> unbindInstructions = mercilessZombie.unbindInstructions;
        unbindInstructions.add(new ApocalypseDropTableInstruction<>(dropTable));

        List<DamageInstruction<BossBarDetail>> damageInstructions = mercilessZombie.damageInstructions;
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<BossBarDetail, DamageInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable, LocalEntity entity, AttackDamage damage) {
                final Entity toHit = BukkitUtil.getBukkitEntity(entity);
                EntityUtil.forceDivideHealth((LivingEntity) toHit, 4);
                return null;
            }
        });

        List<DamagedInstruction<BossBarDetail>> damagedInstructions = mercilessZombie.damagedInstructions;
        damagedInstructions.add(new DamagedInstruction<>() {
            @Override
            public InstructionResult<BossBarDetail, DamagedInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable, DamageSource damageSource, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                LocalEntity localToHit = damageSource.getDamagingEntity();
                if (localToHit == null) return null;
                Entity toHit = BukkitUtil.getBukkitEntity(localToHit);
                if (getEvent(damage).getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
                    ChatUtil.sendWarning(toHit, "Cute.");
                    Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                        distanceTrap((LivingEntity) boss, (LivingEntity) toHit);
                    }, 10);
                }
                return null;
            }
        });

        List<PassiveInstruction<BossBarDetail>> passiveInstructions = mercilessZombie.passiveInstructions;
        passiveInstructions.add(new PassiveInstruction<BossBarDetail>() {
            @Override
            public InstructionResult<BossBarDetail, PassiveInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable) {
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

                        boolean isMerciless = isMercilessZombie(entity);

                        // Merge damage if we're eating another merciless
                        if (isMerciless) {
                            LocalControllable<BossBarDetail> consumed = mercilessZombie.getBound(new BukkitEntity<>(entity));
                            for (UUID damager : consumed.getDamagers()) {
                                controllable.damaged(damager, consumed.getDamage(damager).orElseThrow());
                            }
                        }

                        if (isMerciless || isConsumableZombie(entity)) {
                            totalHealth += ((Zombie) entity).getHealth();
                            ((Zombie) entity).setHealth(0);
                            continue;
                        }

                        if (entity instanceof Player && ((Player) entity).hasLineOfSight(boss)) {
                            Location pLoc = entity.getLocation();
                            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                                pLoc.getWorld().strikeLightningEffect(pLoc);
                                for (Player player : pLoc.getNearbyEntitiesByType(Player.class, 2)) {
                                    if (player.isValid()) {
                                        player.damage(1, boss);
                                    }
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
