/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DirectInventoryAdapter implements InventoryAdapter {
    private final Inventory inventory;

    private final ItemStack[] itemStacks;
    private final boolean[] updateMask;

    public DirectInventoryAdapter(Inventory inventory) {
        this.inventory = inventory;

        this.itemStacks = inventory.getContents();
        this.updateMask = new boolean[itemStacks.length];
    }

    private DirectInventoryAdapter(DirectInventoryAdapter adapter) {
        this.inventory = adapter.inventory;

        this.itemStacks = ItemUtil.clone(adapter.itemStacks);
        this.updateMask = adapter.updateMask.clone();
    }

    @Override
    public int size() {
        return itemStacks.length;
    }

    @Override
    public ItemStack getAt(int index) {
        return itemStacks[index];
    }

    @Override
    public void setAt(int index, ItemStack stack) {
        itemStacks[index] = stack;
        updateMask[index] = true;
    }

    @Override
    public DirectInventoryAdapter copy() {
        return new DirectInventoryAdapter(this);
    }

    @Override
    public boolean applyChanges() {
        boolean anyChanged = false;
        for (int i = 0; i < size(); ++i) {
            if (updateMask[i]) {
                anyChanged = true;
                inventory.setItem(i, itemStacks[i]);
            }
        }
        return anyChanged;
    }
}
