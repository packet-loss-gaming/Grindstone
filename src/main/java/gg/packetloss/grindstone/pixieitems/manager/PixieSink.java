/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.manager;

import gg.packetloss.grindstone.util.item.inventory.DirectInventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.TranslationTableInventoryAdapter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import java.util.List;

class PixieSink {
    private final Location loc;
    private final List<Integer> availableSlots;

    public PixieSink(Location loc, List<Integer> availableSlots) {
        this.loc = loc;
        this.availableSlots = availableSlots;
    }

    public Location getLocation() {
        return loc;
    }

    public Block getBlock() {
        return loc.getBlock();
    }

    public InventoryAdapter adaptInventory(Inventory inventory) {
        if (availableSlots == null) {
            return new DirectInventoryAdapter(inventory);
        }

        int[] translationTable = availableSlots.stream().filter(
            (i) -> i < inventory.getSize()
        ).mapToInt(Integer::intValue).toArray();
        return new TranslationTableInventoryAdapter(translationTable, inventory);
    }
}
