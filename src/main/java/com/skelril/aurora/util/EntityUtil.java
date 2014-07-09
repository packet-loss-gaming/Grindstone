/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class EntityUtil {
    public static boolean nameMatches(Entity entity, String name) {
        if (entity == null || !(entity instanceof LivingEntity)) return false;
        String customName = ((LivingEntity) entity).getCustomName();
        return customName != null && customName.equals(name);
    }

    public static void heal(Entity entity, double amt) {
        if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity)) return;
        double cur = ((LivingEntity) entity).getHealth();
        double max = ((LivingEntity) entity).getMaxHealth();

        ((LivingEntity) entity).setHealth(Math.min(cur + amt, max));
    }

    public static void forceDamage(Entity entity, double amt) {
        if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity)) return;
        double cur = ((LivingEntity) entity).getHealth();

        ((LivingEntity) entity).setHealth(Math.max(cur - amt, 0));
    }
}
