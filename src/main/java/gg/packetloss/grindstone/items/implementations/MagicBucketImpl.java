/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.custom.CustomItems;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MagicBucketImpl extends AbstractItemFeatureImpl {

    public boolean handleRightClick(final Player player) {

        if (admin.isAdmin(player)) return false;

        player.setAllowFlight(!player.getAllowFlight());
        if (player.getAllowFlight()) {
            player.setFlySpeed(.4F);
            // antiCheat.exempt(player, CheckType.FLY);
            ChatUtil.sendNotice(player, "The bucket glows brightly.");
        } else {
            player.setFlySpeed(.1F);
            // antiCheat.unexempt(player, CheckType.FLY);
            ChatUtil.sendNotice(player, "The power of the bucket fades.");
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET) && handleRightClick(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        if (event.getRightClicked() instanceof Cow && ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET)) {
            server.getScheduler().runTaskLater(inst, () -> {
                if (!ItemUtil.swapItem(player.getInventory(), CustomItems.MAGIC_BUCKET, CustomItems.MAD_MILK)) {
                    ChatUtil.sendError(player, "Your inventory is too full!");
                    return;
                }
                if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
                    if (player.getAllowFlight()) {
                        ChatUtil.sendNotice(player, "The power of the bucket fades.");
                    }
                    player.setAllowFlight(false);
                }
            }, 1);
            event.setCancelled(true);
            return;
        }

        if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET) && handleRightClick(player)) {
            event.setCancelled(true);
        }
        //noinspection deprecation
        server.getScheduler().runTaskLater(inst, player::updateInventory, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET) && handleRightClick(player)) {
            event.setCancelled(true);
        }
        //noinspection deprecation
        server.getScheduler().runTaskLater(inst, player::updateInventory, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (ItemUtil.isItem(itemStack, CustomItems.MAGIC_BUCKET)) {
            server.getScheduler().runTaskLater(inst, () -> {
                if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
                    if (player.getAllowFlight()) {
                        ChatUtil.sendNotice(player, "The power of the bucket fades.");
                    }
                    player.setAllowFlight(false);
                    // antiCheat.unexempt(player, CheckType.FLY);
                }
            }, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        if (!player.getAllowFlight()) return;

        ItemStack[] chestContents = event.getInventory().getContents();
        if (!ItemUtil.findItemOfName(chestContents, CustomItems.MAGIC_BUCKET.toString())) return;

        if (!ItemUtil.hasItem(player, CustomItems.MAGIC_BUCKET)) {
            if (player.getAllowFlight()) {
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            player.setAllowFlight(false);
            // antiCheat.unexempt(player, CheckType.FLY);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        ItemStack[] drops = event.getDrops().toArray(new ItemStack[event.getDrops().size()]);

        if (ItemUtil.findItemOfName(drops, CustomItems.MAGIC_BUCKET.toString())) {
            if (player.getAllowFlight()) {
                ChatUtil.sendNotice(player, "The power of the bucket fades.");
            }
            player.setAllowFlight(false);
            // antiCheat.unexempt(player, CheckType.FLY);
        }
    }
}
