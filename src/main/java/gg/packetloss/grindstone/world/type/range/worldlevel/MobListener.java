/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import gg.packetloss.grindstone.events.guild.GuildCalculateCombatExpEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
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

    private GenericRangedWorldMonster genericMonster;

    public MobListener(WorldLevelComponent parent) {
        this.parent = parent;

        genericMonster = new GenericRangedWorldMonster(parent);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!parent.hasScaledHealth(entity)) {
            return;
        }

        double max = entity.getMaxHealth();

        entity.setMaxHealth(parent.scaleHealth(max, 1, 100));
        entity.setHealth(parent.scaleHealth(max, 1, 100));

        genericMonster.bind((Monster) entity);
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

        if (!(entity instanceof Player) || IGNORED_DAMAGE.contains(event.getCause())) {
            return;
        }

        // If the level > 1, scale the damage to the player
        Player player = (Player) entity;
        int level = parent.getWorldLevel(player);
        if (level == 1) {
            return;
        }

        event.setDamage(event.getDamage() + ChanceUtil.getRandomNTimes(level, 2) - 1);
        if (((Player) entity).isFlying() && event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
            event.setDamage(event.getDamage() * 2);
        }
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
        }

        PvMComponent.printHealth(attacker, defender, (health) -> parent.scaleHealth(health, 100, level));
        return true;
    }

    private static EDBEExtractor<LivingEntity, Player, Projectile> DAMAGED_EXTRACTOR = new EDBEExtractor<>(
        LivingEntity.class,
        Player.class,
        Projectile.class
    );

    public boolean onPlayerDamaged(EntityDamageByEntityEvent event) {
        CombatantPair<LivingEntity, Player, Projectile> result = DAMAGED_EXTRACTOR.extractFrom(event);
        if (result == null) {
            return false;
        }

        // If the attacker doesn't have scaled health, return true to kill the damage processing.
        return !parent.hasScaledHealth(result.getAttacker());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onGuildCombatExpCalculation(GuildCalculateCombatExpEvent event) {
        Player player = event.getPlayer();
        LivingEntity target = event.getTarget();
        if (!parent.hasScaledHealth(target)) {
            return;
        }

        int level = parent.getWorldLevel(player);
        // Normalize the damage for the experience calculation
        event.setDamageDealt(parent.scaleHealth(event.getDamageDealt(), 100, level));
        event.setDamageCap(parent.scaleHealth(event.getDamageCap(), 100, level));
    }
}
