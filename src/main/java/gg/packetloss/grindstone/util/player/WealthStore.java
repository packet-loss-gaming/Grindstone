/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.player;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WealthStore extends GenericWealthStore {

    public WealthStore(String ownerName, ItemStack[] inventoryContents) {

        super(ownerName, inventoryContents);
    }

    public WealthStore(String ownerName, ItemStack[] inventoryContents, ItemStack[] armourContents) {

        super(ownerName, inventoryContents, armourContents);
    }

    public WealthStore(String ownerName, List<ItemStack> itemStacks) {

        super(ownerName, itemStacks);
    }

    public WealthStore(String ownerName, List<ItemStack> itemStacks, int value) {

        super(ownerName, itemStacks, value);
    }

    public WealthStore(String ownerName, int value) {

        super(ownerName, value);
    }
}
