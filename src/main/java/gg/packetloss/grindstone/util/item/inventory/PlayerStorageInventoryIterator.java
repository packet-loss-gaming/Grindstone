/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

import org.bukkit.inventory.PlayerInventory;

public class PlayerStorageInventoryIterator extends AbstractInventoryIterator {
    public PlayerStorageInventoryIterator(PlayerInventory playerInventory) {
        super(
            InventoryConstants.PLAYER_INV_ROW_LENGTH - 1,
            InventoryConstants.PLAYER_INV_STORAGE_LENGTH,
            playerInventory
        );
    }
}
