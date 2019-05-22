/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;


import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.bukkit.WorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Lava Flow", desc = "Law flow stopper.")
@Depend(plugins = "WorldGuard")
public class LavaFlowComponent extends BukkitComponent implements Listener {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();

  private WorldGuardPlugin worldGuard;

  @Override
  public void enable() {

    try {
      setUpWorldGuard();
    } catch (UnknownPluginException e) {
      log.warning("Plugin not found: " + e.getMessage() + ".");
      return;
    }

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  private void setUpWorldGuard() throws UnknownPluginException {

    Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

    // WorldGuard may not be loaded
    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
      throw new UnknownPluginException("WorldGuard");
    }

    this.worldGuard = (WorldGuardPlugin) plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockFromTo(BlockFromToEvent event) {

    if (EnvironmentUtil.isLava(event.getBlock())) {
      try {
        WorldConfiguration wcfg = worldGuard.getGlobalStateManager().get(event.getBlock().getWorld());
        if (wcfg.preventWaterDamage.size() > 0) {
          if (wcfg.preventWaterDamage.contains(event.getToBlock().getTypeId())) {
            event.setCancelled(true);
          }
        }
      } catch (NullPointerException ex) {
        log.warning("Blocking lava flow, configuration error!");
        event.setCancelled(true);
      }
    }
  }
}
