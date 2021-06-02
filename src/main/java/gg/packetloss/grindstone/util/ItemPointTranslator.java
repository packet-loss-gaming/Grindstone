/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemPointTranslator {
    private List<PointMapping> pointMappings = new ArrayList<>();

    public void addMapping(ItemStack item, int value) {
        pointMappings.add(new PointMapping(item, value));
        compile();
    }

    public int calculateValue(InventoryAdapter adapter, boolean removeWhileCounting) {
        int value = 0;

        for (int i = 0; i < adapter.size(); ++i) {
            ItemStack curStack = adapter.getAt(i);
            for (PointMapping pointMapping : pointMappings) {
                if (pointMapping.getItem().isSimilar(curStack)) {
                    value += curStack.getAmount() * pointMapping.getValue();
                    if (removeWhileCounting) {
                        adapter.setAt(i, null);
                    }
                    break;
                }
            }
        }

        return value;
    }

    public void streamValue(int value, Consumer<ItemStack> itemStackConsumer) {
        for (PointMapping pointMapping : pointMappings) {
            ItemStack targetStack = pointMapping.getItem();
            int targetValue = pointMapping.getValue();

            while (value >= targetValue) {
                int quantity = Math.min(value / targetValue, targetStack.getMaxStackSize());
                value -= quantity * targetValue;

                ItemStack newStack = targetStack.clone();
                newStack.setAmount(quantity);

                itemStackConsumer.accept(newStack);

                // Stop early if we no longer have anything to add
                if (value == 0) {
                    break;
                }
            }
        }
    }

    public int assignValue(InventoryAdapter adapter, int value) {
        for (PointMapping pointMapping : pointMappings) {
            ItemStack targetStack = pointMapping.getItem();
            for (int i = 0; i < adapter.size(); ++i) {
                final ItemStack stack = adapter.getAt(i);
                int startingAmt = stack == null ? 0 : stack.getAmount();
                int targetValue = pointMapping.getValue();

                if (value >= targetValue && (startingAmt == 0 || targetStack.isSimilar(stack))) {
                    int quantity = Math.min(value / targetValue, targetStack.getMaxStackSize());
                    value -= quantity * targetValue;

                    ItemStack newStack = targetStack.clone();
                    newStack.setAmount(quantity);
                    adapter.setAt(i, newStack);

                    // Stop early if we no longer have anything to add
                    if (value == 0) {
                        break;
                    }
                }
            }
        }
        return value;
    }

    private void compile() {
        pointMappings.sort((a, b) -> b.getValue() - a.getValue());
    }

    private static class PointMapping {
        private ItemStack item;
        private int value;

        private PointMapping(ItemStack item, int value) {
            this.item = item;
            this.value = value;
        }

        public ItemStack getItem() {
            return item;
        }

        public int getValue() {
            return value;
        }
    }
}
