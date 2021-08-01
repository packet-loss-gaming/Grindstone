/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.inventory.ItemStack;

public class CustomGiftItem extends CustomItem {
    private final CustomItem baseItem;

    public CustomGiftItem(CustomItems item, CustomItem baseItem) {
        super(item, baseItem.getBaseType());
        this.baseItem = baseItem;
    }

    @Override
    protected ItemStack build(CustomItems identity) {
        return baseItem.build(identity);
    }
}
