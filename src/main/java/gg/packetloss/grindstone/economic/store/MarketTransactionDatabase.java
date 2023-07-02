/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MarketTransactionDatabase {
    void logPurchaseTransactions(UUID playerID, Collection<MarketTransactionLine> transactionLines);
    void logSaleTransactions(UUID playerID, Collection<MarketTransactionLine> transactionLines);

    List<ItemTransaction> getTransactions(@Nullable String itemName, @Nullable UUID playerID);
}
