/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import org.bukkit.inventory.PlayerInventory;

public class PlayerInventoryIterator extends AbstractInventoryIterator {
    public PlayerInventoryIterator(PlayerInventory playerInventory) {
        super(
            -1,
            InventoryConstants.PLAYER_INV_LENGTH - InventoryConstants.PLAYER_INV_ARMOUR_LENGTH,
            playerInventory
        );
    }

    @Override
    protected void updateIndex() {
        // Update index
        super.updateIndex();

        // Check if we need to skip anything
        if (index == InventoryConstants.PLAYER_INV_ROWS_TOTAL_LENGTH) {
            // Skip the armor slots to get to the offhand slot
            for (int i = 0; i < InventoryConstants.PLAYER_INV_ARMOUR_LENGTH; ++i) {
                ++index;
            }
        }
    }
}
