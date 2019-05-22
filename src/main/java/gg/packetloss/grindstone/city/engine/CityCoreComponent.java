/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.material.Door;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "City Core", desc = "Operate the core city functionality.")
public class CityCoreComponent extends BukkitComponent implements Listener {
  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();

  @Override
  public void enable() {
    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  private boolean isCityWorld(World world) {
    return world.getName().equals("City");
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onDoorBreak(EntityBreakDoorEvent event) {
    Block block = event.getBlock();

    if (!isCityWorld(block.getWorld())) {
      return;
    }

    // Open the door.
    server.getScheduler().runTaskLater(inst, () -> {
      BlockState state = block.getRelative(BlockFace.DOWN).getState();
      Door doorData = (Door) state.getData();
      doorData.setOpen(true);
      state.update(true);
    }, 1);

    // Prevent the door from being destroyed.
    event.setCancelled(true);
  }
}
