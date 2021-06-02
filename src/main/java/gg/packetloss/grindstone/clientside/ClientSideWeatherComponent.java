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
import gg.packetloss.grindstone.events.BetterWeatherChangeEvent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Server;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.betterweather.WeatherType.THUNDERSTORM;

@ComponentInformation(friendlyName = "Client Side Weather Manager", desc = "Turn off the storm!")
public class ClientSideWeatherComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private List<Player> enabledFor = new ArrayList<>();

    @Override
    public void enable() {
        registerCommands(Commands.class);

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    public class Commands {
        @Command(aliases = {"stopweather"},
                usage = "", desc = "Hide all storms",
                flags = "r", min = 0, max = 0)
        public void showStormCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            if (args.hasFlag('r')) {
                enabledFor.remove(player);

                player.resetPlayerWeather();
                ChatUtil.sendNotice(player, "Storms are no longer hidden.");
            } else {
                enabledFor.add(player);

                player.setPlayerWeather(WeatherType.CLEAR);
                ChatUtil.sendNotice(player, "Storms are now hidden.");
                ChatUtil.sendNotice(player, "To show storms again, please use /stopweather -r.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onThunderChange(BetterWeatherChangeEvent event) {
        String state;

        if (event.getOldWeatherType() == THUNDERSTORM) {
            state = "ending";
        } else if (event.getNewWeatherType() == THUNDERSTORM) {
            state = "starting";
        } else {
            return;
        }

        enabledFor.stream().filter(player -> player.getWorld().equals(event.getWorld())).forEach(player -> {
            ChatUtil.sendWarning(player, "A thunder storm is " + state + " on your world.");
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        enabledFor.remove(player);
    }
}
