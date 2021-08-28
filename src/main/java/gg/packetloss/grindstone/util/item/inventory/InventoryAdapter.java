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

    public default ItemStack add(ItemStack itemStack) {
        int remainder = itemStack.getAmount();
        for (int i = 0; i < size(); ++i) {
            ItemStack currenItem = getAt(i);

            // Setup state counts
            int currentlyUsedSpace = 0;
            int availableSpace = itemStack.getType().getMaxStackSize();

            // Setup new item
            ItemStack newItem;
            if (currenItem != null) {
                // If something else is here that's not the same stack in every way but quantity, continue
                if (!currenItem.isSimilar(itemStack)) {
                    continue;
                }

                // Update state counts
                currentlyUsedSpace = currenItem.getAmount();
                availableSpace -= currentlyUsedSpace;

                // Reuse the current item
                newItem = currenItem;
            } else {
                // Use a copy of the item we're trying to add
                newItem = itemStack.clone();
            }

            // Figure out how much we're adding, if nothing continue
            int amountToAdd = Math.min(remainder, availableSpace);
            if (amountToAdd == 0) {
                continue;
            }

            // Update the item and the adapter state
            newItem.setAmount(currentlyUsedSpace + amountToAdd);
            setAt(i, newItem);

            // Update the remainder and see if we're done
            remainder -= amountToAdd;
            if (remainder == 0) {
                break;
            }
        }

        // We have no remainder, return nothing
        if (remainder == 0) {
            return null;
        }

        // Return what remains of the item/what couldn't be added
        ItemStack remainderStack = itemStack.clone();
        remainderStack.setAmount(remainder);
        return remainderStack;
    }

    InventoryAdapter copy();

    boolean applyChanges();
}
