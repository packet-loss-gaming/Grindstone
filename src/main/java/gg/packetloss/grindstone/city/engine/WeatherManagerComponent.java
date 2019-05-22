/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Weather Manager", desc = "Turn off the storm!")
public class WeatherManagerComponent extends BukkitComponent implements Listener {

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

  @EventHandler(ignoreCancelled = true)
  public void onStormChange(WeatherChangeEvent event) {

    if (!event.toWeatherState() && event.getWorld().isThundering()) {
      event.getWorld().setThundering(false);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onThunderChange(ThunderChangeEvent event) {

    World world = event.getWorld();
    String state = event.toThunderState() ? "starting" : "ending";
    Collections.synchronizedList(enabledFor).stream().filter(player -> player.getWorld().equals(event.getWorld())).forEach(player -> {
      if (!event.toThunderState()) {
        if (ItemUtil.hasAncientArmour(player) || ItemUtil.isHoldingItem(player, CustomItems.MASTER_BOW)
            || ItemUtil.isHoldingItem(player, CustomItems.MASTER_SWORD)) {
          ChatUtil.sendWarning(player, ChatColor.DARK_RED + "===============[WARNING]===============");
        }
      }
      ChatUtil.sendNotice(player, "A thunder storm is " + state + " on your world.");
    });

    if (event.toThunderState() && world.getWeatherDuration() < world.getThunderDuration()) {
      world.setStorm(true);
      world.setWeatherDuration(world.getThunderDuration());
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {

    Player player = event.getPlayer();
    enabledFor.remove(player);
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
        ChatUtil.sendNotice(player, "To show storms again please use /stopweather -r.");
      }
    }
  }
}
