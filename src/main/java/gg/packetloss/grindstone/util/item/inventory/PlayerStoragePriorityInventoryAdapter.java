/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerStoragePriorityInventoryAdapter implements InventoryAdapter {
    private static final int[] TRANSLATION_TABLE = new int[InventoryConstants.PLAYER_INV_LENGTH - InventoryConstants.PLAYER_INV_ARMOUR_LENGTH];

    static {
        int k = 0;
        // First we're going for the storage area
        for (int i = 0; i < InventoryConstants.PLAYER_INV_STORAGE_LENGTH; ++i, ++k) {
            TRANSLATION_TABLE[k] = InventoryConstants.PLAYER_INV_ROW_LENGTH + i;
        }
        // Then we're going for the hotbar
        for (int i = 0; i < InventoryConstants.PLAYER_INV_ROW_LENGTH; ++i, ++k) {
            TRANSLATION_TABLE[k] = i;
        }
        // Then we're going for the offhand
        TRANSLATION_TABLE[k] = InventoryConstants.PLAYER_INV_OFFHAND_ITEM_INDEX;

        TranslationTableAdapter.runExpensiveTableValidation(TRANSLATION_TABLE);
    }

    private final TranslationTableAdapter underlyingAdapter;

    public PlayerStoragePriorityInventoryAdapter(Player player) {
        this.underlyingAdapter = new TranslationTableAdapter(TRANSLATION_TABLE, player);
    }

    private PlayerStoragePriorityInventoryAdapter(PlayerStoragePriorityInventoryAdapter adapter) {
        this.underlyingAdapter = adapter.underlyingAdapter.copy();
    }

    @Override
    public int size() {
        return underlyingAdapter.size();
    }

    @Override
    public ItemStack getAt(int index) {
        return underlyingAdapter.getAt(index);
    }

    @Override
    public void setAt(int index, ItemStack stack) {
        underlyingAdapter.setAt(index, stack);
    }

    @Override
    public InventoryAdapter copy() {
        return new PlayerStoragePriorityInventoryAdapter(this);
    }

    @Override
    public void applyChanges() {
        underlyingAdapter.applyChanges();
    }
}
