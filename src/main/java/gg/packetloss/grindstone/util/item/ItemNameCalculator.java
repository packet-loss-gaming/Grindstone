/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.macro.ExpansionReplacementMacro;
import gg.packetloss.grindstone.util.macro.MacroExpander;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemNameCalculator {
    private static Set<String> names = new HashSet<>();

    static {
        for (CustomItems item : CustomItems.values()) {
            names.add(item.getNamespaceName());
        }
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

    public static Optional<String> matchItem(String itemName) {
        itemName = itemName.toLowerCase();
        itemName = itemName.replaceAll(" ", "_");

        int namespaceSeparatorIndex = itemName.indexOf(':');
        if (namespaceSeparatorIndex != -1) {
            return Optional.of(itemName);
        }

        String maybeItemName = CustomItems.computeNamespaceName(itemName);
        if (hasItemOfName(maybeItemName)) {
            return Optional.of(maybeItemName);
        }

        if (Material.matchMaterial(itemName) != null) {
            return Optional.of("minecraft:" + itemName);
        }

        return Optional.empty();
    }

    private static boolean hasItemOfName(String computedName) {
        return names.contains(computedName);
    }

    public static String getDisplayName(ItemStack stack) {
        ItemMeta stackMeta = stack.getItemMeta();
        if (stackMeta.hasDisplayName()) {
            return stackMeta.getDisplayName();
        }
        return stack.getI18NDisplayName();
    }

    public static Optional<String> computeItemName(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }

        // Check for custom item name overrides.
        ItemMeta stackMeta = stack.getItemMeta();
        if (stackMeta.hasDisplayName()) {
            if (verifyValidCustomItem(stackMeta)) {
                String itemName = CustomItems.computeNamespaceName(ChatColor.stripColor(stackMeta.getDisplayName()));
                if (hasItemOfName(itemName)) {
                    return Optional.of(itemName);
                }

                // This matched a custom item, but not any registered item? Be cautious and return nothing.
                return Optional.empty();
            }
        }

        return Optional.of(stack.getType().getKey().toString());
    }

    public static String getUnqualifiedName(String qualifiedName) {
        Validate.isTrue(qualifiedName.contains(":"));
        return qualifiedName.split(":")[1];
    }

    public static Optional<String> computeBlockName(Block block) {
        return Optional.of(block.getType().getKey().toString());
    }

    public static Optional<String> computeBlockName(BlockState block) {
        return Optional.of(block.getType().getKey().toString());
    }

    public static Set<String> computeItemNames(Collection<ItemStack> stacks) {
        Set<String> names = new HashSet<>();

        stacks.forEach((stack) -> {
            computeItemName(stack).ifPresent(names::add);
        });

        return names;
    }

    private static ExpansionReplacementMacro ARMOR_MACRO = new ExpansionReplacementMacro("#armo(|u)r", List.of(
            "_helmet",
            "_chestplate",
            "_leggings",
            "_boots"
    ));

    private static MacroExpander ITEM_NAME_MACRO_EXPANDER = new MacroExpander(List.of(
            ARMOR_MACRO
    ));

    public static Set<String> expandNameMacros(String baseName) {
        return ITEM_NAME_MACRO_EXPANDER.expand(baseName);
    }
}
