/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.WeaponFamily;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.item.itemstack.SerializableItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;

public class ItemUtil {

    public static final ItemStack[] LEATHER_ARMOR = new ItemStack[]{
            new ItemStack(ItemID.LEATHER_BOOTS), new ItemStack(ItemID.LEATHER_PANTS),
            new ItemStack(ItemID.LEATHER_CHEST), new ItemStack(ItemID.LEATHER_HELMET)
    };
    public static final ItemStack[] IRON_ARMOR = new ItemStack[]{
            new ItemStack(ItemID.IRON_BOOTS), new ItemStack(ItemID.IRON_PANTS),
            new ItemStack(ItemID.IRON_CHEST), new ItemStack(ItemID.IRON_HELMET)
    };
    public static final ItemStack[] GOLD_ARMOR = new ItemStack[]{
            new ItemStack(ItemID.GOLD_BOOTS), new ItemStack(ItemID.GOLD_PANTS),
            new ItemStack(ItemID.GOLD_CHEST), new ItemStack(ItemID.GOLD_HELMET)
    };
    public static final ItemStack[] DIAMOND_ARMOR = new ItemStack[]{
            new ItemStack(ItemID.DIAMOND_BOOTS), new ItemStack(ItemID.DIAMOND_PANTS),
            new ItemStack(ItemID.DIAMOND_CHEST), new ItemStack(ItemID.DIAMOND_HELMET)
    };
    public static final ItemStack[] NO_ARMOR = new ItemStack[] {
            null, null, null, null
    };

    public static ItemStack makeSkull(String name) {

        ItemStack skull = new ItemStack(ItemID.HEAD, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        skullMeta.setOwner(name);
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

    public static SerializableItemStack[] serialize(ItemStack[] stacks) {

        SerializableItemStack[] returnStack = new SerializableItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : new SerializableItemStack(stacks[i]);
        }
        return returnStack;
    }

    public static ItemStack[] unSerialize(SerializableItemStack[] stacks) {

        ItemStack[] returnStack = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            returnStack[i] = stacks[i] == null ? null : stacks[i].bukkitRestore();
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
            if (aItemStack != null && aItemStack.getTypeId() != 0) count++;
        }
        return count;
    }

    public static boolean findItemOfName(ItemStack[] itemStacks, String name) {

        for (ItemStack itemStack : itemStacks) {
            if (matchesFilter(itemStack, name)) return true;
        }
        return false;
    }

    public static boolean removeItemOfType(InventoryHolder inventoryHolder, int typeId, int quantity, boolean ignoreToSmall) {

        int c = ItemUtil.countItemsOfType(inventoryHolder.getInventory().getContents(), typeId);

        if (c == 0 || (c < quantity && !ignoreToSmall)) return false;

        c -= Math.min(c, quantity);

        ItemStack[] stacks = ItemUtil.removeItemOfType(inventoryHolder.getInventory().getContents(), typeId);

        inventoryHolder.getInventory().setContents(stacks);

        int amount = Math.min(c, Material.getMaterial(typeId).getMaxStackSize());
        while (amount > 0) {
            inventoryHolder.getInventory().addItem(new ItemStack(typeId, amount));
            c -= amount;
            amount = Math.min(c, 64);
        }

        if (inventoryHolder instanceof Player) {
            //noinspection deprecation
            ((Player) inventoryHolder).updateInventory();
        }
        return true;
    }

    public static ItemStack[] removeItemOfType(ItemStack[] itemStacks, int typeId) {

        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack is = itemStacks[i];
            if (is != null && !isNamed(is) && is.getTypeId() == typeId) {
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

        int amount = Math.min(c, Material.getMaterial(stack.getTypeId()).getMaxStackSize());
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


    public static int countItemsOfType(ItemStack[] itemStacks, int typeId) {

        int count = 0;
        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null && !isNamed(itemStack) && itemStack.getTypeId() == typeId) {
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

    private static final int[] swords = new int[]{
            ItemID.WOOD_SWORD, ItemID.STONE_SWORD,
            ItemID.IRON_SWORD, ItemID.GOLD_SWORD,
            ItemID.DIAMOND_SWORD
    };

    public static boolean isSword(int itemId) {

        for (int sword : swords) {
            if (itemId == sword) {
                return true;
            }
        }
        return false;
    }


    private static final int[] axes = new int[]{
            ItemID.WOOD_AXE, ItemID.STONE_AXE,
            ItemID.IRON_AXE, ItemID.GOLD_AXE,
            ItemID.DIAMOND_AXE
    };

    public static boolean isAxe(int itemId) {

        for (int axe : axes) {
            if (itemId == axe) {
                return true;
            }
        }
        return false;
    }

    private static final int[] pickAxes = new int[]{
            ItemID.WOOD_PICKAXE, ItemID.STONE_PICKAXE,
            ItemID.IRON_PICK, ItemID.GOLD_PICKAXE,
            ItemID.DIAMOND_PICKAXE
    };

    public static boolean isPickAxe(int itemId) {

        for (int pickAxe : pickAxes) {
            if (itemId == pickAxe) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTool(int itemId) {

        return isAxe(itemId) || isPickAxe(itemId);
    }

    private static final int[] ingots = new int[]{
            ItemID.IRON_BAR, ItemID.GOLD_BAR
    };

    public static boolean isIngot(int itemId) {

        for (int ingot : ingots) {
            if (itemId == ingot) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasItem(Player player, CustomItems type) {

        return player.isValid() && findItemOfName(player.getInventory().getContents(), type.toString());
    }

    public static boolean isHoldingItem(Player player, CustomItems type) {

        return player.isValid() && isItem(player.getItemInHand(), type);
    }

    public static boolean isItem(ItemStack stack, CustomItems type) {

        return matchesFilter(stack, type.toString(), false);
    }

    public static boolean isInItemFamily(ItemStack stack, WeaponFamily family) {
        return matchesFilter(stack, family.getPrefix());
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

    public static boolean matchesFilter(ItemStack stack, String filter) {

        return matchesFilter(stack, filter, true);
    }

    public static boolean matchesFilter(ItemStack stack, String filter, boolean loose) {

        return isNamed(stack) && (loose ? stack.getItemMeta().getDisplayName().startsWith(filter) : stack.getItemMeta().getDisplayName().equals(filter));
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

    public static int fortuneModifier(int typeId, int fortuneLevel) {

        int returnValue = 1;
        switch (typeId) {
            case BlockID.IRON_ORE:
            case BlockID.COAL_ORE:
            case BlockID.GOLD_ORE:
            case BlockID.LAPIS_LAZULI_ORE:
            case BlockID.REDSTONE_ORE:
            case BlockID.GLOWING_REDSTONE_ORE:
            case BlockID.DIAMOND_ORE:
            case BlockID.EMERALD_ORE:
            case BlockID.QUARTZ_ORE:
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
}
