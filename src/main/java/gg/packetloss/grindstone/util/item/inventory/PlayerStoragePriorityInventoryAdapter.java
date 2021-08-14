/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class PlayerStoragePriorityInventoryAdapter implements InventoryAdapter {
    private final Player player;
    private final PlayerInventory playerInventory;

    private final int length;
    private final ItemStack[] itemStacks;
    private final boolean[] updateMask;

    public PlayerStoragePriorityInventoryAdapter(Player player) {
        this.player = player;

        this.playerInventory = player.getInventory();

        this.length = playerInventory.getSize();
        this.itemStacks = playerInventory.getContents();
        this.updateMask = new boolean[length];

        Validate.isTrue(length == InventoryConstants.PLAYER_INV_LENGTH);
        Validate.isTrue(itemStacks.length == InventoryConstants.PLAYER_INV_LENGTH);
    }

    private PlayerStoragePriorityInventoryAdapter(PlayerStoragePriorityInventoryAdapter adapter) {
        this.player = adapter.player;

        this.playerInventory = adapter.playerInventory;
        this.length = adapter.length;
        this.itemStacks = ItemUtil.clone(adapter.itemStacks);
        this.updateMask = adapter.updateMask.clone();
    }

    @Override
    public int size() {
        return InventoryConstants.PLAYER_INV_LENGTH - InventoryConstants.PLAYER_INV_ARMOUR_LENGTH;
    }

    private int inflateExternalIndex(int index) {
        if (index >= InventoryConstants.PLAYER_INV_ROWS_TOTAL_LENGTH) {
            return index + InventoryConstants.PLAYER_INV_ARMOUR_LENGTH;
        }
        return index;
    }

    private int prioritizeInternalIndex(int index) {
        if (index > InventoryConstants.PLAYER_INV_ROWS_TOTAL_LENGTH) {
            return index;
        }
        return (index + InventoryConstants.PLAYER_INV_ROW_LENGTH) % InventoryConstants.PLAYER_INV_ROWS_TOTAL_LENGTH;
    }

    private int translateExternalIndexToInternal(int index) {
        return prioritizeInternalIndex(inflateExternalIndex(index));
    }

    @Override
    public ItemStack getAt(int index) {
        return itemStacks[translateExternalIndexToInternal(index)];
    }

    @Override
    public void setAt(int index, ItemStack stack) {
        itemStacks[translateExternalIndexToInternal(index)] = stack;
        updateMask[translateExternalIndexToInternal(index)] = true;
    }

    @Override
    public InventoryAdapter copy() {
        return new PlayerStoragePriorityInventoryAdapter(this);
    }

    @Override
    public void applyChanges() {
        for (int i = 0; i < length; ++i) {
            if (updateMask[i]) {
                playerInventory.setItem(i, itemStacks[i]);
            }
        }
    }
}
