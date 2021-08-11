/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

class LevelAdjustmentListener implements Listener {
    private final WorldLevelComponent parent;

    public LevelAdjustmentListener(WorldLevelComponent parent) {
        this.parent = parent;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (!ItemUtil.isItem(itemStack, CustomItems.DEMONIC_ASHES)) {
            return;
        }

        Player player = event.getPlayer();
        if (!parent.isRangeWorld(player.getWorld())) {
            ChatUtil.sendError(player, "These won't have any effect here.");
            ChatUtil.sendError(player, "This world is shielded from influence.");
            return;
        }

        if (parent.isPeaceful(player)) {
            ChatUtil.sendError(player, "Spreading these ashes would violate your peaceful stature.");
            return;
        }

        ItemUtil.removeItemOfName(
            player,
            CustomItemCenter.build(CustomItems.DEMONIC_ASHES),
            1,
            false
        );

        // Update the world level
        int newLevel = parent.getWorldLevel(player) + 1;
        parent.setWorldLevel(player, newLevel);
        parent.showTitleForLevel(player, newLevel);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSacrifice(PlayerSacrificeItemEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (!ItemUtil.isItem(itemStack, CustomItems.BARBARIAN_BONE)) {
            return;
        }

        Player player = event.getPlayer();
        int worldLevel = parent.getWorldLevel(player);

        // If any bones were consumed, remove the corresponding amount of levels, and add grant them
        // as ashes
        int amountConsumed = Math.min(itemStack.getAmount(), worldLevel - 1);
        if (amountConsumed > 0) {
            // Update the world level
            int newLevel = worldLevel - amountConsumed;
            parent.setWorldLevel(player, newLevel);
            parent.showTitleForLevel(player, newLevel);

            // Give ashes
            GeneralPlayerUtil.giveItemToPlayer(
                player,
                CustomItemCenter.build(CustomItems.DEMONIC_ASHES, amountConsumed)
            );
        }

        // If any bones remain in the stack, give the remainder back
        int newAmountOfBones = itemStack.getAmount() - amountConsumed;
        if (newAmountOfBones > 0) {
            GeneralPlayerUtil.giveItemToPlayer(
                player,
                CustomItemCenter.build(CustomItems.BARBARIAN_BONE, newAmountOfBones)
            );
        }

        event.setItemStack(null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        World world = event.getBlock().getWorld();
        if (!parent.isRangeWorld(world)) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();
        if (parent.shouldSpawnDemonicAshes(player, blockLoc, block.getType())) {
            parent.spawnDemonicAshes(player, blockLoc);
        }
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        World world = player.getWorld();
        if (!parent.isRangeWorld(world) || parent.isRangeWorld(event.getFrom())) {
            return;
        }

        parent.showTitleForLevelIfInteresting(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location respawnLocation = event.getRespawnLocation();
        if (!parent.isRangeWorld(respawnLocation.getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        parent.showTitleForLevelIfInteresting(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();
        if (!parent.isRangeWorld(world)) {
            return;
        }

        int worldLevel = parent.getWorldLevel(player);
        if (worldLevel > 1) {
            parent.setWorldLevel(player, worldLevel / 2);
        }
    }
}
