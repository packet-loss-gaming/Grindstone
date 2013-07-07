package com.skelril.aurora.economic.store;

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
    public void addItem(String playerName, String itemName, double price);

    public void removeItem(String playerName, String itemName);

    public void logTransaction(String playerName, String itemName, int amount);

    /**
     * Gets the item that was requested
     *
     * @param name the name of the item
     * @return the ItemPricePair that was requested or null if nothing was found
     */
    public ItemPricePair getItem(String name);

    /**
     * Returns a list of inmates
     *
     * @return A list of inmates
     */
    public List<ItemPricePair> getItemList();
}
