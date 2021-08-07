/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import gg.packetloss.bukkittext.Text;
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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;

import java.util.*;

import static gg.packetloss.grindstone.util.StringUtil.toUppercaseTitle;

public class ItemNameCalculator {
    protected static final String SERIALIZATION_SPLIT = "!!!";
    protected static final String POTION_SPLIT = SERIALIZATION_SPLIT + "of_";
    protected static final String EXTENDED_POSTFIX = "_extended";
    protected static final String UPGRADED_POSTFIX = "_upgraded";

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

    /**
     * Make sure to update ItemNameDeserializer#restoreItemMetadata(String, ItemMeta) when changing
     * this method.
     */
    private static Optional<String> createMetadataString(ItemMeta stackMeta) {
        if (stackMeta instanceof PotionMeta) {
            PotionData potionData = ((PotionMeta) stackMeta).getBasePotionData();
            StringBuilder builder = new StringBuilder();
            builder.append(POTION_SPLIT);

            builder.append(potionData.getType().name().toLowerCase());
            if (potionData.isExtended()) {
                builder.append(EXTENDED_POSTFIX);
            } else if (potionData.isUpgraded()) {
                builder.append(UPGRADED_POSTFIX);
            }
            return Optional.of(builder.toString());
        }

        return Optional.empty();
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

        String name = stack.getType().getKey().toString();
        Optional<String> optExtra = createMetadataString(stackMeta);
        if (optExtra.isPresent()) {
            name += optExtra.get();
        }
        return Optional.of(name);
    }

    public static String getUnqualifiedName(String qualifiedName) {
        Validate.isTrue(qualifiedName.contains(":"));
        return qualifiedName.split(":")[1].replace(SERIALIZATION_SPLIT, "_");
    }
    public static String getSystemDisplayNameNoColor(String itemName) {
        return toUppercaseTitle(getUnqualifiedName(itemName));
    }

    public static Optional<String> computeBlockName(Block block) {
        return Optional.of(block.getType().getKey().toString());
    }

    public static Text getSystemDisplayName(String itemName) {
        return Text.of(ChatColor.BLUE, getSystemDisplayNameNoColor(itemName));
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
