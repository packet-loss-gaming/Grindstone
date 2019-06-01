package gg.packetloss.grindstone.util.item;

import gg.packetloss.grindstone.items.custom.CustomItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ItemNameCalculator {
    private static Set<String> names = new HashSet<>();

    static {
        for (CustomItems item : CustomItems.values()) {
            names.add(item.name());
        }
    }

    public static boolean hasItemOfName(String name) {
        return names.contains(name.toUpperCase().replace(' ', '_'));
    }

    public static Optional<String> matchItemFromNameOrId(String itemName) {
        // If this is a custom item, return that.
        if (hasItemOfName(itemName)) {
            return Optional.of(itemName);
        }

        // Otherwise try to find a builtin item.
        ItemType type = ItemType.lookup(itemName);
        if (type == null) {
            // Return no result, we found no custom item, and no builtin item.
            return Optional.empty();
        }
        return Optional.of(type.getName());
    }

    private static boolean verifyValidCustomItem(ItemMeta stackMeta) {
        String itemName = stackMeta.getDisplayName();
        return ItemUtil.isAuthenticCustomItem(itemName);
    }

    public static boolean verifyValidCustomItem(ItemStack stack) {
        ItemMeta stackMeta = stack.getItemMeta();
        if (!stackMeta.hasDisplayName()) {
            return true;
        }

        return verifyValidCustomItem(stackMeta);
    }

    public static Optional<String> computeItemName(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }

        String itemName = stack.getTypeId() + ":" + stack.getDurability();

        // Check for custom item name overrides.
        ItemMeta stackMeta = stack.getItemMeta();
        if (stackMeta.hasDisplayName()) {
            if (verifyValidCustomItem(stackMeta)) {
                itemName = ChatColor.stripColor(stackMeta.getDisplayName());
            } else {
                // Return no result, we found an invalid custom item.
                return Optional.empty();
            }
        }

        return matchItemFromNameOrId(itemName);
    }

}
