/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration.migrations;

import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.migration.Migration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomItemModelMigration implements Migration {
    @Override
    public boolean test(ItemStack itemStack) {
        return !itemStack.getItemMeta().hasCustomModelData();
    }

    @Override
    public ItemStack apply(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        CustomItemCenter.getModelId(itemStack).ifPresent((id) -> {
            meta.setCustomModelData(id);
            itemStack.setItemMeta(meta);
        });
        return itemStack;
    }
}
