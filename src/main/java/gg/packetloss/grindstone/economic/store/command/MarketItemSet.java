package gg.packetloss.grindstone.economic.store.command;

import gg.packetloss.grindstone.economic.store.MarketItem;

import java.util.Iterator;
import java.util.Set;

public class MarketItemSet implements Iterable<MarketItem> {
    private Set<MarketItem> items;

    public MarketItemSet(Set<MarketItem> items) {
        this.items = items;
    }

    public Set<MarketItem> getItems() {
        return items;
    }

    @Override
    public Iterator<MarketItem> iterator() {
        return items.iterator();
    }
}
