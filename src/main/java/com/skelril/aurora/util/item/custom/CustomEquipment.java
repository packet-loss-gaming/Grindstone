/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item.custom;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

public class CustomEquipment extends CustomItem {
    public CustomEquipment(CustomItems item, ItemStack base) {
        super(item, base);
    }

    public CustomEquipment(CustomItems item, Material type) {
        this(item, new ItemStack(type));
    }

    @Override
    public ItemStack build() {
        ItemStack stack = super.build();
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof Repairable) {
            ((Repairable) meta).setRepairCost(400);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
