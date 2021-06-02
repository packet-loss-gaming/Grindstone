/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.repair.profile;

import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.util.item.ItemUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class SacrificeRepairProfile implements RepairProfile {
    private final ItemFamily itemFamily;
    private final float repairPercentage;

    public SacrificeRepairProfile(ItemFamily itemFamily, float repairPercentage) {
        this.itemFamily = itemFamily;
        this.repairPercentage = repairPercentage;
    }

    public float getRepairPercentage() {
        return repairPercentage;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        return ItemUtil.isInItemFamily(itemStack, itemFamily);
    }

    @Override
    public BaseComponent[] getWarningMessage() {
        return Text.of(ChatColor.YELLOW, "Sacrifice this item.").build();
    }
}
