/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import org.bukkit.inventory.ItemStack;

public interface InventoryAdapter {
    int size();

    ItemStack getAt(int index);
    void setAt(int index, ItemStack stack);

    InventoryAdapter copy();

    void applyChanges();
}
