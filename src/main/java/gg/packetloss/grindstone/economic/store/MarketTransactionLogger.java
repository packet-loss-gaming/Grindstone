/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import gg.packetloss.grindstone.events.economy.MarketPurchaseEvent;
import gg.packetloss.grindstone.events.economy.MarketSellEvent;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MarketTransactionLogger implements Listener {
    private MarketTransactionDatabase transactionDatabase;

    public MarketTransactionLogger(MarketTransactionDatabase transactionDatabase) {
        this.transactionDatabase = transactionDatabase;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMarketPurchase(MarketPurchaseEvent event) {
        Player player = event.getPlayer();
        PluginTaskExecutor.submitAsync(() -> {
            for (MarketTransactionLine transactionLine : event.getTransactionLines()) {
                transactionDatabase.logPurchaseTransaction(player, transactionLine);
            }
            transactionDatabase.save();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMarketSale(MarketSellEvent event) {
        Player player = event.getPlayer();
        PluginTaskExecutor.submitAsync(() -> {
            for (MarketTransactionLine transactionLine : event.getTransactionLines()) {
                transactionDatabase.logSaleTransaction(player, transactionLine);
            }
            transactionDatabase.save();
        });
    }
}
