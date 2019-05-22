/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.garbagemule.MobArena.events.ArenaPlayerJoinEvent;
import com.garbagemule.MobArena.events.ArenaPlayerLeaveEvent;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.PrayerApplicationEvent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "MACL", desc = "Mob Arena Compatibility Layer")
@Depend(plugins = {"MobArena"}, components = {AdminComponent.class})
public class MobArenaCLComponent extends BukkitComponent implements Listener {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = CommandBook.logger();
  private final Server server = CommandBook.server();
  Set<Player> playerList = new HashSet<>();
  @InjectComponent
  private AdminComponent admin;

  @Override
  public void enable() {

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  @EventHandler(ignoreCancelled = true)
  public void onArenaEnter(ArenaPlayerJoinEvent event) {

    Player player = event.getPlayer();

    if (!admin.deadmin(player)) {
      ChatUtil.sendError(player, "Failed to disable admin mode, mob arena add cancelled.");
      event.setCancelled(true);
      return;
    }
    playerList.add(player);
  }

  @EventHandler
  public void onArenaLeave(ArenaPlayerLeaveEvent event) {

    Player player = event.getPlayer();

    playerList.remove(player);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPrayerApplication(PrayerApplicationEvent event) {

    Player player = event.getPlayer();

    if (playerList.contains(player)) {
      event.setCancelled(true);
    }
  }
}
