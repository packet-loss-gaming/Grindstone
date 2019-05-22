/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.modifiers;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.database.IOUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Modifiers", desc = "Commands and saving for the Modifier system.")
public class ModifierComponent extends BukkitComponent implements Listener {
  private static Modifier modifierCenter;
  private final CommandBook inst = CommandBook.inst();
  private final Logger log = CommandBook.logger();
  private final Server server = CommandBook.server();

  public static Modifier getModifierCenter() {
    return modifierCenter;
  }

  @Override
  public void enable() {
    load();
    registerCommands(Commands.class);

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  @Override
  public void disable() {
    save();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    List<String> messages = new ArrayList<>();
    for (ModifierType type : ModifierType.values()) {
      long dur = modifierCenter.status(type);
      if (dur == 0) {
        continue;
      }
      String friendlyTime = ChatUtil.getFriendlyTime(System.currentTimeMillis() + dur);
      messages.add(" - " + type.fname() + " till " + friendlyTime);
    }
    if (messages.isEmpty()) {
      return;
    }

    messages.sort(String.CASE_INSENSITIVE_ORDER);
    messages.add(0, "\n\nThe following donation perks are enabled:");

    Player player = event.getPlayer();
    server.getScheduler().runTaskLater(inst, () -> {
      for (String message : messages) {
        gg.packetloss.grindstone.util.ChatUtil.sendNotice(player, ChatColor.GOLD, message);
      }
    }, 20);
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

  public class Commands {
    @Command(aliases = {"modifiers"}, desc = "Modifier Commands")
    @NestedCommand({ModifierCommands.class})
    public void modCommands(CommandContext args, CommandSender sender) throws CommandException {

    }
  }

  public class ModifierCommands {
    @Command(aliases = {"extend"}, desc = "Extend the duration of a modifier",
        usage = "<modifier> <time> [player]",
        flags = "", min = 2, max = 3)
    @CommandPermissions("aurora.modifiers.extend")
    public void extendCmd(CommandContext args, CommandSender sender) throws CommandException {
      ModifierType modifierType;
      try {
        String modifierStr = args.getString(0);
        if (modifierStr.equalsIgnoreCase("rand")) {
          modifierType = CollectionUtil.getElement(ModifierType.values());
        } else {
          modifierType = ModifierType.valueOf(modifierStr);
        }
      } catch (IllegalArgumentException ex) {
        throw new CommandException("No modifier by that name could be found!");
      }
      long amount = InputUtil.TimeParser.matchDate(args.getString(1));

      boolean wasOn = modifierCenter.isActive(modifierType);
      modifierCenter.extend(modifierType, amount);
      save();

      String friendlyTime = ChatUtil.getFriendlyTime(System.currentTimeMillis() + modifierCenter.status(modifierType));
      String change = wasOn ? " extended" : " enabled";
      String by = args.argsLength() > 2 ? " by " + args.getString(2) : "";
      Bukkit.broadcastMessage(ChatColor.GOLD + modifierType.fname() + change + by + " till " + friendlyTime + "!");
    }
  }
}
