/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import com.skelril.aurora.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemCondenser {

    private Map<ItemStack, ItemStack> supported = new HashMap<>();
    private List<Step> conversionSteps = new ArrayList<>();

    public void addSupport(ItemStack from, ItemStack to) {
        supported.put(from, to);
        compile();
    }

    public boolean supports(ItemStack itemStack) {
        for (ItemStack is : supported.keySet()) {
            if (itemStack.isSimilar(is)) {
                return true;
            }
        }
        return true;
    }

    /**
     *
     * @param itemStacks - the old item stacks
     * @return the new item stacks, or null if the operation could not be completed/did nothing
     */
    public ItemStack[] operate(ItemStack[] itemStacks) {
        itemStacks = ItemUtil.clone(itemStacks); // Make sure we're working in our domain here
        int modified = 0;
        for (Step step : conversionSteps) {
            int total = 0;
            List<Integer> positions = new ArrayList<>();
            ItemStack target = step.getOldItem();
            for (int i = 0; i < itemStacks.length; ++i) {
                ItemStack cur = itemStacks[i];
                if (target.isSimilar(cur)) {
                    total += cur.getAmount();
                    positions.add(i);
                }
            }

            int divisor = step.getOldItem().getAmount();
            int newAmt = (total / divisor) * step.getNewItem().getAmount();

            if (newAmt < 1) continue;

            int oldAmt = total % divisor;

            for (Integer pos : positions) {
                itemStacks[pos] = null;
            }

            final ItemStack newStack = step.getNewItem();
            final ItemStack oldStack = step.getOldItem();

            for (int i = 0; i < itemStacks.length; ++i) {
                final ItemStack stack = itemStacks[i];
                int startingAmt = stack == null ? 0 : stack.getAmount();
                int quantity;
                if (newAmt > 0 && (startingAmt == 0 || newStack.isSimilar(stack))) {
                    quantity = Math.min(newAmt, newStack.getMaxStackSize());
                    newAmt -= quantity - startingAmt;
                    itemStacks[i] = newStack.clone();
                } else if (oldAmt > 0 && (startingAmt == 0 || oldStack.isSimilar(stack))) {
                    quantity = Math.min(oldAmt, oldStack.getMaxStackSize());
                    oldAmt -= quantity - startingAmt;
                    itemStacks[i] = oldStack.clone();
                } else if (newAmt == 0 && oldAmt == 0) {
                    break;
                } else {
                    continue;
                }
                itemStacks[i].setAmount(quantity);
            }
            // There is an illegal remainder
            if (newAmt > 0 || oldAmt > 0) return null;
            ++modified;
        }
        return modified == 0 ? null : itemStacks;
    }

    private void compile() {
        conversionSteps.clear();
        for (Map.Entry<ItemStack, ItemStack> entry : supported.entrySet()) {
            addStep:
            {
                Step step = new Step(entry.getKey(), entry.getValue());
                for (int i = 0; i < conversionSteps.size(); ++i) {
                    // If the step's needed item, is what this makes, put this in front
                    if (conversionSteps.get(i).getOldItem().equals(step.getNewItem())) {
                        conversionSteps.add(i, step);
                        break addStep;
                    }
                }
                conversionSteps.add(step);
            }
        }
    }

    private static class Step {
        private ItemStack oldItem;
        private ItemStack newItem;

        private Step(ItemStack oldItem, ItemStack newItem) {
            this.oldItem = oldItem;
            this.newItem = newItem;
        }

        public ItemStack getOldItem() {
            return oldItem.clone();
        }

        public ItemStack getNewItem() {
            return newItem.clone();
        }
    }
}
