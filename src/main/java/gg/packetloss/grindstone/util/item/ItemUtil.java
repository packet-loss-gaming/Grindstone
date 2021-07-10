/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class ItemUtil {

    private static final Set<Material> LEATHER_ARMOR_TYPES = Set.of(
            Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS,
            Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET
    );

    public static boolean isLeatherArmorPiece(Material type) {
        return LEATHER_ARMOR_TYPES.contains(type);
    }

    public static final ItemStack[] LEATHER_ARMOR  = new ItemStack[]{
            new ItemStack(Material.LEATHER_BOOTS), new ItemStack(Material.LEATHER_LEGGINGS),
            new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET)
    };
    public static final ItemStack[] IRON_ARMOR = new ItemStack[]{
            new ItemStack(Material.IRON_BOOTS), new ItemStack(Material.IRON_LEGGINGS),
            new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_HELMET)
    };
    public static final ItemStack[] GOLD_ARMOR = new ItemStack[]{
            new ItemStack(Material.GOLDEN_BOOTS), new ItemStack(Material.GOLDEN_LEGGINGS),
            new ItemStack(Material.GOLDEN_CHESTPLATE), new ItemStack(Material.GOLDEN_HELMET)
    };
    public static final ItemStack[] DIAMOND_ARMOR = new ItemStack[]{
            new ItemStack(Material.DIAMOND_BOOTS), new ItemStack(Material.DIAMOND_LEGGINGS),
            new ItemStack(Material.DIAMOND_CHESTPLATE), new ItemStack(Material.DIAMOND_HELMET)
    };
    public static final ItemStack[] NO_ARMOR = new ItemStack[] {
            null, null, null, null
    };

    public static ItemStack makeSkull(PlayerProfile playerProfile) {
        // Require textures be provided, otherwise we have major performance issues
        Validate.isTrue(playerProfile.hasTextures());

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setPlayerProfile(playerProfile);
        skull.setItemMeta(skullMeta);
        return skull;
    }

    public static ItemStack[] clone(ItemStack[] stacks) {

        ItemStack[] returnStack = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : stacks[i].clone();
        }
        return returnStack;
    }
    
    public static int countFilledSlots(Player player) {

        PlayerInventory inventory = player.getInventory();
        return countFilledSlots(inventory.getContents()) + countFilledSlots(inventory.getArmorContents());
    }

    public static int countFilledSlots(ItemStack[] inventory) {

        int count = 0;
        for (ItemStack aItemStack : inventory) {
            if (aItemStack != null && aItemStack.getType() != Material.AIR) count++;
        }
        return count;
    }

    public static boolean findItemOfName(ItemStack[] itemStacks, String name) {

        for (ItemStack itemStack : itemStacks) {
            if (matchesFilter(itemStack, name)) return true;
        }
        return false;
    }

    public static boolean removeItemOfType(InventoryHolder inventoryHolder, Material type, int quantity, boolean ignoreToSmall) {

        int c = ItemUtil.countItemsOfType(inventoryHolder.getInventory().getContents(), type);

        if (c == 0 || (c < quantity && !ignoreToSmall)) return false;

        c -= Math.min(c, quantity);

        ItemStack[] stacks = ItemUtil.removeItemOfType(inventoryHolder.getInventory().getContents(), type);

        inventoryHolder.getInventory().setContents(stacks);

        int amount = Math.min(c, type.getMaxStackSize());
        while (amount > 0) {
            inventoryHolder.getInventory().addItem(new ItemStack(type, amount));
            c -= amount;
            amount = Math.min(c, 64);
        }

        if (inventoryHolder instanceof Player) {
            //noinspection deprecation
            ((Player) inventoryHolder).updateInventory();
        }
        return true;
    }

    public static ItemStack[] removeItemOfType(ItemStack[] itemStacks, Material type) {

        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack is = itemStacks[i];
            if (is != null && !isNamed(is) && is.getType() == type) {
                itemStacks[i] = null;
            }
        }
        return itemStacks;
    }

    public static ItemStack[] removeItemOfName(ItemStack[] itemStacks, String name) {

        for (int i = 0; i < itemStacks.length; i++) {
            if (matchesFilter(itemStacks[i], name, false)) {
                itemStacks[i] = null;
            }
        }
        return itemStacks;
    }

    public static boolean removeItemOfName(InventoryHolder inventoryHolder, ItemStack stack, int quantity, boolean ignoreToSmall) {

        // Force to 1
        stack.setAmount(1);

        // Get name
        String itemName = stack.getItemMeta().getDisplayName();

        int c = ItemUtil.countItemsOfName(inventoryHolder.getInventory().getContents(), itemName);

        if (c == 0 || (c < quantity && !ignoreToSmall)) return false;

        c -= Math.min(c, quantity);

        ItemStack[] stacks = ItemUtil.removeItemOfName(inventoryHolder.getInventory().getContents(), itemName);

        inventoryHolder.getInventory().setContents(stacks);

        int amount = Math.min(c, stack.getType().getMaxStackSize());
        while (amount > 0) {
            // Clone the item and add it
            ItemStack aStack = stack.clone();
            aStack.setAmount(amount);
            inventoryHolder.getInventory().addItem(aStack);
            c -= amount;
            amount = Math.min(c, 64);
        }

        if (inventoryHolder instanceof Player) {
            //noinspection deprecation
            ((Player) inventoryHolder).updateInventory();
        }
        return true;
    }

    public static int countItemsOfName(ItemStack[] itemStacks, String name) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            if (matchesFilter(itemStack, name, false)) count += itemStack.getAmount();
        }
        return count;
    }


    public static int countItemsOfType(ItemStack[] itemStacks, Material type) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && !isNamed(itemStack) && itemStack.getType() == type) {
                count += itemStack.getAmount();
            }
        }
        return count;
    }

    public static int countItemsOfComputedName(ItemStack[] itemStacks, String computedName) {
        int count = 0;

        for (ItemStack itemStack : itemStacks) {
            Optional<String> optItemName = ItemNameCalculator.computeItemName(itemStack);
            if (optItemName.isEmpty()) {
                continue;
            }

            if (optItemName.get().equals(computedName)) {
                ++count;
            }
        }

        return count;
    }

    private static final Set<Material> SWORDS;

    static {
        List<Material> newSwords = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_SWORD")) {
                newSwords.add(material);
            }
        }

        SWORDS = Set.copyOf(newSwords);
    }

    public static boolean isSword(Material item) {
        return SWORDS.contains(item);
    }

    public static boolean isSword(ItemStack stack) {
        return isSword(stack.getType());
    }

    public static boolean isBow(Material item) {
        return item == Material.BOW;
    }

    public static boolean isBow(ItemStack stack) {
        return isBow(stack.getType());
    }

    private static final Set<Material> AXES;

    static {
        List<Material> newAxes = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_AXE")) {
                newAxes.add(material);
            }
        }

        AXES = Set.copyOf(newAxes);
    }

    public static boolean isAxe(Material type) {
        return AXES.contains(type);
    }

    public static boolean isAxe(ItemStack itemStack) {
        return isAxe(itemStack.getType());
    }

    private static final Set<Material> PICKAXES;

    static {
        List<Material> newPickaxes = new ArrayList<>();

        for (Material material : Material.values()) {
            if (material.name().endsWith("_PICKAXE")) {
                newPickaxes.add(material);
            }
        }

        PICKAXES = Set.copyOf(newPickaxes);
    }

    public static boolean isPickaxe(Material type) {
        return PICKAXES.contains(type);
    }

    public static boolean isPickaxe(ItemStack stack) {
        return isPickaxe(stack.getType());
    }

    public static boolean isTool(Material type) {
        return isAxe(type) || isPickaxe(type);
    }

    public static boolean isTool(ItemStack stack) {
        return isTool(stack.getType());
    }

    public static boolean hasItem(Player player, CustomItems type) {
        return player.isValid() && findItemOfName(player.getInventory().getContents(), type.toString());
    }

    public static boolean isItem(ItemStack stack, CustomItems type) {
        return matchesFilter(stack, type.toString(), false);
    }

    public static boolean isInItemFamily(ItemStack stack, ItemFamily family) {
        return matchesFilter(stack, family.getPrefix());
    }

    public static boolean isHoldingItem(Player player, CustomItems type) {
        return player.isValid() && isItem(player.getItemInHand(), type);
    }

    // FIXME: This sucks
    public static boolean isHoldingWeapon(Player player) {
        if (!player.isValid()) {
            return false;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        return isSword(heldItem) || isBow(heldItem);
    }

    public static boolean isHoldingItemInFamily(Player player, ItemFamily family) {
        return player.isValid() && isInItemFamily(player.getItemInHand(), family);
    }

    public static boolean swapItem(Inventory target, CustomItems oldItem, CustomItems newItem) {
        if (target.removeItem(CustomItemCenter.build(oldItem)).isEmpty()) {
            if (target.addItem(CustomItemCenter.build(newItem)).isEmpty()) {
                return true;
            }
            target.addItem(CustomItemCenter.build(oldItem));
        }
        return false;
    }

    private static int getNumOfPiecesWorn(LivingEntity entity, String namePrefix) {
        if (!entity.isValid()) return 0;

        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return 0;
        }

        ItemStack[] armour = equipment.getArmorContents();

        int numWorn = 0;
        for (int i = 0; i < 4; i++) {
            if (matchesFilter(armour[i], namePrefix)) {
                ++numWorn;
            }
        }

        return numWorn;
    }

    private static boolean hasArmour(LivingEntity entity, String namePrefix) {
        return getNumOfPiecesWorn(entity, namePrefix) == 4;
    }

    public static boolean hasAncientArmour(LivingEntity entity) {
        return hasArmour(entity, ChatColor.GOLD + "Ancient");
    }

    public static boolean hasAncientRoyalArmour(LivingEntity entity) {
        int numWorn = getNumOfPiecesWorn(entity, ChatColor.GOLD + "Ancient Royal");

        // Check to see if there's an ancient crown in the mix
        if (numWorn == 3) {
            if (ItemUtil.isItem(entity.getEquipment().getHelmet(), CustomItems.ANCIENT_CROWN)) {
                ++numWorn;
            }
        }

        return numWorn == 4;
    }

    public static boolean hasNecrosArmour(LivingEntity entity) {
        return hasArmour(entity, ChatColor.DARK_RED + "Necros");
    }

    public static boolean hasNectricArmour(LivingEntity entity) {
        return hasArmour(entity, ChatColor.DARK_RED + "Nectric");
    }

    public static boolean hasApocalypticCamouflage(LivingEntity entity) {
        return hasArmour(entity, ChatColor.DARK_GREEN + "Apocalyptic Camouflage");
    }

    public static boolean isNamed(ItemStack stack) {

        return stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName();
    }

    public static boolean isAuthenticCustomItem(String itemName) {

        return !(itemName.indexOf(ChatColor.COLOR_CHAR) == -1);
    }

    public static boolean isAuthenticCustomItem(ItemStack stack) {
        if (!isNamed(stack)) {
            return false;
        }

        String name = stack.getItemMeta().getDisplayName();
        return isAuthenticCustomItem(name);
    }

    public static boolean matchesFilter(ItemStack stack, String filter) {

        return matchesFilter(stack, filter, true);
    }

    public static boolean matchesFilter(ItemStack stack, String filter, boolean loose) {

        return isNamed(stack) && (loose ? stack.getItemMeta().getDisplayName().startsWith(filter) : stack.getItemMeta().getDisplayName().equals(filter));
    }

    public static boolean isCustomItemKind(ItemStack stack, String kind) {
        if (!isNamed(stack)) {
            return false;
        }

        String name = stack.getItemMeta().getDisplayName();
        return isAuthenticCustomItem(name) && name.endsWith(kind);
    }

    public static boolean blocksSweepAttack(ItemStack stack) {
        return isCustomItemKind(stack, "Short Sword");
    }

    public static boolean hasForgeBook(Player player) {

        return player.isValid() && hasForgeBook(player.getItemInHand());
    }

    public static Map<String, String> getItemTags(ItemStack item) {

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;

        Map<String, String> tags = new HashMap<>();
        for (String line : item.getItemMeta().getLore()) {
            String[] args = line.split(":");
            if (args.length < 2) continue;

            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
            tags.put(args[0], args[args.length - 1]);
        }
        return tags;
    }

    public static boolean hasForgeBook(ItemStack item) {

        return item.hasItemMeta() && item.getItemMeta() instanceof BookMeta
                && ((BookMeta) item.getItemMeta()).hasAuthor()
                && ((BookMeta) item.getItemMeta()).getAuthor().equals("The Forge Knights");
    }

    public static int fortuneLevel(ItemStack pickaxe) {

        if (!pickaxe.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)) return 0;
        return pickaxe.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
    }

    public static int fortuneModifier(Material type, int fortuneLevel) {
        int returnValue = 1;

        if (EnvironmentUtil.isOre(type)) {
            switch (fortuneLevel) {
                case 1:
                    if (ChanceUtil.getChance(3)) {
                        returnValue *= 2;
                    }
                    break;
                case 2:
                    if (ChanceUtil.getChance(4)) {
                        returnValue *= 3;
                    } else if (ChanceUtil.getChance(4)) {
                        returnValue *= 2;
                    }
                    break;
                case 3:
                    if (ChanceUtil.getChance(5)) {
                        returnValue *= 4;
                    } else if (ChanceUtil.getChance(5)) {
                        returnValue *= 3;
                    } else if (ChanceUtil.getChance(5)) {
                        returnValue *= 2;
                    }
                    break;
            }
        }

        return returnValue;
    }

    public static double getDamageModifier(ItemStack stack) {
        double modifier = 1;

        Map<String, String> map = getItemTags(stack);
        if (map != null) {
            String modifierString = map.get(ChatColor.RED + "Damage Modifier");
            if (modifierString != null) {
                try {
                    modifier = Double.parseDouble(modifierString);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return modifier;
    }

    public static void copyEnchancements(ItemStack source, ItemStack destination) {
        if (!source.hasItemMeta()) {
            return;
        }

        ItemMeta oldMeta = source.getItemMeta();
        ItemMeta newMeta = destination.getItemMeta();
        oldMeta.getEnchants().forEach((enchantment, level) -> {
            newMeta.addEnchant(enchantment, level, true);
        });
        destination.setItemMeta(newMeta);
    }

    public static ItemStack getUndamagedStack(ItemStack stack) {
        stack = stack.clone();

        ItemMeta filterMeta = stack.getItemMeta();
        if (filterMeta instanceof Damageable && ((Damageable) filterMeta).hasDamage()) {
            ((Damageable) filterMeta).setDamage(0);
            stack.setItemMeta(filterMeta);
        }

        return stack;
    }

    public static List<Map.Entry<String, String>> loadLoreKeyValues(String string) {
        List<Map.Entry<String, String>> result = new ArrayList<>();

        String[] commaSplit = string.split(", ");
        for (String commaSplitEl : commaSplit) {
            String[] keyValueSplit = commaSplitEl.split(": ");
            result.add(new AbstractMap.SimpleEntry<>(keyValueSplit[0], keyValueSplit[1]));
        }

        return result;
    }

    public static String saveLoreKeyValues(List<Map.Entry<String, String>> entries) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (Map.Entry<String, String> entry : entries) {
            if (!first) {
                builder.append(", ");
            }
            first = false;

            builder.append(entry.getKey());
            builder.append(": ");
            builder.append(entry.getValue());
        }

        return builder.toString();
    }
}
