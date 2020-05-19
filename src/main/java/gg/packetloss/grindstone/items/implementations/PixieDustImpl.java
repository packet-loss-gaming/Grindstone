/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.flight.FlightCategory;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ItemPointTranslator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStickyInventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PixieDustImpl extends AbstractItemFeatureImpl {
    private static final ItemPointTranslator PIXIE_DUST_CONVERTER = new ItemPointTranslator();

    static {
        PIXIE_DUST_CONVERTER.addMapping(CustomItemCenter.build(CustomItems.PIXIE_DUST), 1);
    }

    private static class PixieFlightData {
        private final static int FULL_CREDIT = 15;
        private int secondsOfCredit = FULL_CREDIT;

        public boolean needsRefresh() {
            return secondsOfCredit <= 0;
        }

        public int getSecondsOfCredit() {
            return secondsOfCredit;
        }

        public void setSecondsOfCredit(int secondsRemaining) {
            this.secondsOfCredit = secondsRemaining;
        }
    }

    private void maybeWarnPlayer(Player player, PixieFlightData flightData) {
        InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);

        int pixieDustCount = PIXIE_DUST_CONVERTER.calculateValue(adapter, false);
        if (pixieDustCount == 0) {
            ChatUtil.sendWarning(player, "You will be out of pixie dust in: " + flightData.getSecondsOfCredit() + " seconds");
        }
    }

    /**
     *
     * @param player
     * @return true if out of pixie dust
     */
    private boolean takeOnePixieDust(Player player) {
        InventoryAdapter adapter = new PlayerStickyInventoryAdapter(
                player,
                PlayerStickyInventoryAdapter.Priority.HOTBAR,
                (item -> ItemUtil.isItem(item, CustomItems.PIXIE_DUST))
        );

        int pixieDustCount = PIXIE_DUST_CONVERTER.calculateValue(adapter, true);

        // Welp, that's it, they're out of credit, and there's no dust to renew
        if (pixieDustCount == 0) {
            return true;
        }

        PIXIE_DUST_CONVERTER.assignValue(adapter, pixieDustCount - 1);
        adapter.applyChanges();

        return false;
    }

    public boolean handleRightClick(final Player player) {
        if (player.getAllowFlight()) {
            return false;
        }

        player.setFlySpeed(FlightCategory.PIXIE_DUST.getSpeed());
        player.setAllowFlight(true);
        flightItems.registerFlightProvider(player, FlightCategory.PIXIE_DUST);
        // antiCheat.exempt(player, CheckType.FLY);

        ChatUtil.sendNotice(player, "You use the Pixie Dust to gain flight.");
        takeOnePixieDust(player);

        TaskBuilder.Countdown builder = TaskBuilder.countdown();

        builder.setInterval(20);

        PixieFlightData flightData = new PixieFlightData();
        builder.setAction((times) -> {
            // Don't loop forever on a disconnected player
            if (!player.isValid()) {
                return true;
            }

            // Warn player if they're out of dust
            maybeWarnPlayer(player, flightData);

            // Decrement credits, this should be after the warning so that the warning is base 1, not base 0
            flightData.setSecondsOfCredit(flightData.getSecondsOfCredit() - 1);

            // Attempt a refresh if necessary
            if (flightData.needsRefresh()) {
                // They've landed, or they're getting flight elsewhere, don't worry about a refresh
                boolean landed = !player.isFlying() && GeneralPlayerUtil.isStandingOnSolidGround(player);
                if (landed || GeneralPlayerUtil.isFlyingGamemode(player.getGameMode())) {
                    return true;
                }

                // Welp, that's it, they're out of credit, and there's no dust to renew
                if (takeOnePixieDust(player)) {
                    return true;
                }

                flightData.setSecondsOfCredit(PixieFlightData.FULL_CREDIT);

                ChatUtil.sendNotice(player, "You use some more Pixie Dust to keep flying.");
            }

            // They have credit, continue
            return false;
        });
        builder.setFinishAction(() -> {
            if (!player.isValid()) {
                return;
            }

            if (GeneralPlayerUtil.takeFlightSafely(player)) {
                ChatUtil.sendNotice(player, "You are no longer influenced by the Pixie Dust.");
                // antiCheat.unexempt(player, CheckType.FLY);
            }
        });

        builder.build();

        return true;
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!ItemUtil.isHoldingItem(player, CustomItems.PIXIE_DUST)) {
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
        ItemStack itemStack = player.getItemInHand();

        if (ItemUtil.isItem(itemStack, CustomItems.PIXIE_DUST) && handleRightClick(player)) {
            event.setCancelled(true);
        }
        //noinspection deprecation
        server.getScheduler().runTaskLater(inst, player::updateInventory, 1);
    }
}
