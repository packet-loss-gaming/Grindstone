/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.legacymigration.migrations;

import com.google.common.collect.Sets;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.legacymigration.Migration;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class GodSwordMigration implements Migration {
    @Override
    public Set<String> getValidOptions() {
        return Sets.newHashSet("heavy", "light");
    }

    @Override
    public ItemStack apply(ItemStack stack, String s) {
        if (s.equals("heavy")) {
            ItemStack sword = CustomItemCenter.build(CustomItems.GOD_SWORD);
            sword.setDurability(stack.getDurability());
            ItemUtil.copyEnchancements(stack, sword);
            return sword;
        } else {
            ItemStack sword = CustomItemCenter.build(CustomItems.GOD_SHORT_SWORD);
            sword.setDurability(stack.getDurability());
            ItemUtil.copyEnchancements(stack, sword);
            return sword;
        }
    }

    @Override
    public boolean test(ItemStack stack) {
        boolean isGodSword = ItemUtil.matchesFilter(stack, CustomItems.GOD_SWORD.getColoredName());
        if (!isGodSword) {
            return false;
        }

        return ItemUtil.getDamageModifier(stack) == 1.5;
    }
}
