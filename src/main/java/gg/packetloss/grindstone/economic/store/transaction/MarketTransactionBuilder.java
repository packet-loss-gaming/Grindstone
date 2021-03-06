/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.transaction;

import gg.packetloss.grindstone.economic.store.MarketItem;
import org.apache.commons.lang.Validate;

import java.util.*;

public class MarketTransactionBuilder {
    private Set<String> itemNames = new HashSet<>();
    private Map<MarketItem, Integer> entries = new HashMap<>();

    public void add(MarketItem item, int amount) {
        Validate.isTrue(amount > 0);

        entries.merge(item, amount, Integer::sum);
        itemNames.add(item.getName());

        Validate.isTrue(itemNames.size() == entries.size());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<MarketTransactionLine> toTransactionLines() {
        List<MarketTransactionLine> transactionLines = new ArrayList<>(entries.size());
        entries.forEach((item, amount) -> transactionLines.add(new MarketTransactionLine(item, amount)));
        return transactionLines;
    }
}
