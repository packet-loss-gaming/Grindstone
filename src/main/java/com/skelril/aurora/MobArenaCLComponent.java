/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import com.garbagemule.MobArena.events.ArenaPlayerLeaveEvent;
import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "MACL", desc = "Mob Arena Compatibility Layer")
@Depend(plugins = {"MobArena"}, components = {AdminComponent.class})
public class MobArenaCLComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent admin;

    Set<Player> playerList = new HashSet<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArenaEnter(ArenaPlayerJoinEvent event) {

        Player player = event.getPlayer();

        admin.deadmin(player, true);
        if (!playerList.contains(player)) playerList.add(player);
    }

    @EventHandler
    public void onArenaLeave(ArenaPlayerLeaveEvent event) {

        Player player = event.getPlayer();

        if (playerList.contains(player)) playerList.remove(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        Player player = event.getPlayer();

        if (playerList.contains(player)) event.setCancelled(true);
    }
}
