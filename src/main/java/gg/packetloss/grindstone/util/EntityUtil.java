/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.EntityEffect;
import org.bukkit.entity.*;

public class EntityUtil {
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

    public static void forceDamage(Entity entity, double amt) {
        // Check for validity
        if (entity == null || !entity.isValid()) {
            return;
        }

        // Check living
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        // Check damageable
        if (entity instanceof Player && GeneralPlayerUtil.isInvulnerable((Player) entity)) {
            return;
        }

        double cur = ((LivingEntity) entity).getHealth();

        ((LivingEntity) entity).setHealth(Math.max(cur - amt, 0));
        entity.playEffect(EntityEffect.HURT);
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
}
