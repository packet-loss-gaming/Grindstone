/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ItemStoreDatabase {

    /**
     * Load the item database.
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

    void updatePrices(int restockingRounds);

    /**
     * Add/Set an item
     */
    void addItem(String itemName, double price, boolean infinite, boolean disableBuy, boolean disableSell);

    void removeItem(String itemName);

    /**
     * Change the stock of an item
     */
    void adjustStocksForBuy(List<MarketTransactionLine> transactionLines);
    void adjustStocksForSale(List<MarketTransactionLine> transactionLines);

    /**
     * Gets the item that was requested
     *
     * @param name the name of the item
     * @return the MarketItemInfo that was requested or null if nothing was found
     */
    MarketItemInfo getItem(String name);

    /**
     * Gets the items that were requested.
     *
     * @param names the names of the items to lookup
     * @return a mapping of names to ItemPricePairs
     */
    Map<String, MarketItemInfo> getItems(Collection<String> names);

    /**
     * Returns a list of items
     *
     * @return A list of items
     */
    List<MarketItemInfo> getItemList();

    /**
     * Returns a list of items
     *
     * @param filter     the item name must start with this to be returned
     * @param showHidden return items which are database only
     * @return A list of items
     */
    List<MarketItemInfo> getItemList(String filter, boolean showHidden);
}
