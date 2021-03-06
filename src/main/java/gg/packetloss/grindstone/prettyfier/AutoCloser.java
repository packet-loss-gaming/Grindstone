/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prettyfier;

import com.google.common.collect.Lists;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Openable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class AutoCloser implements Prettyfier, Listener {
    private Map<Location, DebounceHandle<Location>> pendingCloses = new HashMap<>();

    @Override
    public void forceFinish() {
        Lists.newArrayList(pendingCloses.values()).forEach(DebounceHandle::bounceNow);
    }

    private Block getClosableBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return null;
        }

        Block clicked = event.getClickedBlock();
        if (!EnvironmentUtil.isClosable(clicked)) {
            return null;
        }

        Block lowerBlock = clicked.getRelative(BlockFace.DOWN);
        if (EnvironmentUtil.isTrapdoorBlock(clicked)) {
            // If the block immediately below isn't a ladder, leave this trapdoor alone. Their uses are
            // versatile enough there's a fair chance this is a decoration, or otherwise shouldn't be closed
            if (!EnvironmentUtil.isLadder(lowerBlock)) {
                return null;
            }
        } else if (EnvironmentUtil.isClosable(lowerBlock)) {
            clicked = lowerBlock;
        }

        return clicked;
    }

    private void createNewHandle(Location blockLoc) {
        TaskBuilder.Debounce<Location> builder = TaskBuilder.debounce();
        builder.setWaitTime(20 * 10);

        builder.setInitialValue(blockLoc);
        builder.setUpdateFunction((oldBlock, newBlock) -> newBlock);

        builder.setBounceAction((location) -> {
            Block block = location.getBlock();
            if (EnvironmentUtil.isClosable(block)) {
                Openable openable = (Openable) block.getBlockData();
                openable.setOpen(false);
                block.setBlockData(openable);
            }

            pendingCloses.remove(location);
        });

        DebounceHandle<Location> newHandle = builder.build();
        newHandle.accept(blockLoc);
        pendingCloses.put(blockLoc, newHandle);
    }

    @EventHandler
    public void onDoorInteract(PlayerInteractEvent event) {
        Block closable = getClosableBlock(event);
        if (closable == null) {
            return;
        }

        DebounceHandle<Location> existingHandle = pendingCloses.get(closable.getLocation());
        if (existingHandle != null) {
            existingHandle.accept(closable.getLocation());
            return;
        }

        createNewHandle(closable.getLocation());
    }
}
