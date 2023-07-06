/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.listener;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.material.Door;

import java.util.function.Predicate;

public class DoorRestorationListener implements Listener {
    private final Predicate<World> appliesTo;

    public DoorRestorationListener(Predicate<World> appliesTo) {
        this.appliesTo = appliesTo;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDoorBreak(EntityBreakDoorEvent event) {
        Block block = event.getBlock();

        if (!appliesTo.test(block.getWorld())) {
            return;
        }

        // Open the door.
        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            BlockState state = block.getRelative(BlockFace.DOWN).getState();
            Door doorData = (Door) state.getData();
            doorData.setOpen(true);
            state.update(true);
        }, 1);

        // Prevent the door from being destroyed.
        event.setCancelled(true);
    }
}
