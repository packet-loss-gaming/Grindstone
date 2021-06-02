/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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

    public int size() {
        return items.size();
    }

    @Override
    public Iterator<MarketItem> iterator() {
        return items.iterator();
    }
}
