/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.events.entity.item.ChestSuckEvent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.Inventory;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Chest Deposit", desc = "Make item on top chest go into the chest.")
public class ChestDepositComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        //inst.registerEvents(this);
        //server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20);
    }

    @Override
    public void run() {

        for (World world : server.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                addItemToChest(item);
            }
        }
    }

    private void addItemToChest(Item item) {

        try {
            Block block = item.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (block.getTypeId() == BlockID.CHEST) {

                if (item.getItemStack().getTypeId() == BlockID.BEDROCK) {
                    item.remove();
                    return;
                }

                for (Location location : LocationUtil.getNearbyLocations(block.getLocation(), 2, 1)) {
                    if (EnvironmentUtil.isSign(location.getBlock())) {
                        Sign sign = (Sign) location.getBlock().getState();

                        if (sign.getLine(1).equalsIgnoreCase("[CFilter]")) {
                            if (sign.getLine(2).trim().equals(String.valueOf(item.getItemStack().getTypeId()))
                                    || sign.getLine(3).trim().equals(String.valueOf(item.getItemStack().getTypeId()))) {
                                return;
                            }
                        } else if (sign.getLine(1).equalsIgnoreCase("[CDestroy]")) {
                            if (sign.getLine(2).trim().equals(String.valueOf(item.getItemStack().getTypeId()))
                                    || sign.getLine(3).trim().equals(String.valueOf(item.getItemStack().getTypeId()))
                                    || sign.getLine(2).trim().equalsIgnoreCase("All")) {
                                item.remove();
                                return;
                            }
                        }
                    }
                }

                Inventory chestInventory = ((Chest) block.getState()).getInventory();
                if (chestInventory.firstEmpty() != -1) {

                    ChestSuckEvent event = new ChestSuckEvent(block, item, item.getItemStack());
                    server.getPluginManager().callEvent(event);

                    if (event.isCancelled()) return;

                    chestInventory.addItem(event.getItemStack());
                    item.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.warning("The item: " + item.getEntityId()
                    + " could not be processed by the: "
                    + this.getInformation().friendlyName() + " component.");
        }
    }

    @EventHandler
    public void onSignTextChange(SignChangeEvent event) {

        Player player = event.getPlayer();
        String[] lines = event.getLines();

        if (lines[1].equalsIgnoreCase("[CDestroy]")) {
            ChatUtil.sendNotice(player, "Chest Filter Sign Successfully created.");

            if (lines[2].equalsIgnoreCase("All")) {
                event.setLine(2, "All");
                ChatUtil.sendNotice(player, "Chest Will Destroy: EVERYTHING.");
            } else if (lines[2].trim().length() > 0) {
                ChatUtil.sendNotice(player, "Chest Will Destroy: " + lines[2] + ".");
            }

            if (lines[3].trim().length() > 0 && !lines[2].equalsIgnoreCase("All")) {
                ChatUtil.sendNotice(player, "Chest Will Destroy: " + lines[3] + ".");
            }
        } else if (lines[1].equalsIgnoreCase("[CFilter]")) {
            ChatUtil.sendNotice(player, "Chest Filter Sign Successfully created.");

            if (lines[2].equalsIgnoreCase("[CDestroy]")) {
                ChatUtil.sendNotice(player, "Chest Will Avoid: " + lines[2] + ".");
            }

            if (lines[3].trim().length() > 0) {
                ChatUtil.sendNotice(player, "Chest Will Avoid: " + lines[2] + ".");
            }
        }
    }
}
