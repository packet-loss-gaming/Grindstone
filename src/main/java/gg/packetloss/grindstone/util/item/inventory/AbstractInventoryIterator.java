/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Iterator;

public abstract class AbstractInventoryIterator implements Iterator<ItemStack> {
    protected final PlayerInventory playerInventory;
    protected final int inventoryLength;

    protected int index;
    protected int itemNumber = 0;

    protected AbstractInventoryIterator(int startingIndex, int inventoryLength, PlayerInventory playerInventory) {
        this.inventoryLength = inventoryLength;
        this.index = startingIndex;
        this.playerInventory = playerInventory;
    }

    @Override
    public final boolean hasNext() {
        return itemNumber < inventoryLength;
    }

    protected void updateIndex() {
        ++index;
    }

    @Override
    public final ItemStack next() {
        updateIndex();
        ++itemNumber;
        return playerInventory.getItem(index);
    }

    public final void set(ItemStack itemStack) {
        playerInventory.setItem(index, itemStack);
    }

    public final void clear() {
        playerInventory.setItem(index, null);
    }
}
