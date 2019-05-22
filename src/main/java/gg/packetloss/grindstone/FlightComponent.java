/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Wing", desc = "Fly like the wind!")
public class FlightComponent extends BukkitComponent implements Listener {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = CommandBook.logger();
  private final Server server = CommandBook.server();

  @Override
  public void enable() {

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
    registerCommands(Commands.class);
  }

  // Player Management Section
  private void wingPlayer(Player player) {

    wingPlayer(player, .3F);
  }

  private void wingPlayer(Player player, float speed) {

    final boolean couldFly = player.getAllowFlight();

    player.setAllowFlight(true);
    player.setFlySpeed(speed);

    if (couldFly) {
      ChatUtil.sendNotice(player, "Your wings have been changed!");
    } else {
      ChatUtil.sendNotice(player, "You gain wings and can fly!");
    }
  }

  private void dewingPlayer(Player player) {

    player.setAllowFlight(false);
    player.setFallDistance(0F);
    ChatUtil.sendWarning(player, "You lose your wings and can no longer fly.");
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {

    final Player player = event.getPlayer();

    server.getScheduler().runTaskLater(inst, () -> {
      //noinspection deprecation
      if (inst.hasPermission(player, "aurora.fly.auto") && !player.isOnGround()) {
        player.setAllowFlight(true);
        player.setFlying(true);
      }
    }, 1);
  }

  public class Commands {

    @Command(aliases = {"wing"},
        usage = "[-s speed] [player]", desc = "Fly like the wind!",
        flags = "s:", min = 0, max = 1)
    public void flyCmd(CommandContext args, CommandSender sender) throws CommandException {

      // Get the target
      Player target;
      if (args.argsLength() > 0) {
        inst.checkPermission(sender, "aurora.fly.other.wing");
        target = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));
      } else {
        inst.checkPermission(sender, "aurora.fly.self.wing");
        target = PlayerUtil.checkPlayer(sender);

      }

      float speed = -2;
      if (args.hasFlag('s')) {
        try {
          speed = Float.parseFloat(args.getFlag('s'));
          if (speed > 1 || speed < -1) {
            throw new CommandException("The number must be between -1 and 1.");
          }
        } catch (NumberFormatException e) {
          throw new CommandException("The number must be between -1 and 1.");
        }
      }

      // Check to see if they can already fly
      if (speed == -2) {
        if (target.getAllowFlight()) {
          throw new CommandException("The player: " + target.getName() + " already has wings.");
        } else {
          ChatUtil.sendNotice(sender, "The player: " + target.getName() + " has been given wings.");
          wingPlayer(target);
        }
      } else {
        if (speed == target.getFlySpeed() && target.getAllowFlight()) {
          throw new CommandException("The player: " + target.getName() + " already has wings.");
        } else if (speed != target.getFlySpeed() && target.getAllowFlight()) {
          ChatUtil.sendNotice(sender, "The player: " + target.getName() + "'s wings have been changed.");
          wingPlayer(target, speed);
        } else {
          ChatUtil.sendNotice(sender, "The player: " + target.getName() + " has been given wings.");
          wingPlayer(target, speed);
        }
      }
    }


    @Command(aliases = {"dewing"},
        usage = "[player]", desc = "Fly like the wind!",
        flags = "", min = 0, max = 1)
    public void deflyCmd(CommandContext args, CommandSender sender) throws CommandException {

      // Get the Target
      Player target;
      if (args.argsLength() > 0) {
        inst.checkPermission(sender, "aurora.fly.other.dewing");
        target = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));
      } else {
        inst.checkPermission(sender, "aurora.fly.self.dewing");
        target = PlayerUtil.checkPlayer(sender);
      }

      // Check to see if they can already fly
      if (!target.getAllowFlight()) {
        ChatUtil.sendError(sender, "The player: " + target.getName() + " has no wings to take.");
      } else {
        ChatUtil.sendNotice(sender, "The player: " + target.getName() + " has lost his or her wings.");
        dewingPlayer(target);
      }
    }
  }
}