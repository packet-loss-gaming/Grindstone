/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Pumpkin Scuba", desc = "Breath underwater.")
public class PumpkinScubaHelmetComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {

        Entity e = event.getEntity();
        if (!(e instanceof Player) || !event.getCause().equals(EntityDamageEvent.DamageCause.DROWNING)) return;

        Player player = (Player) e;

        if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getTypeId() == BlockID.PUMPKIN) {
            player.setRemainingAir(player.getMaximumAir());
            event.setCancelled(true);
        }
    }
}
