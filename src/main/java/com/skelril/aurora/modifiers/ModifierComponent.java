/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.modifiers;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.*;
import com.skelril.aurora.util.database.IOUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Modifiers", desc = "Commands and saving for the Modifier system.")
public class ModifierComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private final static int interval = 20 * 60 * 5;
    private static Modifier modifierCenter;

    @Override
    public void enable() {
        load();
        registerCommands(Commands.class);
    }

    @Override
    public void disable() {
        save();
    }

    public static Modifier getModifierCenter() {
        return modifierCenter;
    }

    public class Commands {
        @Command(aliases = {"modifiers"}, desc = "Modifier Commands")
        @NestedCommand({ModifierCommands.class})
        public void modCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class ModifierCommands {
        @Command(aliases = {"extend"}, desc = "Extend the duration of a modifier",
                usage = "<modifier> <time>",
                flags = "", min = 2, max = 2)
        @CommandPermissions("aurora.modifiers.extend")
        public void extendCmd(CommandContext args, CommandSender sender) throws CommandException {
            ModifierType modifierType;
            try {
                modifierType = ModifierType.valueOf(args.getString(0));
            } catch (IllegalArgumentException ex) {
                throw new CommandException("No modifier by that name could be found!");
            }
            long amount = InputUtil.TimeParser.matchDate(args.getString(1));

            modifierCenter.extend(modifierType, amount);
            save();

            String friendlyTime = ChatUtil.getFriendlyTime(System.currentTimeMillis() + modifierCenter.status(modifierType));
            Bukkit.broadcastMessage(ChatColor.GOLD + modifierType.name() + " enabled till " + friendlyTime + "!");
        }
    }

    public void load() {
        Object obj = IOUtil.readBinaryFile(new File(inst.getDataFolder(), "modifiers.dat"));
        if (obj instanceof Modifier) {
            modifierCenter = (Modifier) obj;
        } else {
            modifierCenter = new Modifier();
        }
    }

    public void save() {
        IOUtil.toBinaryFile(inst.getDataFolder(), "modifiers", modifierCenter);
    }
}
