/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.listener.combatwatchdog;

import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.collection.FiniteCache;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class UnbalancedCombatWatchdog implements Listener {
    private final Predicate<World> appliesTo;
    private final Map<UUID, PlayerCombatStats> playerPlayerCombatStats = new HashMap<>();
    private FiniteCache<Location> recentEndermiteSpawnLocations = new FiniteCache<>(5);

    public UnbalancedCombatWatchdog(Predicate<World> appliesTo) {
        this.appliesTo = appliesTo;
    }

    private PlayerCombatStats getPlayerCombatStats(Player player) {
        return playerPlayerCombatStats.computeIfAbsent(player.getUniqueId(), (key) -> new PlayerCombatStats());
    }

    private boolean hasUnbalancedCombatRatio(PlayerCombatStats combatStats) {
        return combatStats.getCombatRatio() > 30 && combatStats.getCombatFactors() > 35;
    }

    private static final long MAX_TIME = TimeUnit.MINUTES.toMillis(5);

    private boolean areCombatStatsStale(PlayerCombatStats combatStats) {
        return System.currentTimeMillis() - combatStats.getLastCombatStatsReset() >= MAX_TIME;
    }

    private Deque<LivingEntity> spawnEndermitesAt(Location location) {
        int numToSpawn = ChanceUtil.getRandom(5);

        Deque<LivingEntity> spawned = new ArrayDeque<>(numToSpawn);
        for (int i = numToSpawn; i > 0; --i) {
            spawned.add(location.getWorld().spawn(location, Endermite.class));
        }
        recentEndermiteSpawnLocations.add(location);

        return spawned;
    }

    private void seekEndermitesOnPlayer(Player player, Deque<LivingEntity> endermites) {
        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setDelay(40);
        taskBuilder.setInterval(20);

        taskBuilder.setNumberOfRuns(endermites.size());

        taskBuilder.setAction((times) -> {
            Entity mob = endermites.poll();
            if (mob.isValid() && appliesTo.test(player.getWorld())) {
                mob.teleport(player.getLocation());
            }
            return true;
        });

        taskBuilder.build();
    }

    private void spawnEndermiteSwarm(Player attacker, LivingEntity defender) {
        Deque<LivingEntity> spawned = spawnEndermitesAt(defender.getLocation());
        seekEndermitesOnPlayer(attacker, spawned);
    }

    private static final EDBEExtractor<LivingEntity, LivingEntity, Projectile> COMBAT_EXTRACTOR = new EDBEExtractor<>(
        LivingEntity.class,
        LivingEntity.class,
        Projectile.class
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        World world = event.getEntity().getWorld();
        if (!appliesTo.test(world)) {
            return;
        }

        // Use an EDBEExtractor to handle projectile processing.
        CombatantPair<LivingEntity, LivingEntity, Projectile> result = COMBAT_EXTRACTOR.extractFrom(event);
        if (result == null) {
            return;
        }

        LivingEntity attacker = result.getAttacker();
        LivingEntity defender = result.getDefender();

        if (attacker instanceof Player playerAttacker && EntityUtil.isHostileMob(defender)) {
            if (GeneralPlayerUtil.hasInvulnerableGamemode(playerAttacker)) {
                return;
            }

            PlayerCombatStats combatStats = getPlayerCombatStats(playerAttacker);
            if (areCombatStatsStale(combatStats)) {
                combatStats.resetCombatStats();
            } else {
                combatStats.markOffensiveCombat();

                if (hasUnbalancedCombatRatio(combatStats)) {
                    spawnEndermiteSwarm(playerAttacker, defender);
                }
            }
        } else if (EntityUtil.isHostileMob(attacker) && defender instanceof Player playerDefender) {
            if (GeneralPlayerUtil.hasInvulnerableGamemode(playerDefender)) {
                return;
            }

            getPlayerCombatStats(playerDefender).markDefensiveCombat();

            if (attacker instanceof Endermite) {
                // Replace endermite damage with force damage
                event.setDamage(0);
                EntityUtil.forceDamage(defender, ChanceUtil.getRandom(3));

                // Endermites are occasionally poisonous
                if (ChanceUtil.getChance(5)) {
                    defender.removePotionEffect(PotionEffectType.POISON);
                    defender.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 30 * 20, 1));
                }
            }
        }
    }

    private boolean isNearAnyRecentSpawnPoint(Entity entity) {
        Location entityLoc = entity.getLocation();
        for (Location loc : recentEndermiteSpawnLocations) {
            if (loc == null) {
                continue;
            }

            if (LocationUtil.isWithin2DDistance(loc, entityLoc, 10)) {
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!appliesTo.test(entity.getWorld())) {
            return;
        }

        if (entity instanceof Endermite && ChanceUtil.getChance(20) && isNearAnyRecentSpawnPoint(entity)) {
            ExplosionStateFactory.createExplosion(
                entity.getLocation(),
                4F,
                false,
                true
            );
        }
    }
}
