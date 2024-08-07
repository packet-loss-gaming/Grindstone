/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.support;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

import static com.zachsthings.libcomponents.bukkit.BasePlugin.callEvent;

public abstract class RadialExecutor {
    private CustomItems itemType;

    public RadialExecutor(CustomItems itemType) {
        this.itemType = itemType;
    }

    public abstract boolean accepts(Block block);

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

    private int getRadius(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Radius"));
    }

    private int getMaxRadius(ItemStack item) {
        return Integer.parseInt(ItemUtil.getItemTags(item).get(ChatColor.RED + "Max Radius"));
    }

    private void handleRightClick(Player player) {
        if (!impedeRightClick(player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        final int radius = getRadius(item);
        final int maxRadius = getMaxRadius(item);
        final short dur = item.getDurability();
        Map<Enchantment, Integer> enchants = item.getItemMeta().getEnchants();

        CustomItem cItem = CustomItemCenter.get(itemType);
        for (Tag tag : cItem.getTags()) {
            if (tag.getKey().equals("Radius")) {
                int newRadius = radius + 1;
                if (newRadius > maxRadius) {
                    newRadius = 0;
                }
                tag.setProp(String.valueOf(newRadius));
                ChatUtil.sendNotice(player, "Radius set to: " + newRadius);
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
        Block clicked = event.getClickedBlock();

        BuildToolUseEvent useEvent = new BuildToolUseEvent(player, clicked.getLocation(), itemType);
        callEvent(useEvent);
        if (useEvent.isCancelled()) {
            return;
        }

        final World world = clicked.getWorld();

        if (!accepts(clicked)) return;

        event.setCancelled(true);

        ItemStack item = player.getInventory().getItemInMainHand();
        final int radius = getRadius(item);

        BlockVector3 min = BlockVector3.at(clicked.getX(), clicked.getY(), clicked.getZ());
        BlockVector3 max = BlockVector3.at(clicked.getX(), clicked.getY(), clicked.getZ());
        switch (event.getBlockFace()) {
            case NORTH:
            case SOUTH:
                min = min.add(BlockVector3.at(
                        BlockFace.EAST.getModX(),
                        BlockFace.DOWN.getModY(),
                        BlockFace.EAST.getModZ()
                ).multiply(radius));
                max = max.add(BlockVector3.at(
                        BlockFace.WEST.getModX(),
                        BlockFace.UP.getModY(),
                        BlockFace.WEST.getModZ()
                ).multiply(radius));
                break;
            case EAST:
            case WEST:
                min = min.add(BlockVector3.at(
                        BlockFace.NORTH.getModX(),
                        BlockFace.DOWN.getModY(),
                        BlockFace.NORTH.getModZ()
                ).multiply(radius));
                max = max.add(BlockVector3.at(
                        BlockFace.SOUTH.getModX(),
                        BlockFace.UP.getModY(),
                        BlockFace.SOUTH.getModZ()
                ).multiply(radius));
                break;
            case UP:
            case DOWN:
                min = min.add(BlockVector3.at(
                        BlockFace.NORTH_EAST.getModX(),
                        0,
                        BlockFace.NORTH_EAST.getModZ()
                ).multiply(radius));
                max = max.add(BlockVector3.at(
                        BlockFace.SOUTH_WEST.getModX(),
                        0,
                        BlockFace.SOUTH_WEST.getModZ()
                ).multiply(radius));
                break;
            default:
                return;
        }

        CuboidRegion region = new CuboidRegion(min, max);
        min = region.getMinimumPoint();
        max = region.getMaximumPoint();

        final Material initialType = clicked.getType();

        // callEvent(new RapidBlockBreakEvent(player));
        short degradation = 0;
        int unbreakingLevel = item.getEnchantmentLevel(Enchantment.UNBREAKING);
        short curDur = item.getDurability();
        short maxDur = item.getType().getMaxDurability();
        blockBreaker:
        {
            for (int x = min.getBlockX(); x <= max.getBlockX(); ++x) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); ++z) {
                    for (int y = min.getBlockY(); y <= max.getBlockY(); ++y) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() != initialType) continue;

                        if (curDur + degradation > maxDur) {
                            break blockBreaker;
                        }

                        if (breakBlock(block, player, item)) {
                            if (ChanceUtil.getChance(unbreakingLevel + 1)) {
                                ++degradation;
                            }
                        }
                    }
                }
            }
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
