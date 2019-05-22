/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCreatePortalEvent;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Ender Dragon", desc = "Stop Enderdragon portals")
public class EnderDragonComponent extends BukkitComponent implements Listener {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = CommandBook.logger();
  private final Server server = CommandBook.server();

  @Override
  public void enable() {

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCreatePortal(EntityCreatePortalEvent event) {

    if (event.getEntityType().equals(EntityType.ENDER_DRAGON)
        && !event.getEntity().getWorld().getEnvironment().equals(World.Environment.THE_END)) {
      Bukkit.broadcastMessage(ChatColor.GOLD + "Jeffery died, the village is safe once again!");
      if (!event.isCancelled()) {
        event.setCancelled(true);
      }
    }
  }
}
