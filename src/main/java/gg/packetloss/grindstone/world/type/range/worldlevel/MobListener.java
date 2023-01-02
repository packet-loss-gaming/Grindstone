/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.EntityHealthInContextEvent;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.world.type.city.combat.PvMComponent;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashSet;
import java.util.Set;

class MobListener implements Listener {
    private WorldLevelComponent parent;

    private GenericRangeWorldMonster genericMonster;

    public MobListener(WorldLevelComponent parent) {
        this.parent = parent;

        genericMonster = new GenericRangeWorldMonster(parent);
    }

    @EventHandler
    public void onEntityHealthInContextRequest(EntityHealthInContextEvent event) {
        LivingEntity target = event.getTarget();
        if (!parent.hasScaledHealth(target)) {
            return;
        }

        Player player = event.getPlayer();
        int level = parent.getWorldLevel(player);
        if (event.getKind().isDescale()) {
            event.setValue(parent.scaleHealth(event.getValue(), 100, level));
        } else {
            event.setValue(parent.scaleHealth(event.getValue(), level, 100));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!parent.isRangeWorld(entity.getWorld())) {
            return;
        }

        CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> {
            if (!parent.hasScaledHealth(entity)) {
                return;
            }

            double max = entity.getMaxHealth();

            entity.setMaxHealth(parent.scaleHealth(max, 1, 100));
            entity.setHealth(parent.scaleHealth(max, 1, 100));

            genericMonster.bind((Mob) entity);
        });
    }

    private int previousSourceDamageLevel;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageBegin(EntityDamageEvent event) {
        previousSourceDamageLevel = parent.sourceDamageLevel;

        // Reset whether or not damage was modified, if it was that will be set when we do the modification
        // later in the combat cycle.
        parent.sourceDamageLevel = 0;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageEnd(EntityDamageEvent event) {
        if (event.isCancelled()) {
            // Restore whatever the original value was before this even cycle started, this attack doesn't
            // factor into particles.
            parent.sourceDamageLevel = previousSourceDamageLevel;
        }
    }

    private static Set<EntityDamageEvent.DamageCause> IGNORED_DAMAGE = new HashSet<>();

    static {
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.WITHER);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.FIRE_TICK);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.POISON);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.CONTACT);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.DROWNING);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.FALL);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.STARVATION);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.SUFFOCATION);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.VOID);
        // Note while explosions are modified for general purposes,
        // creeper explosions are modified in the onPlayerDamaged handler.
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
        IGNORED_DAMAGE.add(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        if (!parent.isRangeWorld(location.getWorld())) {
            return;
        }

        if (event instanceof EntityDamageByEntityEvent) {
            if (onPlayerDamage((EntityDamageByEntityEvent) event)) {
                return;
            }

            if (onPlayerDamaged((EntityDamageByEntityEvent) event)) {
                return;
            }
        }

        // Make Skeletons & Zombies burn faster
        if (entity instanceof Zombie || entity instanceof Skeleton) {
            if (event.getCause().equals(EntityDamageEvent.DamageCause.FIRE_TICK)
                && location.getBlock().getLightFromSky() == 15) {
                event.setDamage(parent.scaleHealth(event.getDamage(), 1, 100));
                return;
            }
        }

        if (!(entity instanceof Player player) || IGNORED_DAMAGE.contains(event.getCause())) {
            return;
        }

        int level = parent.getWorldLevel(player);
        if (level == 1) {
            return;
        }

        event.setDamage(WorldLevelComponent.scaleDamageForLevel(event.getDamage(), level));
    }

    private static EDBEExtractor<Player, LivingEntity, Projectile> DAMAGE_EXTRACTOR = new EDBEExtractor<>(
        Player.class,
        LivingEntity.class,
        Projectile.class
    );

    public boolean onPlayerDamage(EntityDamageByEntityEvent event) {
        CombatantPair<Player, LivingEntity, Projectile> result = DAMAGE_EXTRACTOR.extractFrom(event);
        if (result == null) {
            return false;
        }

        final Player attacker = result.getAttacker();
        LivingEntity defender = result.getDefender();

        // The defender is a player, we don't want to print the HP or modify the damage,
        // but at the same time we don't want to allow the processing of onEntityDamage to continue,
        // so we return true instead of false to end the cycle.
        if (defender instanceof Player) {
            return true;
        }

        int level = parent.getWorldLevel(attacker);
        if (parent.hasScaledHealth(defender)) {
            parent.sourceDamageLevel = level;
            event.setDamage(parent.scaleHealth(event.getDamage(), level, 100));

            // Don't print health for non-damage scaled entities as this causes double printing in some cases
            // e.g the apocalypse.
            PvMComponent.printHealth(attacker, defender);
        }

        return true;
    }

    private static EDBEExtractor<LivingEntity, Player, Projectile> DAMAGED_EXTRACTOR = new EDBEExtractor<>(
        LivingEntity.class,
        Player.class,
        Projectile.class
    );

    public boolean onPlayerDamaged(EntityDamageByEntityEvent event) {
        CombatantPair<LivingEntity, Player, Projectile> result = DAMAGED_EXTRACTOR.extractFrom(event);
        // The pattern didn't match, revert to default damage processing.
        if (result == null) {
            return false;
        }

        // If the attacker doesn't have scaled health, return true to kill the damage processing.
        if (!parent.hasScaledHealth(result.getAttacker())) {
            return true;
        }

        Player player = result.getDefender();

        int level = parent.getWorldLevel(player);
        if (level == 1) {
            return true;
        }

        if (result.getAttacker() instanceof Creeper) {
            int modifyEveryXLevels = parent.getConfig().mobsCreeperExplosionMultiplyEveryXLevels;
            // Reduce by 1 level and add the multiplier interval.
            //
            // If the constant is 10, this should mean that level 2 results in an 11 / 10 modifier (1.1x).
            int adjustedLevel = ((level - 1) + modifyEveryXLevels);
            event.setDamage(event.getDamage() * ((double) adjustedLevel / modifyEveryXLevels));
            return true;
        }

        if (player.isFlying() && result.hasProjectile()) {
            if (level >= parent.getConfig().mobsLightningProjectilesLevelEnabledAt) {
                // Strike with real lightning
                player.getWorld().strikeLightning(player.getLocation());
                // Reset no damage ticks for combo with the currently processed event.
                player.setNoDamageTicks(0);

                // Make the player fall
                GeneralPlayerUtil.takeFlightSafely(player);
            }
        }

        return true;
    }
}
