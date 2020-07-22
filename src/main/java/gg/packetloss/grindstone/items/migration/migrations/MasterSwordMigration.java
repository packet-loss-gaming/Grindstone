/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration.migrations;

import com.google.common.collect.Sets;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.migration.Migration;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class MasterSwordMigration implements Migration {
    @Override
    public Set<String> getValidOptions() {
        return Sets.newHashSet("heavy", "light");
    }

    @Override
    public ItemStack apply(ItemStack stack, String s) {
        if (s.equals("heavy")) {
            ItemStack sword = CustomItemCenter.build(CustomItems.MASTER_SWORD);
            sword.setDurability(stack.getDurability());
            ItemUtil.copyEnchancements(stack, sword);
            return sword;
        } else {
            ItemStack sword = CustomItemCenter.build(CustomItems.MASTER_SHORT_SWORD);
            sword.setDurability(stack.getDurability());
            ItemUtil.copyEnchancements(stack, sword);
            return sword;
        }
    }

    @Override
    public boolean test(ItemStack stack) {
        boolean isMasterSword = ItemUtil.matchesFilter(stack, CustomItems.MASTER_SWORD.getColoredName());
        if (!isMasterSword) {
            return false;
        }

        return ItemUtil.getDamageModifier(stack) == 2;
    }
}
