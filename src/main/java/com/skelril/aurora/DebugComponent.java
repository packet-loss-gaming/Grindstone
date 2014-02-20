/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.hackbook.ChunkBook;
import com.skelril.hackbook.exceptions.UnsupportedFeatureException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "ADebug", desc = "Debug tools")
public class DebugComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        //inst.registerEvents(new InventoryCorruptionFixer());
        //noinspection AccessStaticViaInstance
        //inst.registerEvents(new BlockDebug());

        registerCommands(ChunkLighter.class);
        //registerCommands(LocationDebug.class);

        // Bug fixes

        // Fixes an issue where potion effects are not removed from players on death
        //noinspection AccessStaticViaInstance
        //inst.registerEvents(new PotionDeathFix());
    }

    private class InventoryCorruptionFixer implements Listener {

        @EventHandler
        public void onLogin(PlayerJoinEvent event) {

            Player player = event.getPlayer();

            if (!player.getName().equals("Dark_Arc")) return;
            ItemStack[] inventory = player.getInventory().getContents();
            inventory = ItemUtil.removeItemOfType(inventory, Material.DOUBLE_PLANT.getId());
            player.getInventory().setContents(inventory);
        }
    }

    public class ChunkLighter {

        @Command(aliases = {"relight"}, desc = "Get your location",
                flags = "", min = 0, max = 0)
        @CommandPermissions("aurora.debug.relight")
        public void myLocCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            try {
                ChunkBook.relight(player.getLocation().getChunk());
            } catch (UnsupportedFeatureException e) {
                throw new CommandException("This feature is not currently supported.");
            }
            ChatUtil.sendNotice(player, "The chunk lighting has successfully been recalculated.");
        }
    }

    public class LocationDebug {

        @Command(aliases = {"myloc"}, desc = "Get your location",
                flags = "", min = 0, max = 0)
        public void myLocCmd(CommandContext args, CommandSender sender) throws CommandException {

            Location l = PlayerUtil.checkPlayer(sender).getLocation();
            ChatUtil.sendNotice(sender, "X: " + l.getX() + ", Y:" + l.getY() + ", Z: " + l.getZ());
            ChatUtil.sendNotice(sender, "Pitch: " + l.getPitch() + ", Yaw: " + l.getYaw());
        }
    }

    private class BlockDebug implements Listener {

        @EventHandler
        public void onRightClick(PlayerInteractEvent event) {

            ItemStack held = event.getItem();
            if (held != null && held.getType() == Material.COAL && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Block block = event.getClickedBlock();
                ChatUtil.sendNotice(event.getPlayer(),
                        "Block name: " + block.getType()
                                + ", Type ID: " + block.getTypeId()
                                + ", Block data: " + block.getData()
                );
                event.setUseInteractedBlock(Event.Result.DENY);
            }
        }
    }

    private class PotionDeathFix implements Listener {

        @EventHandler
        public void onRespawn(final PlayerRespawnEvent event) {

            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    Player player = event.getPlayer();

                    for (PotionEffect next : player.getActivePotionEffects()) {
                        player.addPotionEffect(new PotionEffect(next.getType(), 0, 0), true);
                    }
                }
            }, 1);
        }
    }
}
