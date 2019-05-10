/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

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
