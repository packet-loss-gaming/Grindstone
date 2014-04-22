/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class EntityUtil {
    public static void heal(Entity entity, double amt) {
        if (!(entity instanceof LivingEntity)) return;
        double cur = ((LivingEntity) entity).getHealth();
        double max = ((LivingEntity) entity).getMaxHealth();

        ((LivingEntity) entity).setHealth(Math.min(cur + amt, max));
    }

    public static void forceDamage(Entity entity, double amt) {
        if (!(entity instanceof LivingEntity)) return;
        double cur = ((LivingEntity) entity).getHealth();

        ((LivingEntity) entity).setHealth(Math.max(cur - amt, 0));
    }
}
