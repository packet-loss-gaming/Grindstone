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
   * @param itemStacks - the old item stacks
   * @return the new item stacks, or null if the operation could not be completed/did nothing
   */
  public ItemStack[] operate(ItemStack[] itemStacks, boolean enableSpaceSaving) {
    itemStacks = ItemUtil.clone(itemStacks); // Make sure we're working in our domain here
    List<Remainder> remainders = new ArrayList<>();
    int modified = 0;
    for (Step step : conversionSteps) {
      int sourceTotal = 0;
      List<Integer> destStackPos = new ArrayList<>();

      ItemStack source = step.getOldItem();
      ItemStack dest = step.getNewItem();

      for (int i = 0; i < itemStacks.length; ++i) {
        ItemStack cur = itemStacks[i];
        if (source.isSimilar(cur)) {
          sourceTotal += cur.getAmount();
          itemStacks[i] = null;
        } else if (dest.isSimilar(cur) && cur.getAmount() < cur.getMaxStackSize()) {
          destStackPos.add(i);
        }
      }

      // Add in any remainders from prior steps
      Iterator<Remainder> it = remainders.iterator();
      while (it.hasNext()) {
        Remainder remainder = it.next();
        if (step.getOldItem().isSimilar(remainder.getItem())) {
          sourceTotal += remainder.getAmount();
          it.remove();
        }
      }

      int divisor = step.getOldItem().getAmount();

      int newAmt = (sourceTotal / divisor) * step.getNewItem().getAmount();

      // If we're adding any items to the players inventory, include any stacks
      // of the destination type that already exist.
      //
      // Similarly, if we've got more than 1 stack of the destination type, even
      // if we're not adding any new items because of compaction, compress by
      // reducing the number of used slots in the inventory.
      //
      // If this is a limited use item, only activate this in the first case
      // -- where we are already making changes -- this prevents the item from being
      // used accidentally in situations where it would not be beneficial for instance
      // two stacks of 36 / 64, while we can make one 64, and one 28 that would be an
      // extremely unfortunate activation and certainly annoying.
      if (newAmt > 0 || (destStackPos.size() > 1 && enableSpaceSaving)) {
        for (int position : destStackPos) {
          newAmt += itemStacks[position].getAmount();
          itemStacks[position] = null;
        }
      }

      if (newAmt > 0) {
        ItemStack newStack = step.getNewItem();
        remainders.add(new Remainder(newStack, newAmt));
      }

      int oldAmt = sourceTotal % divisor;
      if (oldAmt > 0) {
        ItemStack oldStack = step.getOldItem();
        remainders.add(new Remainder(oldStack, oldAmt));
      }

      modified += newAmt;
    }

    for (Remainder remainder : remainders) {
      ItemStack rStack = remainder.getItem();
      for (int i = 0; i < itemStacks.length; ++i) {
        // Use an insertion position which prefers to declutter the hotbar.
        int insertionPos = (i + 9) % itemStacks.length;

        final ItemStack stack = itemStacks[insertionPos];
        int startingAmt = stack == null ? 0 : stack.getAmount();
        int rAmt = remainder.getAmount();

        if (rAmt > 0 && (startingAmt == 0 || rStack.isSimilar(stack))) {
          int quantity = Math.min(rAmt + startingAmt, rStack.getMaxStackSize());
          rAmt -= quantity - startingAmt;
          itemStacks[insertionPos] = rStack.clone();
          itemStacks[insertionPos].setAmount(quantity);
          remainder.setAmount(rAmt);

          // Stop early if we no longer have anything to add
          if (rAmt == 0) {
            break;
          }
        }
      }
      // We couldn't place all items
      if (remainder.getAmount() > 0) {
        return null;
      }
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
