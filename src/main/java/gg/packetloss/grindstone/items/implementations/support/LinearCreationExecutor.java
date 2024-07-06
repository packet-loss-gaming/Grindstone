/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.support;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.custom.item.BuildToolUseEvent;
import gg.packetloss.grindstone.items.custom.CustomItem;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.Tag;
import gg.packetloss.grindstone.util.ActionSimulationUtil;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

import static com.zachsthings.libcomponents.bukkit.BasePlugin.callEvent;

public class LinearCreationExecutor {
    private final CustomItems itemType;

    public LinearCreationExecutor(CustomItems itemType) {
        this.itemType = itemType;
    }

    public boolean isHoldingRelevantToolInOffhand(Player player) {
        return ItemUtil.isHoldingItemInOffhand(player, itemType);
    }

    public boolean isHoldingRelevantToolInAnyHand(Player player) {
        return ItemUtil.isHoldingItem(player, itemType) || ItemUtil.isHoldingItemInOffhand(player, itemType);
    }

    private int getDist(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Distance"));
    }

    private int getMaxDist(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Max Distance"));
    }

    public void adjustTool(Player player) {
        ItemStack item = player.getInventory().getItemInOffHand();

        final int dist = getDist(item);
        final int maxDist = getMaxDist(item);
        final short dur = item.getDurability();
        Map<Enchantment, Integer> enchants = item.getItemMeta().getEnchants();

        CustomItem cItem = CustomItemCenter.get(itemType);
        for (Tag tag : cItem.getTags()) {
            if (tag.getKey().equals("Distance")) {
                int newDist = dist + (player.isSneaking() ? -1 : 1);
                if (newDist > maxDist) {
                    newDist = 1;
                } else if (newDist < 1) {
                    newDist = maxDist;
                }
                tag.setProp(String.valueOf(newDist));
                ChatUtil.sendNotice(player, "Distance set to: " + newDist);
            }
        }

        ItemStack result = cItem.build();
        result.setDurability(dur);
        ItemMeta meta = result.getItemMeta();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        result.setItemMeta(meta);
        player.getInventory().setItemInOffHand(result);
    }

    public boolean canItemStackBePlaced(ItemStack itemStack) {
        return !ItemUtil.isNullItemStack(itemStack) && itemStack.getType().isBlock();
    }

    public void placeBlocksFrom(Player player, Block curTarget, BlockFace clickedFace) {
        BlockState blockSnapshot = curTarget.getState();

        BuildToolUseEvent useEvent = new BuildToolUseEvent(player, curTarget.getLocation(), itemType);
        callEvent(useEvent);
        if (useEvent.isCancelled()) {
            return;
        }

        // Update the target to be the next block, since one has already been placed.
        curTarget = curTarget.getRelative(clickedFace);

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!canItemStackBePlaced(heldItem) || heldItem.getType() != blockSnapshot.getType()) {
            throw new RuntimeException("This should not happen, possible cheater? Player: " + player.getName());
        }

        ItemStack item = player.getInventory().getItemInOffHand();
        short degradation = 0;
        int unbreakingLevel = item.getEnchantmentLevel(Enchantment.UNBREAKING);
        short curDur = item.getDurability();
        short maxDur = item.getType().getMaxDurability();
        BlockFace oppositeFace = clickedFace.getOppositeFace();

        // One block is already placed "by the player"
        int blocksPlaced, dist;
        for (blocksPlaced = 1, dist = getDist(item); blocksPlaced < dist; ++blocksPlaced) {
            if (!EnvironmentUtil.isAirBlock(curTarget)) {
                if (!EnvironmentUtil.isShrubBlock(curTarget)) {
                    break;
                }

                BlockBreakEvent event = new BlockBreakEvent(
                    curTarget,
                    player
                );

                CommandBook.callEvent(event);
                if (event.isCancelled()) {
                    break;
                }
            }

            if (blocksPlaced >= heldItem.getAmount() && player.getGameMode() != GameMode.CREATIVE) {
                break;
            }

            if (curDur + degradation > maxDur) {
                break;
            }

            if (placeBlock(curTarget, player, oppositeFace, heldItem, blockSnapshot)) {
                if (ChanceUtil.getChance(unbreakingLevel + 1)) {
                    ++degradation;
                }
                --dist;
            } else {
                break;
            }

            curTarget = curTarget.getRelative(clickedFace);
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (curDur + degradation >= maxDur) {
            player.setItemInHand(null);
        } else {
            item.setDurability((short) (curDur + degradation));
        }

        if (heldItem.getAmount() <= blocksPlaced) {
            player.getInventory().setItemInMainHand(null);
        } else {
            heldItem.setAmount(heldItem.getAmount() - blocksPlaced);
        }
    }

    private boolean placeBlock(Block b, Player player, BlockFace oppositeFace, ItemStack item, BlockState blockState) {
        BlockState oldState = b.getState();
        BlockState newState = b.getState();
        newState.setType(blockState.getType());
        newState.setBlockData(blockState.getBlockData());

        return ActionSimulationUtil.placeBlock(
            player,
            b,
            oldState,
            newState,
            b.getRelative(oppositeFace),
            item,
            EquipmentSlot.HAND,
            true,
            CommandBook::callEvent
        );
    }
}
