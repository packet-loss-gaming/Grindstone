/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.EntityHealthInContextEvent;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.UUID;

import static gg.packetloss.grindstone.events.EntityHealthInContextEvent.HealthKind.CURRENT;
import static gg.packetloss.grindstone.events.EntityHealthInContextEvent.HealthKind.MAX;

public class EntityUtil {

    public static double getHealth(Player context, LivingEntity entity) {
        var event = new EntityHealthInContextEvent(context, entity, CURRENT);
        CommandBook.callEvent(event);
        return event.getValue();
    }

    public static double getMaxHealth(Player context, LivingEntity entity) {
        var event = new EntityHealthInContextEvent(context, entity, MAX);
        CommandBook.callEvent(event);
        return event.getValue();
    }

    public static double getHealth(LivingEntity context, LivingEntity entity) {
        if (context instanceof Player) {
            return getHealth((Player) context, entity);
        }
        return entity.getHealth();
    }

    public static double getMaxHealth(LivingEntity context, LivingEntity entity) {
        if (context instanceof Player) {
            return getMaxHealth((Player) context, entity);
        }
        return entity.getMaxHealth();
    }

    public static double upscaleDamage(Player context, LivingEntity entity, double damageToEntity) {
        var event = new EntityHealthInContextEvent(context, entity, damageToEntity, true);
        CommandBook.callEvent(event);
        return event.getValue();
    }

    public static double descaleDamage(Player context, LivingEntity entity, double damageToEntity) {
        var event = new EntityHealthInContextEvent(context, entity, damageToEntity, false);
        CommandBook.callEvent(event);
        return event.getValue();
    }

    public static boolean nameMatches(Entity entity, String name) {
        String customName = entity.getCustomName();
        return customName != null && customName.equals(name);
    }

    public static void heal(Entity entity, double amt) {
        if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity)) return;
        double cur = ((LivingEntity) entity).getHealth();
        double max = ((LivingEntity) entity).getMaxHealth();

        ((LivingEntity) entity).setHealth(Math.min(cur + amt, max));
    }

    public static void extendHeal(Entity entity, double amt, double maxHealth) {
        if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity)) return;
        double cur = ((LivingEntity) entity).getHealth();

        // Clamp to the max health to fix a bug where max health runs away
        double curMax = Math.min(maxHealth, ((LivingEntity) entity).getMaxHealth());

        double amtToHeal = Math.min(cur + amt, maxHealth);

        ((LivingEntity) entity).setMaxHealth(Math.max(amtToHeal, curMax));
        ((LivingEntity) entity).setHealth(amtToHeal);
    }

    private static void forceDamageNow(Entity entity, double amt) {
        // Check for validity
        if (entity == null || !entity.isValid()) {
            return;
        }

        // Check living
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        // Check damageable
        if (living instanceof Player && GeneralPlayerUtil.hasInvulnerableGamemode((Player) living)) {
            return;
        }

        // Update Absorption
        double curAbsorption = living.getAbsorptionAmount();
        double absorptionRemoved = Math.min(curAbsorption, amt);
        living.setAbsorptionAmount(curAbsorption - absorptionRemoved);
        amt -= absorptionRemoved;

        // Update base health
        double cur = living.getHealth();
        living.setHealth(Math.max(cur - amt, 0));

        // Play damage effect
        living.playEffect(EntityEffect.HURT);
    }

    public static void forceDamage(Entity entity, double amt) {
        // Process this out of band if applying force damage to a player.
        //
        // This prevents double deaths if we're coming from a EntityDamageEvent and this kills the player (the
        // original damage event would then become a secondary "kill"/death).
        if (entity instanceof Player) {
            CommandBook.server().getScheduler().runTask(CommandBook.inst(), () -> forceDamageNow(entity, amt));
        } else {
            forceDamageNow(entity, amt);
        }
    }

    public static void forceDamage(Player context, LivingEntity entity, double amt) {
        forceDamage(entity, upscaleDamage(context, entity, amt));
    }

    public static void forceDamage(LivingEntity context, LivingEntity entity, double amt) {
        if (context instanceof Player) {
            forceDamage((Player) context, entity, amt);
            return;
        }
        forceDamage(entity, amt);
    }

    public static boolean isHostileMob(Entity entity) {
        if (entity instanceof Monster) {
            return true;
        }

        // Slime (and descendants) are not considered monsters
        if (entity instanceof Slime) {
            return true;
        }

        // Flying (Phantom, Ghasts) are not considered monsters
        if (entity instanceof Flying) {
            return true;
        }

        // Skeleton Horses are not considered monsters
        if (entity instanceof SkeletonHorse) {
            return true;
        }

        // Hoglin are not considered monsters
        if (entity instanceof Hoglin) {
            return true;
        }

        return false;
    }

    public static boolean isHostileMobOrPlayer(Entity entity) {
        return isHostileMob(entity) || entity instanceof Player;
    }

    public static boolean willFollowOwner(Entity entity) {
        if (entity instanceof Wolf) {
            return true;
        }

        if (entity instanceof Cat) {
            return true;
        }

        if (entity instanceof Parrot) {
            return true;
        }

        return false;
    }

    public static void protectDrop(Item item, UUID playerID) {
        item.setOwner(playerID);

        // Prevent environmental shenanigans
        item.setInvulnerable(true);
        item.setCanMobPickup(false);
    }

    public static Item spawnProtectedItem(ItemStack stack, UUID playerID, Location destination) {
        Item item = destination.getWorld().dropItem(destination, stack);
        protectDrop(item, playerID);
        return item;
    }

    public static Item spawnProtectedItem(ItemStack stack, Player player, Location destination) {
        return spawnProtectedItem(stack, player.getUniqueId(), destination);
    }

    public static Item spawnProtectedItem(ItemStack stack, Player player) {
        return spawnProtectedItem(stack, player, player.getLocation());
    }

    public static void setMovementSpeed(LivingEntity entity, double speed) {
        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(speed);
    }

    public static void setKnockbackResistance(LivingEntity entity, double resistance) {
        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).setBaseValue(resistance);
    }

    public static void setAttackKnockback(LivingEntity entity, double knockback) {
        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK)).setBaseValue(knockback);
    }

    public static double getFollowRange(LivingEntity entity) {
        return Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).getValue();
    }

    public static void setFollowRange(LivingEntity entity, double followRange) {
        Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(followRange);
    }
}
