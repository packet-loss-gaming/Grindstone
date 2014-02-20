/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.util.ChanceUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Aurora Admin", desc = "Player Administration commands.")
public class XPBottlerComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {

        registerCommands(Commands.class);
    }

    public class Commands {

        @Command(aliases = {"bottle", "xpbottle"},
                usage = "", desc = "Bottle your xp",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.bottlexp"})
        public void bottleCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player))
                throw new CommandException("You must be a player to use this command.");
            Player player = (Player) sender;
            final int startingXp = player.getTotalExperience();

            int xp;
            for (xp = 0; xp < (startingXp / (3 + ChanceUtil.getRandom(8))); xp++) {
                if (!player.getInventory().contains(ItemID.GLASS_BOTTLE))
                    throw new CommandException("You do not have enough glass bottles.");
                player.getInventory().removeItem(new ItemStack(ItemID.GLASS_BOTTLE, 1));
                player.getInventory().addItem(new ItemStack(ItemID.BOTTLE_O_ENCHANTING, 1));
            }

            int newXp = startingXp - ((3 + ChanceUtil.getRandom(8)) * xp);
            player.setTotalExperience(newXp);
        }
    }
}
