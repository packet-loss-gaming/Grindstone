/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.EntityEffect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

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
        double curMax = ((LivingEntity) entity).getMaxHealth();

        double amtToHeal = Math.min(cur + amt, maxHealth);

        ((LivingEntity) entity).setMaxHealth(Math.max(amtToHeal, curMax));
        ((LivingEntity) entity).setHealth(amtToHeal);
    }

    public static void forceDamage(Entity entity, double amt) {
        if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity)) return;
        double cur = ((LivingEntity) entity).getHealth();

        ((LivingEntity) entity).setHealth(Math.max(cur - amt, 0));
        entity.playEffect(EntityEffect.HURT);
    }
}
