/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE;

public class DamageUtil {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private static Map<Entity, Entity> entries = new ConcurrentHashMap<>();

    public static void damage(LivingEntity attacker, LivingEntity defender, double amount) {

        entries.put(attacker, defender);

        defender.damage(amount, attacker);
    }

    public static boolean remove(Entity attacker, Entity defender) {

        Entity testDefender = entries.remove(attacker);

        return testDefender != null && testDefender.equals(defender);
    }

    public static void multiplyFinalDamage(EntityDamageEvent event, double multiplier) {
        event.setDamage(BASE, Math.max(0, event.getDamage() + (event.getFinalDamage() * (multiplier - 1))));
    }
}
