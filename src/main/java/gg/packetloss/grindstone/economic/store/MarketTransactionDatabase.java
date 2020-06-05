/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import org.bukkit.entity.Player;

import java.util.List;

public interface MarketTransactionDatabase {
    /**
     * Load the database.
     *
     * @return whether the operation was fully successful
     */
    boolean load();

    /**
     * Save the database.
     *
     * @return whether the operation was fully successful
     */
    boolean save();

    void logPurchaseTransaction(Player player, MarketTransactionLine transactionLine);
    void logSaleTransaction(Player player, MarketTransactionLine transactionLine);

    List<ItemTransaction> getTransactions();
    List<ItemTransaction> getTransactions(String itemName, String playerName);
}
