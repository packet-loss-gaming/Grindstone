/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

public class SacrificeResult implements Iterable<ItemStack> {
    private List<ItemStack> itemStacks;
    private BigDecimal remainingValue;
    private int remainingItems;

    public SacrificeResult(List<ItemStack> itemStacks, BigDecimal remainingValue, int remainingItems) {
        this.itemStacks = itemStacks;
        this.remainingValue = remainingValue;
        this.remainingItems = remainingItems;
    }

    public List<ItemStack> getItemStacks() {
        return itemStacks;
    }

    public BigDecimal getRemainingValue() {
        return remainingValue;
    }

    public int getRemainingItems() {
        return remainingItems;
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return itemStacks.iterator();
    }
}
