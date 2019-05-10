/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import java.util.List;

public interface ItemStoreDatabase {

    /**
     * Load the item database.
     *
     * @return whether the operation was fully successful
     */
    public boolean load();

    /**
     * Save the database.
     *
     * @return whether the operation was fully successful
     */
    public boolean save();

    /**
     * Add/Set an item
     */
    public void addItem(String playerName, String itemName, double price, boolean disableBuy, boolean disableSell);

    public void removeItem(String playerName, String itemName);

    /**
     * Gets the item that was requested
     *
     * @param name the name of the item
     * @return the ItemPricePair that was requested or null if nothing was found
     */
    public ItemPricePair getItem(String name);

    /**
     * Returns a list of items
     *
     * @param filter the item name must start with this to be returned
     * @return A list of items
     */
    public List<ItemPricePair> getItemList();

    /**
     * Returns a list of items
     *
     * @param filter     the item name must start with this to be returned
     * @param showHidden return items which are database only
     * @return A list of items
     */
    public List<ItemPricePair> getItemList(String filter, boolean showHidden);
}
