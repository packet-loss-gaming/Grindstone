package gg.packetloss.grindstone.items.implementations.support;

import com.sk89q.commandbook.CommandBook;
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
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.zachsthings.libcomponents.bukkit.BasePlugin.callEvent;
import static com.zachsthings.libcomponents.bukkit.BasePlugin.server;

public class LinearCreationExecutor {
    private CustomItems itemType;
    private Set<UUID> reentrantDelay = new HashSet<>();

    public LinearCreationExecutor(CustomItems itemType) {
        this.itemType = itemType;
    }

    public void process(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (!ItemUtil.isHoldingItem(player, itemType)) {
            return;
        }

        event.setCancelled(true);

        if (reentrantDelay.contains(player.getUniqueId())) {
            return;
        }

        reentrantDelay.add(player.getUniqueId());
        server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            reentrantDelay.remove(player.getUniqueId());
        }, 2);

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
                handleRightClick(player, event);
                break;
            case LEFT_CLICK_BLOCK:
                handleLeftClick(player);
                break;
        }
    }

    private int getDist(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Distance"));
    }

    private int getMaxDist(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Max Distance"));
    }

    private void handleRightClick(Player player, PlayerInteractEvent event) {
        BlockFace clickedFace = event.getBlockFace();
        Block curTarget = event.getClickedBlock().getRelative(clickedFace);

        BuildToolUseEvent useEvent = new BuildToolUseEvent(player, curTarget.getLocation(), itemType);
        callEvent(useEvent);
        if (useEvent.isCancelled()) {
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInOffHand();
        if (heldItem.getType() == Material.AIR || !heldItem.getType().isBlock()) {
            ChatUtil.sendError(player, "Put the block you'd like to place in your off-hand.");
            return;
        }

        // callEvent(new RapidBlockBreakEvent(player));

        ItemStack item = player.getInventory().getItemInMainHand();
        short degradation = 0;
        int unbreakingLevel = item.getEnchantmentLevel(Enchantment.DURABILITY);
        short curDur = item.getDurability();
        short maxDur = item.getType().getMaxDurability();
        short blocksPlaced = 0;
        for (int dist = getDist(item); dist > 0; ++blocksPlaced) {
            if (curTarget.getType() != Material.AIR) {
                break;
            }

            if (blocksPlaced > heldItem.getAmount()) {
                break;
            }

            if (curDur + degradation > maxDur) {
                break;
            }

            if (placeBlock(curTarget, player, clickedFace.getOppositeFace(), heldItem)) {
                if (ChanceUtil.getChance(unbreakingLevel + 1)) {
                    ++degradation;
                }
                --dist;
            } else {
                break;
            }

            curTarget = curTarget.getRelative(clickedFace);
        }

        if (curDur + degradation >= maxDur) {
            player.setItemInHand(null);
        } else {
            item.setDurability((short) (curDur + degradation));
        }

        if (heldItem.getAmount() <= blocksPlaced) {
            player.getInventory().setItemInOffHand(null);
        } else {
            heldItem.setAmount(heldItem.getAmount() - blocksPlaced);
        }
    }

    private void handleLeftClick(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

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
        player.setItemInHand(result);
    }

    private boolean placeBlock(Block b, Player p, BlockFace oppositeFace, ItemStack i) {
        BlockState oldState = b.getState();

        b.setType(i.getType(), true);

        BlockPlaceEvent event = new BlockPlaceEvent(
                b,
                oldState,
                b.getRelative(oppositeFace),
                i,
                p,
                true,
                EquipmentSlot.OFF_HAND
        );

        callEvent(event);

        if (event.isCancelled()) {
            oldState.update(true, false);
            return false;
        }

        return true;
    }
}
