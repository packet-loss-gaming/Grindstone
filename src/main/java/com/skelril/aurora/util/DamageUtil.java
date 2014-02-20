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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by wyatt on 12/28/13.
 */
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
}
