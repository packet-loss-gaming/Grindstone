/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.clientside;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;

@ComponentInformation(friendlyName = "Client Side Time Manager", desc = "Turn off the night!")
public class ClientSideTimeComponent extends BukkitComponent implements Listener {
    private final List<Player> enabledFor = new ArrayList<>();

    @Override
    public void enable() {
        registerCommands(Commands.class);

        CommandBook.registerEvents(this);

        Bukkit.getScheduler().runTaskTimer(CommandBook.inst(), this::syncTimes, 0, 20 * 5);
    }

    private void applyToPlayer(Player player) {
        if (EnvironmentUtil.isDayTime(player.getWorld().getTime())) {
            player.setPlayerTime(0, true);
        } else {
            player.setPlayerTime(EnvironmentUtil.getNightStartTime(), true);
        }
    }

    private void syncTimes() {
        for (Player player : enabledFor) {
            applyToPlayer(player);
        }
    }

    public class Commands {
        @Command(aliases = {"daylight"},
                usage = "", desc = "Hide the darkness",
                flags = "r", min = 0, max = 0)
        public void showDaylightCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            if (args.hasFlag('r')) {
                enabledFor.remove(player);

                player.resetPlayerTime();
                ChatUtil.sendNotice(player, "Daylight is no longer forced.");
            } else {
                enabledFor.add(player);

                applyToPlayer(player);
                ChatUtil.sendNotice(player, "Daylight is now forced.");
                ChatUtil.sendNotice(player, "To stop forcing daylight, please use /daylight -r.");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        enabledFor.remove(player);
    }
}
