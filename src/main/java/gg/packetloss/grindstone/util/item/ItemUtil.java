/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ItemUtil {

    public static final ItemStack[] LEATHER_ARMOR = new ItemStack[]{
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

    public static ItemStack makeSkull(OfflinePlayer offlinePlayer) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwningPlayer(offlinePlayer);
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

    public static int countItemsOfType(ItemStack[] itemStacks, ItemStack[] checkItemStacks) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            for (ItemStack checkItem : checkItemStacks) {
                if (itemStack != null && !isNamed(itemStack) && itemStack.equals(checkItem)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static final Set<Material> swords = Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD,
            Material.IRON_SWORD, Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD
    );

    public static boolean isSword(Material item) {
        return swords.contains(item);
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

    private static final Set<Material> AXES = Set.of(
            Material.WOODEN_AXE, Material.STONE_AXE,
            Material.IRON_AXE, Material.GOLDEN_AXE,
            Material.DIAMOND_AXE
    );

    public static boolean isAxe(Material type) {
        return AXES.contains(type);
    }

    public static boolean isAxe(ItemStack itemStack) {
        return isAxe(itemStack.getType());
    }

    private static final Set<Material> PICKAXES = Set.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE,
            Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE
    );

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

    private static final Set<Material> INGOTS = Set.of(
            Material.IRON_INGOT, Material.GOLD_INGOT
    );

    public static boolean isIngot(Material type) {
        return INGOTS.contains(type);
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
    @Deprecated
    public static boolean isHoldingMasterSword(Player player) {
        return isHoldingItem(player, CustomItems.MASTER_SWORD) || isHoldingItem(player, CustomItems.MASTER_SHORT_SWORD);
    }

    // FIXME: This also sucks
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
    public static boolean hasAncientArmour(LivingEntity entity) {

        if (!entity.isValid()) return false;

        ItemStack[] armour;
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) armour = equipment.getArmorContents();
        else return false;

        boolean[] b = new boolean[]{false, false, false, false};

        for (int i = 0; i < 4; i++) {
            b[i] = matchesFilter(armour[i], ChatColor.GOLD + "Ancient");
        }
        return b[0] && b[1] && b[2] && b[3];
    }

    public static boolean hasNecrosArmour(LivingEntity entity) {

        if (!entity.isValid()) return false;

        ItemStack[] armour;
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) armour = equipment.getArmorContents();
        else return false;

        boolean[] b = new boolean[]{false, false, false, false};

        for (int i = 0; i < 4; i++) {
            b[i] = matchesFilter(armour[i], ChatColor.DARK_RED + "Necros");
        }
        return b[0] && b[1] && b[2] && b[3];
    }

    public static boolean hasNectricArmour(LivingEntity entity) {

        if (!entity.isValid()) return false;

        ItemStack[] armour;
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) armour = equipment.getArmorContents();
        else return false;

        boolean[] b = new boolean[]{false, false, false, false};

        for (int i = 0; i < 4; i++) {
            b[i] = matchesFilter(armour[i], ChatColor.DARK_RED + "Nectric");
        }
        return b[0] && b[1] && b[2] && b[3];
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
}
