/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item.inventory;

public class InventoryConstants {
    public static final int PLAYER_INV_ROW_LENGTH = 9;
    public static final int PLAYER_INV_ROW_COUNT = 4;
    public static final int PLAYER_INV_ROWS_TOTAL_LENGTH = PLAYER_INV_ROW_LENGTH * PLAYER_INV_ROW_COUNT;
    public static final int PLAYER_INV_STORAGE_LENGTH = PLAYER_INV_ROWS_TOTAL_LENGTH - PLAYER_INV_ROW_LENGTH;
    public static final int PLAYER_INV_ARMOUR_LENGTH = 4;
    public static final int PLAYER_INV_EXTRA_LENGTH = 1;
    public static final int PLAYER_INV_LENGTH = PLAYER_INV_ROWS_TOTAL_LENGTH + PLAYER_INV_ARMOUR_LENGTH + PLAYER_INV_EXTRA_LENGTH;
    public static final int PLAYER_INV_OFFHAND_ITEM_INDEX = PLAYER_INV_LENGTH - 1;
}
