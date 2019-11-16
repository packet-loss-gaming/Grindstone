package gg.packetloss.grindstone.util.item.inventory;

import org.bukkit.inventory.ItemStack;

public interface InventoryAdapter {
    int size();

    ItemStack getAt(int index);
    void setAt(int index, ItemStack stack);

    InventoryAdapter copy();

    void applyChanges();
}
