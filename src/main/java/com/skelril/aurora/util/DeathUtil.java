/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.Random;

public class DeathUtil {

    private static final Random random = new Random();

    private static final PotionType[] thrownTypes = new PotionType[]{
            PotionType.INSTANT_DAMAGE, PotionType.INSTANT_DAMAGE,
            PotionType.POISON, PotionType.WEAKNESS
    };

    public static void throwSlashPotion(Location location) {

        ThrownPotion potionEntity = (ThrownPotion) location.getWorld().spawnEntity(location, EntityType.SPLASH_POTION);
        PotionType type = CollectionUtil.getElement(thrownTypes);
        Potion potion = new Potion(type);
        potion.setLevel(type.getMaxLevel());
        potion.setSplash(true);
        potionEntity.setItem(potion.toItemStack(1));
        potionEntity.setVelocity(new Vector(
                random.nextDouble() * .5 - .25,
                random.nextDouble() * .4 + .1,
                random.nextDouble() * .5 - .25
        ));
    }
}
