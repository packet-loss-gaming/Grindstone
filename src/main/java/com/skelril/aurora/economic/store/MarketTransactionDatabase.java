/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.store;

import java.util.List;

public interface MarketTransactionDatabase {
    /**
     * Load the database.
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

    public void logTransaction(String playerName, String itemName, int amount);

    public List<ItemTransaction> getTransactions();
    public List<ItemTransaction> getTransactions(String itemName, String playerName);
}
