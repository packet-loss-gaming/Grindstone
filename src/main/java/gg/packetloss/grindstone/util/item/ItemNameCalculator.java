package gg.packetloss.grindstone.util.item;

import de.themoep.idconverter.IdMappings;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.item.legacy.ItemType;
import gg.packetloss.grindstone.util.macro.ExpansionReplacementMacro;
import gg.packetloss.grindstone.util.macro.MacroExpander;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemNameCalculator {
    private static Set<String> names = new HashSet<>();
    private static Set<String> legacyNames = new HashSet<>();

    static {
        for (CustomItems item : CustomItems.values()) {
            names.add(item.getNamespaceName());
            legacyNames.add(item.name());
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

    public static Optional<String> matchItemFromNameOrId(String itemName) {
        // For now just use our legacy item name calculation and lookup
        Optional<String> name = legacyMatchItemFromNameOrId(itemName);
        if (name.isPresent()) {
            return translateLegacyComputedItemName(name.get());
        }

        return Optional.empty();
    }

    private static boolean hasItemOfName(String computedName) {
        return names.contains(computedName);
    }

    private static Map<String, String> overrides = new HashMap<>();

    static {
        overrides.put("17:0", "minecraft:oak_log");
        overrides.put("17:1", "minecraft:spruce_log");
        overrides.put("17:2", "minecraft:birch_log");
        overrides.put("17:3", "minecraft:jungle_log");
        overrides.put("162:0", "minecraft:acacia_log");
        overrides.put("162:1", "minecraft:dark_oak_log");
    }

    private static Optional<String> getWithoutMapping(String idString) {
        return Optional.ofNullable(overrides.get(idString));
    }

    private static Optional<String> mappingToMCID(IdMappings.Mapping mapping) {
        if (mapping == null) {
            return Optional.empty();
        }

        String mappingResult = mapping.get(IdMappings.IdType.FLATTENING);
        if (mappingResult == null) {
            return Optional.empty();
        }

        return Optional.of("minecraft:" + mappingResult.toLowerCase());
    }

    @Deprecated
    public static Optional<String> computeBlockName(int blockID, int data) {
        String idString = blockID + ":" + data;
        Optional<String> optLibOverride = getWithoutMapping(idString);
        if (optLibOverride.isPresent()) {
            return optLibOverride;
        }

        IdMappings.Mapping mappedName = IdMappings.get(IdMappings.IdType.NUMERIC, idString);
        if (mappedName == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(mappedName.getFlatteningType());
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

        // FIXME: Once we move to these being the real native types, this becomes a much simpler and faster
        // routine.
        String idString = stack.getTypeId() + ":" + stack.getDurability();
        Optional<String> optLibOverride = getWithoutMapping(idString);
        if (optLibOverride.isPresent()) {
            return optLibOverride;
        }

        IdMappings.Mapping mappedName = IdMappings.get(IdMappings.IdType.NUMERIC, idString);
        if (mappedName == null) {
            // Try again without the durability
            mappedName = IdMappings.get(IdMappings.IdType.NUMERIC, String.valueOf(stack.getTypeId()));
            if (mappedName == null) {
                // Return no result, we couldn't map this from its legacy id to a mapping for translation
                return Optional.empty();
            }
        }

        return mappingToMCID(mappedName);
    }

    public static Set<String> computeItemNames(Collection<ItemStack> stacks) {
        Set<String> names = new HashSet<>();

        stacks.forEach((stack) -> {
            computeItemName(stack).ifPresent(names::add);
        });

        return names;
    }

    private static boolean legacyHasItemOfName(String name) {
        return legacyNames.contains(name.toUpperCase().replaceAll("'S", "").replaceAll(" ", "_"));
    }

    private static Optional<String> legacyMatchItemFromNameOrId(String itemName) {
        // If this is a custom item, return that.
        if (legacyHasItemOfName(itemName)) {
            return Optional.of(itemName.toLowerCase());
        }

        // Otherwise try to find a builtin item.
        ItemType type = ItemType.lookup(itemName);
        if (type == null) {
            // Return no result, we found no custom item, and no builtin item.
            return Optional.empty();
        }
        return Optional.of(type.getName());
    }

    @Deprecated
    public static Optional<String> translateLegacyComputedItemName(String name) {
        // if this name is a prefix based name, assume it's updated
        if (name.contains(":")) {
            return Optional.of(name);
        }

        // If this is a custom item, return that.
        if (legacyHasItemOfName(name)) {
            return Optional.of(CustomItems.computeNamespaceName(name));
        }

        // Otherwise try to find a builtin item.
        ItemType type = ItemType.lookup(name);
        if (type == null) {
            // Return no result, we found no custom item, and no builtin item.
            return Optional.empty();
        }

        String idString = type.getID() + ":" + type.getData();
        Optional<String> optLibOverride = getWithoutMapping(idString);
        if (optLibOverride.isPresent()) {
            return optLibOverride;
        }

        IdMappings.Mapping mappedName = IdMappings.get(IdMappings.IdType.NUMERIC, idString);
        if (mappedName == null) {
            // Return no result, we couldn't map this from its legacy id to a mapping for translation
            return Optional.empty();
        }

        return mappingToMCID(mappedName);
    }

    public static class NumericItem {
        private final int id;
        private final short data;

        public NumericItem(int id, short data) {
            this.id = id;
            this.data = data;
        }

        public int getId() {
            return id;
        }

        public short getData() {
            return data;
        }
    }

    @Deprecated
    public static Optional<NumericItem> toNumeric(String flattenedName) {
        // Check for overrides, this is really dirty
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            if (entry.getValue().equals(flattenedName)) {
                String[] parts = entry.getKey().split(":");
                return Optional.of(new NumericItem(Integer.parseInt(parts[0]), Short.parseShort(parts[1])));
            }
        }

        // Remove prefix and convert back
        flattenedName = flattenedName.replaceFirst("minecraft:", "");
        IdMappings.Mapping mapping = IdMappings.get(IdMappings.IdType.FLATTENING, flattenedName);
        if (mapping == null) {
            return Optional.empty();
        }

        return Optional.of(new NumericItem(mapping.getNumericId(), (short) mapping.getData()));
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

    public static List<String> expandNameMacros(String baseName) {
        return ITEM_NAME_MACRO_EXPANDER.expand(baseName);
    }
}
