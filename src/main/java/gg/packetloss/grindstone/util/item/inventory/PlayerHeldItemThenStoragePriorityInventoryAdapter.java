/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerHeldItemThenStoragePriorityInventoryAdapter implements InventoryAdapter {
    private static final int[][] TRANSLATION_TABLES = new int[InventoryConstants.PLAYER_INV_ROW_LENGTH + 1][InventoryConstants.PLAYER_INV_LENGTH - InventoryConstants.PLAYER_INV_ARMOUR_LENGTH];

    private static void computeMainhandTable(int[] translationTable, int hotbarSpotUsed) {
        int k = 0;
        // First we're going for the hand slot
        translationTable[k++] = hotbarSpotUsed;

        // Then we're going for the storage area
        for (int i = 0; i < InventoryConstants.PLAYER_INV_STORAGE_LENGTH; ++i, ++k) {
            translationTable[k] = InventoryConstants.PLAYER_INV_ROW_LENGTH + i;
        }
        // Then we're going for the hotbar
        for (int i = 0; i < InventoryConstants.PLAYER_INV_ROW_LENGTH; ++i, ++k) {
            // This spot was already used, skip it
            if (i == hotbarSpotUsed) {
                --k;
                continue;
            }

            translationTable[k] = i;
        }
        // Then we're going for the offhand
        translationTable[k] = InventoryConstants.PLAYER_INV_OFFHAND_ITEM_INDEX;

        TranslationTableAdapter.runExpensiveTableValidation(translationTable);
    }

    private static void computeOffhandTable(int[] translationTable) {
        int k = 0;
        // First we're going for the hand slot
        translationTable[k++] = InventoryConstants.PLAYER_INV_OFFHAND_ITEM_INDEX;

        // Then we're going for the storage area
        for (int i = 0; i < InventoryConstants.PLAYER_INV_STORAGE_LENGTH; ++i, ++k) {
            translationTable[k] = InventoryConstants.PLAYER_INV_ROW_LENGTH + i;
        }
        // Then we're going for the hotbar
        for (int i = 0; i < InventoryConstants.PLAYER_INV_ROW_LENGTH; ++i, ++k) {
            translationTable[k] = i;
        }

        TranslationTableAdapter.runExpensiveTableValidation(translationTable);
    }

    static {
        for (int i = 0; i < InventoryConstants.PLAYER_INV_ROW_LENGTH; ++i) {
            computeMainhandTable(TRANSLATION_TABLES[i], i);
        }
        computeOffhandTable(TRANSLATION_TABLES[InventoryConstants.PLAYER_INV_ROW_LENGTH]);
    }

    private final TranslationTableAdapter underlyingAdapter;

    public PlayerHeldItemThenStoragePriorityInventoryAdapter(Player player, EquipmentSlot handSlot) {
        this.underlyingAdapter = new TranslationTableAdapter(getTranslationTable(player, handSlot), player);
    }

    private int[] getTranslationTable(Player player, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HAND) {
            return TRANSLATION_TABLES[player.getInventory().getHeldItemSlot()];
        } else if (slot == EquipmentSlot.OFF_HAND) {
            return TRANSLATION_TABLES[TRANSLATION_TABLES.length - 1];
        } else {
            throw new IllegalArgumentException();
        }
    }

    private PlayerHeldItemThenStoragePriorityInventoryAdapter(PlayerHeldItemThenStoragePriorityInventoryAdapter adapter) {
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
        return new PlayerHeldItemThenStoragePriorityInventoryAdapter(this);
    }

    @Override
    public void applyChanges() {
        underlyingAdapter.applyChanges();
    }
}
