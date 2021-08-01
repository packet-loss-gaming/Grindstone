/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

public class CustomEquipment extends CustomItem {
    public CustomEquipment(CustomItems item, Material type) {
        super(item, type);
    }

    public CustomEquipment(CustomEquipment item) {
        super(item);
    }

    @Override
    public void accept(CustomItemVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected ItemStack build(CustomItems identity) {
        ItemStack stack = super.build(identity);
        ItemMeta meta = stack.getItemMeta();

        // If has pre-populated enchantments, force them
        if (meta instanceof Repairable && !getEnchants().isEmpty()) {
            ((Repairable) meta).setRepairCost(400);
        }

        stack.setItemMeta(meta);
        return stack;
    }
}
