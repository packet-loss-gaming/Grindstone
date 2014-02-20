/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.CommandBookUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.jail.JailComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Block Hack", desc = "Stop the nukers")
@Depend(components = AdminComponent.class)
public class AntiBlockBreakComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    JailComponent jailComponent;

    ConcurrentHashMap<Player, AbstractMap.SimpleEntry<Long, Integer>> counter = new ConcurrentHashMap<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        //inst.registerEvents(this);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (!counter.containsKey(player)) {
            counter.put(player, new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), 1));
            return;
        }

        if (System.currentTimeMillis() - counter.get(player).getKey() / TimeUnit.SECONDS.toMillis(1) < 2) {
            AbstractMap.SimpleEntry<Long, Integer> k = counter.get(player);
            k.setValue(k.getValue() + 1);
            counter.put(player, k);
            if (counter.get(player).getKey() > 20) {
                event.setCancelled(true);
                try {
                    long duration = CommandBookUtil.matchFutureDate(k.getValue() + "m");
                    jailComponent.jail(player.getName(), duration);
                    Bukkit.broadcastMessage("The player:" + player.getName() + " has been jailed for "
                            + k.getValue() + " minutes because they attempted to break "
                            + k.getValue() + " blocks too quickly.");
                } catch (CommandException ignored) {
                }
            } else if (counter.get(player).getValue() > 5) {
                event.setCancelled(true);
            }
        }
    }
}