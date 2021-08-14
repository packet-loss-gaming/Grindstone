/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration.migrations;

import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.migration.Migration;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ToPeacefulWarriorArmor implements Migration {
    private static final String OLD_PREFIX = ChatColor.DARK_GREEN + "Apocalyptic Camouflage ";

    private static final Map<String, CustomItems> OLD_ITEM_TO_NEW = Map.of(
        OLD_PREFIX + "Helmet", CustomItems.PEACEFUL_WARRIOR_HELMET,
        OLD_PREFIX + "Chestplate", CustomItems.PEACEFUL_WARRIOR_CHESTPLATE,
        OLD_PREFIX + "Leggings", CustomItems.PEACEFUL_WARRIOR_LEGGINGS,
        OLD_PREFIX + "Boots", CustomItems.PEACEFUL_WARRIOR_BOOTS
    );

    private static final List<String> OLD_ITEMS = List.copyOf(OLD_ITEM_TO_NEW.keySet());

    @Override
    public boolean test(ItemStack itemStack) {
        for (String oldItemName : OLD_ITEMS) {
            if (ItemUtil.matchesFilter(itemStack, oldItemName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack apply(ItemStack itemStack) {
        for (String oldItemName : OLD_ITEMS) {
            if (ItemUtil.matchesFilter(itemStack, oldItemName)) {
                return CustomItemCenter.build(OLD_ITEM_TO_NEW.get(oldItemName));
            }
        }
        throw new IllegalStateException();
    }
}


