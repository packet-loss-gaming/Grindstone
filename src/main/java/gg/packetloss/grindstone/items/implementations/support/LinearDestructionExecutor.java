/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.support;

import gg.packetloss.grindstone.events.custom.item.BuildToolUseEvent;
import gg.packetloss.grindstone.items.custom.CustomItem;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.Tag;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

import static com.zachsthings.libcomponents.bukkit.BasePlugin.callEvent;

public abstract class LinearDestructionExecutor {
    private CustomItems itemType;

    public LinearDestructionExecutor(CustomItems itemType) {
        this.itemType = itemType;
    }

    public abstract boolean accepts(Block target);

    public void process(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!ItemUtil.isHoldingItem(player, itemType)) {
            return;
        }

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
                handleRightClick(player);
                break;
            case LEFT_CLICK_BLOCK:
                handleLeftClick(player, event);
                break;
        }
    }

    public boolean impedeRightClick(Player player) {
        if (!ItemUtil.isHoldingItem(player, itemType)) {
            return false;
        }

        return player.isSneaking();
    }

    private int getDist(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Distance"));
    }

    private int getMaxDist(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Max Distance"));
    }

    private void handleRightClick(Player player) {
        if (!impedeRightClick(player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        final int dist = getDist(item);
        final int maxDist = getMaxDist(item);
        final short dur = item.getDurability();
        Map<Enchantment, Integer> enchants = item.getItemMeta().getEnchants();

        CustomItem cItem = CustomItemCenter.get(itemType);
        for (Tag tag : cItem.getTags()) {
            if (tag.getKey().equals("Distance")) {
                int newDist = dist + 1;
                if (newDist > maxDist) {
                    newDist = 1;
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
        player.setItemInHand(result);
    }

    private void handleLeftClick(Player player, PlayerInteractEvent event) {
        Block curTarget = event.getClickedBlock();

        BuildToolUseEvent useEvent = new BuildToolUseEvent(player, curTarget.getLocation(), itemType);
        callEvent(useEvent);
        if (useEvent.isCancelled()) {
            return;
        }

        if (!accepts(curTarget)) return;

        event.setCancelled(true);
        // callEvent(new RapidBlockBreakEvent(player));

        final Material initialType = curTarget.getType();

        ItemStack item = player.getInventory().getItemInMainHand();
        short degradation = 0;
        int unbreakingLevel = item.getEnchantmentLevel(Enchantment.DURABILITY);
        short curDur = item.getDurability();
        short maxDur = item.getType().getMaxDurability();
        for (int dist = getDist(item); dist > 0;) {
            if (curTarget.getType() != initialType) {
                break;
            }

            if (curDur + degradation > maxDur) {
                break;
            }
            if (breakBlock(curTarget, player, item)) {
                if (ChanceUtil.getChance(unbreakingLevel + 1)) {
                    ++degradation;
                }
                --dist;
            } else {
                break;
            }
            curTarget = curTarget.getRelative(event.getBlockFace().getOppositeFace());
        }

        if (curDur + degradation >= maxDur) {
            player.setItemInHand(null);
        } else {
            item.setDurability((short) (curDur + degradation));
        }
    }

    private boolean breakBlock(Block b, Player p, ItemStack i) {
        BlockBreakEvent event = new BlockBreakEvent(b, p);
        callEvent(event);
        return !event.isCancelled() && b.breakNaturally(i);
    }
}
