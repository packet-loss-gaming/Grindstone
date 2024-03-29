/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.custom.CustomItem;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.Tag;
import gg.packetloss.grindstone.items.flight.FlightCategory;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class MagicBucketImpl extends AbstractItemFeatureImpl {
    public static final MagicBucketSpeed DEFAULT_SPEED = MagicBucketSpeed.FAST;

    private MagicBucketSpeed getHeldBucketSpeed(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        Map<String, String> itemTags = ItemUtil.getItemTags(item);
        if (itemTags == null) {
            return DEFAULT_SPEED;
        }

        String value = itemTags.get(ChatColor.GREEN + "Speed");
        if (value == null) {
            return DEFAULT_SPEED;
        }

        return MagicBucketSpeed.valueOf(value);
    }

    private boolean grantFlight(Player player) {
        ChatUtil.sendNotice(player, "The bucket glows brightly.");

        FlightCategory flightCategory = getHeldBucketSpeed(player).getCategory();
        player.setFlySpeed(flightCategory.getSpeed());
        player.setAllowFlight(true);
        flightItems.registerFlightProvider(player, flightCategory);

        return true;
    }

    private boolean takeFlight(Player player) {
        if (GeneralPlayerUtil.takeFlightSafely(player)) {
            ChatUtil.sendNotice(player, "The power of the bucket fades.");
            return true;
        }
        return false;
    }

    private boolean updateBucketSpeed(Player player) {
        MagicBucketSpeed speed = getHeldBucketSpeed(player);

        CustomItem cItem = CustomItemCenter.get(CustomItems.MAGIC_BUCKET);
        for (Tag tag : cItem.getTags()) {
            if (tag.getKey().equals("Speed")) {
                int newSpeedIndex = (speed.ordinal() + 1) % MagicBucketSpeed.values().length;
                MagicBucketSpeed newSpeed = MagicBucketSpeed.values()[newSpeedIndex];

                tag.setProp(newSpeed.name());

                ChatUtil.sendNotice(player, "Speed set to: " + newSpeed.name());
            }
        }

        player.getInventory().setItemInMainHand(cItem.build());

        return true;
    }

    private boolean handleRightClick(Player player) {
        if (player.getAllowFlight()) {
            return takeFlight(player);
        } else {
            if (player.isSneaking()) {
                return updateBucketSpeed(player);
            } else {
                return grantFlight(player);
            }
        }
    }

    private boolean isHoldingMagicBucket(Player player) {
        return ItemUtil.isHoldingItem(player, CustomItems.MAGIC_BUCKET);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isHoldingMagicBucket(player)) {
            return false;
        }

        if (handleRightClick(player)) {
            event.setCancelled(true);
            return true;
        }

        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();

        if (!isHoldingMagicBucket(player)) {
            return;
        }

        if (event.getRightClicked() instanceof Cow) {
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                // Swap the magic bucket for mad milk
                if (!ItemUtil.swapItem(player.getInventory(), CustomItems.MAGIC_BUCKET, CustomItems.MAD_MILK)) {
                    ChatUtil.sendError(player, "Your inventory is too full!");
                    return;
                }

                // Since the player's magic bucket is now mad milk,
                // if they don't have another one, take their flight.
                if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
                    takeFlight(player);
                }
            }, 1);
            event.setCancelled(true);
            return;
        }

        if (handleRightClick(player)) {
            event.setCancelled(true);
        }

        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), player::updateInventory, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItemStack();

        if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET) || isHoldingMagicBucket(player)) {
            event.setCancelled(true);

            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), player::updateInventory, 1);
        }

        if (isHoldingMagicBucket(player) && handleRightClick(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET)) {
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
                    takeFlight(player);
                }
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        if (!player.getAllowFlight()) return;

        @NotNull Inventory chestContents = event.getInventory();
        if (!ItemUtil.hasItem(chestContents, CustomItems.MAGIC_BUCKET)) return;

        if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET) && !GeneralPlayerUtil.hasFlyingGamemode(player)) {
            takeFlight(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        @NotNull List<ItemStack> drops = event.getDrops();

        if (ItemUtil.hasItem(drops, CustomItems.MAGIC_BUCKET)) {
            takeFlight(player);
        }
    }

    public enum MagicBucketSpeed {
        SLOW(FlightCategory.MAGIC_BUCKET_SLOW),
        MEDIUM(FlightCategory.MAGIC_BUCKET_MEDIUM),
        FAST(FlightCategory.MAGIC_BUCKET_FAST);

        private final FlightCategory category;

        private MagicBucketSpeed(FlightCategory category) {
            this.category = category;
        }

        public FlightCategory getCategory() {
            return category;
        }
    }
}
