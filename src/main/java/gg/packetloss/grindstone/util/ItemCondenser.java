/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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
        List<Remainder> remainders = new ArrayList<>();
        int modified = 0;
        for (Step step : conversionSteps) {
            int total = 0;
            ItemStack target = step.getOldItem();
            for (int i = 0; i < itemStacks.length; ++i) {
                ItemStack cur = itemStacks[i];
                if (target.isSimilar(cur)) {
                    total += cur.getAmount();
                    itemStacks[i] = null;
                }
            }

            // Add in any remainders from prior steps
            Iterator<Remainder> it = remainders.iterator();
            while (it.hasNext()) {
                Remainder remainder = it.next();
                if (step.getOldItem().isSimilar(remainder.getItem())) {
                    total += remainder.getAmount();
                    it.remove();
                }
            }

            int divisor = step.getOldItem().getAmount();

            int newAmt = (total / divisor) * step.getNewItem().getAmount();
            if (newAmt > 0) {
                final ItemStack newStack = step.getNewItem();
                remainders.add(new Remainder(newStack, newAmt));
            }

            int oldAmt = total % divisor;
            if (oldAmt > 0) {
                ItemStack oldStack = step.getOldItem();
                remainders.add(new Remainder(oldStack, oldAmt));
            }

            modified += newAmt;
        }

        for (Remainder remainder : remainders) {
            ItemStack rStack = remainder.getItem();
            for (int i = 0; i < itemStacks.length; ++i) {
                final ItemStack stack = itemStacks[i];
                int startingAmt = stack == null ? 0 : stack.getAmount();
                int rAmt = remainder.getAmount();

                if (rAmt > 0 && (startingAmt == 0 || rStack.isSimilar(stack))) {
                    int quantity = Math.min(rAmt + startingAmt, rStack.getMaxStackSize());
                    rAmt -= quantity - startingAmt;
                    itemStacks[i] = rStack.clone();
                    itemStacks[i].setAmount(quantity);
                    remainder.setAmount(rAmt);

                    // Stop early if we no longer have anything to add
                    if (rAmt == 0) {
                        break;
                    }
                }
            }
            // We couldn't place all items
            if (remainder.getAmount() > 0) return null;
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

    private static class Remainder {
        private final ItemStack item;
        private int amount;

        public Remainder(ItemStack item, int amount) {
            this.item = item.clone();
            this.amount = amount;
        }

        public ItemStack getItem() {
            return item.clone();
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amt) {
            this.amount = amt;
        }
    }
}
