/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.implementations.support.LinearCreationExecutor;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static com.zachsthings.libcomponents.bukkit.BasePlugin.server;

// FIXME: The seperation between "LinearBlockPlacer" and "LinearCreationExecutor" is a bit convoluted.
public class LinearBlockPlacer extends AbstractItemFeatureImpl {
    private static LinearCreationExecutor executor = new LinearCreationExecutor(CustomItems.LINEAR_BLOCK_PLACER);

    private Set<UUID> reentrantDelay = new HashSet<>();
    private Map<UUID, BlockFace> lastClickedFaces = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!executor.isHoldingRelevantToolInAnyHand(player)) {
            return;
        }

        if (reentrantDelay.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        reentrantDelay.add(player.getUniqueId());
        server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            reentrantDelay.remove(player.getUniqueId());
        }, 2);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && executor.isHoldingRelevantToolInOffhand(player)) {
            event.setCancelled(true);
            executor.adjustTool(player);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Prevent using the placer as a hoe.
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (!executor.canItemStackBePlaced(heldItem)) {
                ChatUtil.sendError(player, "Put the block you'd like to place in your main-hand.");
                event.setCancelled(true);
                return;
            }

            // Allow the block place, and track the face that was clicked.
            lastClickedFaces.put(event.getPlayer().getUniqueId(), event.getBlockFace());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastClickedFaces.remove(event.getPlayer().getUniqueId());
    }

    private boolean alreadyActive = false;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if (alreadyActive) {
            return;
        }

        Player player = event.getPlayer();
        if (!executor.isHoldingRelevantToolInOffhand(player)) {
            return;
        }

        BlockFace lastClickedFace = lastClickedFaces.get(player.getUniqueId());
        if (lastClickedFace == null) {
            return;
        }

        try {
            alreadyActive = true;
            executor.placeBlocksFrom(player, event.getBlock(), lastClickedFace);
        } finally {
            alreadyActive = false;
        }
    }
}
