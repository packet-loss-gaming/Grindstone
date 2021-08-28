/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class TranslationTableInventoryAdapter implements InventoryAdapter {
    private final int[] translationTable;

    private final Inventory inventory;

    private final ItemStack[] itemStacks;
    private final boolean[] updateMask;

    protected static void runExpensiveTableValidation(int[] translationTable) {
        Set<Integer> indexSet = new HashSet<>();
        for (int index : translationTable) {
            indexSet.add(index);
        }

        Validate.isTrue(indexSet.size() == translationTable.length);
    }

    public TranslationTableInventoryAdapter(int[] translationTable, Inventory inventory) {
        this.translationTable = translationTable;

        this.inventory = inventory;

        this.itemStacks = inventory.getContents();
        this.updateMask = new boolean[translationTable.length];
    }

    protected TranslationTableInventoryAdapter(int[] translationTable, Player player) {
        this(translationTable, player.getInventory());

        Validate.isTrue(itemStacks.length == InventoryConstants.PLAYER_INV_LENGTH);
    }

    private TranslationTableInventoryAdapter(TranslationTableInventoryAdapter adapter) {
        this.translationTable = adapter.translationTable;

        this.inventory = adapter.inventory;

        this.itemStacks = ItemUtil.clone(adapter.itemStacks);
        this.updateMask = adapter.updateMask.clone();
    }

    @Override
    public int size() {
        return translationTable.length;
    }

    private int translateToRealSlot(int index) {
        return translationTable[index];
    }

    @Override
    public ItemStack getAt(int index) {
        return itemStacks[translateToRealSlot(index)];
    }

    @Override
    public void setAt(int index, ItemStack stack) {
        itemStacks[translateToRealSlot(index)] = stack;
        updateMask[index] = true;
    }

    @Override
    public TranslationTableInventoryAdapter copy() {
        return new TranslationTableInventoryAdapter(this);
    }

    @Override
    public boolean applyChanges() {
        boolean anyChanged = false;
        for (int i = 0; i < size(); ++i) {
            if (updateMask[i]) {
                anyChanged = true;

                int realIndex = translateToRealSlot(i);
                inventory.setItem(realIndex, itemStacks[realIndex]);
            }
        }
        return anyChanged;
    }
}
