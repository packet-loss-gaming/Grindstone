/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.custom.CustomItemCenter;
import gg.packetloss.grindstone.util.item.custom.CustomItems;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class PixieDustImpl extends AbstractItemFeatureImpl {

    private List<String> players = new ArrayList<>();

    public boolean handleRightClick(final Player player) {

        if (admin.isAdmin(player)) return false;

        final long currentTime = System.currentTimeMillis();

        if (player.getAllowFlight()) return false;

        if (players.contains(player.getName())) {
            ChatUtil.sendError(player, "You need to wait to regain your faith, and trust.");
            return false;
        }

        player.setAllowFlight(true);
        player.setFlySpeed(.6F);
        // antiCheat.exempt(player, CheckType.FLY);

        ChatUtil.sendNotice(player, "You use the Pixie Dust to gain flight.");

        IntegratedRunnable integratedRunnable = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {

                // Just get out of here you stupid players who don't exist!
                if (!player.isValid()) return true;

                if (player.getAllowFlight()) {
                    int c = ItemUtil.countItemsOfName(player.getInventory().getContents(), CustomItems.PIXIE_DUST.toString()) - 1;

                    if (c >= 0) {
                        ItemStack[] pInventory = player.getInventory().getContents();
                        pInventory = ItemUtil.removeItemOfName(pInventory, CustomItems.PIXIE_DUST.toString());
                        player.getInventory().setContents(pInventory);

                        int amount = Math.min(c, 64);
                        while (amount > 0) {
                            player.getInventory().addItem(CustomItemCenter.build(CustomItems.PIXIE_DUST, amount));
                            c -= amount;
                            amount = Math.min(c, 64);
                        }

                        //noinspection deprecation
                        player.updateInventory();

                        if (System.currentTimeMillis() >= currentTime + 13000) {
                            ChatUtil.sendNotice(player, "You use some more Pixie Dust to keep flying.");
                        }
                        return false;
                    }
                    ChatUtil.sendWarning(player, "The effects of the Pixie Dust are about to wear off!");
                }
                return true;
            }

            @Override
            public void end() {

                if (player.isValid()) {
                    if (player.getAllowFlight()) {
                        ChatUtil.sendNotice(player, "You are no longer influenced by the Pixie Dust.");
                        // antiCheat.unexempt(player, CheckType.FLY);
                    }
                    player.setFallDistance(0);
                    player.setAllowFlight(false);
                    player.setFlySpeed(.1F);
                }
            }
        };

        TimedRunnable runnable = new TimedRunnable(integratedRunnable, 1);
        BukkitTask task = server.getScheduler().runTaskTimer(inst, runnable, 0, 20 * 15);
        runnable.setTask(task);
        return true;
    }


    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if (ItemUtil.isItem(itemStack, CustomItems.PIXIE_DUST) && handleRightClick(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        ItemStack itemStack = player.getItemInHand();

        if (ItemUtil.isItem(itemStack, CustomItems.PIXIE_DUST) && handleRightClick(player)) {
            event.setCancelled(true);
        }
        //noinspection deprecation
        server.getScheduler().runTaskLater(inst, player::updateInventory, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {

        Player player = event.getPlayer();

        if (event.isSneaking() && player.getAllowFlight() && player.isOnGround() && !admin.isAdmin(player)) {

            if (player.getFlySpeed() != .6F || !ItemUtil.hasItem(player, CustomItems.PIXIE_DUST)) return;

            player.setAllowFlight(false);
            // antiCheat.unexempt(player, CheckType.FLY);
            ChatUtil.sendNotice(player, "You are no longer influenced by the Pixie Dust.");

            final String playerName = player.getName();

            players.add(playerName);

            server.getScheduler().runTaskLater(inst, () -> players.remove(playerName), 20 * 30);
        }
    }
}
