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
