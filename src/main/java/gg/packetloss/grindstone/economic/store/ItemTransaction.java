/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store;

public class ItemTransaction {
    private final String player;
    private final MarketItemInfo item;
    private final int amount;

    public ItemTransaction(String player, MarketItemInfo item, int amount) {
        this.player = player;
        this.item = item;
        this.amount = amount;
    }

    public String getPlayer() {
        return player;
    }

    public MarketItem getItem() {
        return new MarketItem(item);
    }

    public int getAmount() {
        return amount;
    }
}
