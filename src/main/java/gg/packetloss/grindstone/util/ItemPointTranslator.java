package gg.packetloss.grindstone.util;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemPointTranslator {
    private List<PointMapping> pointMappings = new ArrayList<>();

    public void addMapping(ItemStack item, int value) {
        pointMappings.add(new PointMapping(item, value));
        compile();
    }

    public int calculateValue(ItemStack[] itemStacks, boolean removeWhileCounting) {
        int value = 0;

        for (int i = 0; i < itemStacks.length; ++i) {
            ItemStack curStack = itemStacks[i];
            for (PointMapping pointMapping : pointMappings) {
                if (pointMapping.getItem().isSimilar(curStack)) {
                    value += curStack.getAmount() * pointMapping.getValue();
                    if (removeWhileCounting) {
                        itemStacks[i] = null;
                    }
                    break;
                }
            }
        }

        return value;
    }

    public DepositReport assignValue(ItemStack[] itemStacks, int value) {
        itemStacks = ItemUtil.clone(itemStacks); // Make sure we're working in our domain here
        for (PointMapping pointMapping : pointMappings) {
            ItemStack targetStack = pointMapping.getItem();
            for (int i = 0; i < itemStacks.length; ++i) {
                // Use an insertion position which prefers to declutter the hotbar.
                int insertionPos = (i + 9) % itemStacks.length;

                final ItemStack stack = itemStacks[insertionPos];
                int startingAmt = stack == null ? 0 : stack.getAmount();
                int targetValue = pointMapping.getValue();

                if (value >= targetValue && (startingAmt == 0 || targetStack.isSimilar(stack))) {
                    int quantity = Math.min(value / targetValue, targetStack.getMaxStackSize());
                    value -= quantity * targetValue;
                    itemStacks[insertionPos] = targetStack.clone();
                    itemStacks[insertionPos].setAmount(quantity);

                    // Stop early if we no longer have anything to add
                    if (value == 0) {
                        break;
                    }
                }
            }
        }
        return new DepositReport(itemStacks, value);
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

    public static class DepositReport {
        private ItemStack[] newStackState;
        private int remainingValue;

        private DepositReport(ItemStack[] newStackState, int remainingValue) {
            this.newStackState = newStackState;
            this.remainingValue = remainingValue;
        }

        public ItemStack[] getNewStackState() {
            return newStackState;
        }

        public int getRemainingValue() {
            return remainingValue;
        }

        public boolean completelyDeposited() {
            return remainingValue == 0;
        }

        public boolean failedToDeposit() {
            return !completelyDeposited();
        }
    }
}
