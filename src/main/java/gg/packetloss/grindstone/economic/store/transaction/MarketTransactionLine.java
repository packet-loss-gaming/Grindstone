/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.transaction;

import gg.packetloss.grindstone.economic.store.MarketItem;

public class MarketTransactionLine {
    private final MarketItem item;
    private final int amount;

    protected MarketTransactionLine(MarketItem item, int amount) {
        this.item = item;
        this.amount = amount;
    }

    public MarketItem getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }
}
