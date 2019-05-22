/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;

@ComponentInformation(friendlyName = "Safe Tele", desc = "No falling pl0x.")
public class SafeTeleComponent extends BukkitComponent implements Listener {

  private static final List<PlayerTeleportEvent.TeleportCause> CAUSES = new ArrayList<>(2);

  static {
    CAUSES.add(PlayerTeleportEvent.TeleportCause.COMMAND);
    CAUSES.add(PlayerTeleportEvent.TeleportCause.PLUGIN);
  }

  private final CommandBook inst = CommandBook.inst();
  private final Server server = CommandBook.server();

  @Override
  public void enable() {

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  @EventHandler(ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {

    if (CAUSES.contains(event.getCause()) && !event.getPlayer().isFlying()) {
      Location loc = LocationUtil.findFreePosition(event.getTo(), false);
      if (loc == null) {
        ChatUtil.sendError(event.getPlayer(), "That location is not safe!");
        event.setCancelled(true);
        return;
      }
      event.setTo(loc);
    }
  }
}
