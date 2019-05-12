/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Collection;
import java.util.HashMap;

import static gg.packetloss.grindstone.items.custom.CustomItems.*;

public class CustomItemCenter {
    private static HashMap<CustomItems, CustomItem> items = new HashMap<>();

    private static void addItem(CustomItem item) {
        items.put(item.getItem(), item);
    }

    static {
        // Ancient Armor
        CustomEquipment ancientCrown = new CustomEquipment(ANCIENT_CROWN, Material.GOLD_HELMET);
        ancientCrown.addEnchant(Enchantment.DURABILITY, 3);
        ancientCrown.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientCrown.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientCrown.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientCrown.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientCrown.addEnchant(Enchantment.OXYGEN, 3);
        ancientCrown.addEnchant(Enchantment.WATER_WORKER, 1);
        ancientCrown.addSource(ItemSource.MARKET);
        ancientCrown.addSource(ItemSource.GIANT_BOSS);
        ancientCrown.addUse("Set Effect: Ancient Armor");
        ancientCrown.addUse("Double Health Regen");
        ancientCrown.addUse("Double XP Gain");
        ancientCrown.addUse("Acts as an Imbued Crystal");
        ancientCrown.addUse("Acts as a Gem of Darkness");
        ancientCrown.addUse("Acts as an Ancient Helmet");
        ancientCrown.addUse("Repaired with XP");
        addItem(ancientCrown);

        CustomEquipment ancientHelmet = new CustomEquipment(ANCIENT_HELMET, Material.CHAINMAIL_HELMET);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientHelmet.addEnchant(Enchantment.OXYGEN, 3);
        ancientHelmet.addEnchant(Enchantment.WATER_WORKER, 1);
        ancientHelmet.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientHelmet.addSource(ItemSource.GRAVE_YARD);
        ancientHelmet.addSource(ItemSource.MARKET);
        ancientHelmet.addUse("Set Effect: Ancient Armor");
        ancientHelmet.addUse("Repaired with XP");
        addItem(ancientHelmet);

        CustomEquipment ancientChestplate = new CustomEquipment(ANCIENT_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientChestplate.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientChestplate.addSource(ItemSource.GRAVE_YARD);
        ancientChestplate.addSource(ItemSource.MARKET);
        ancientChestplate.addUse("Set Effect: Ancient Armor");
        ancientChestplate.addUse("Repaired with XP");
        addItem(ancientChestplate);

        CustomEquipment ancientLeggings = new CustomEquipment(ANCIENT_LEGGINGS, Material.CHAINMAIL_LEGGINGS);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientLeggings.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientLeggings.addSource(ItemSource.GRAVE_YARD);
        ancientLeggings.addSource(ItemSource.MARKET);
        ancientLeggings.addUse("Set Effect: Ancient Armor");
        ancientLeggings.addUse("Repaired with XP");
        addItem(ancientLeggings);

        CustomEquipment ancientBoots = new CustomEquipment(ANCIENT_BOOTS, Material.CHAINMAIL_BOOTS);
        ancientBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        ancientBoots.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientBoots.addSource(ItemSource.GRAVE_YARD);
        ancientBoots.addSource(ItemSource.MARKET);
        ancientBoots.addUse("Set Effect: Ancient Armor");
        ancientBoots.addUse("Repaired with XP");
        addItem(ancientBoots);

        // Nectric Armor
        CustomEquipment nectricHelmet = new CustomEquipment(NECTRIC_HELMET, Material.DIAMOND_HELMET);
        nectricHelmet.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        nectricHelmet.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        nectricHelmet.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        nectricHelmet.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        nectricHelmet.addEnchant(Enchantment.OXYGEN, 3);
        nectricHelmet.addEnchant(Enchantment.WATER_WORKER, 1);
        nectricHelmet.addSource(ItemSource.MARKET);
        nectricHelmet.addSource(ItemSource.PATIENT_X);
        nectricHelmet.addUse("Set Effect: Necrotic Armor");
        addItem(nectricHelmet);

        CustomEquipment nectricChestplate = new CustomEquipment(NECTRIC_CHESTPLATE, Material.DIAMOND_CHESTPLATE);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        nectricChestplate.addSource(ItemSource.MARKET);
        nectricChestplate.addSource(ItemSource.PATIENT_X);
        nectricChestplate.addUse("Set Effect: Necrotic Armor");
        addItem(nectricChestplate);

        CustomEquipment nectricLeggings = new CustomEquipment(NECTRIC_LEGGINGS, Material.DIAMOND_LEGGINGS);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        nectricLeggings.addSource(ItemSource.MARKET);
        nectricLeggings.addSource(ItemSource.PATIENT_X);
        nectricLeggings.addUse("Set Effect: Necrotic Armor");
        addItem(nectricLeggings);

        CustomEquipment nectricBoots = new CustomEquipment(NECTRIC_BOOTS, Material.DIAMOND_BOOTS);
        nectricBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        nectricBoots.addSource(ItemSource.MARKET);
        nectricBoots.addSource(ItemSource.PATIENT_X);
        nectricBoots.addUse("Set Effect: Necrotic Armor");
        addItem(nectricBoots);

        // Necros Armor
        CustomEquipment necrosHelmet = new CustomEquipment(NECROS_HELMET, Material.DIAMOND_HELMET);
        necrosHelmet.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        necrosHelmet.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        necrosHelmet.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        necrosHelmet.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        necrosHelmet.addEnchant(Enchantment.OXYGEN, 3);
        necrosHelmet.addEnchant(Enchantment.WATER_WORKER, 1);
        necrosHelmet.addSource(ItemSource.MARKET);
        necrosHelmet.addSource(ItemSource.PATIENT_X);
        necrosHelmet.addUse("Set Effect: Necrotic Armor");
        necrosHelmet.addUse("Repaired with XP");
        addItem(necrosHelmet);

        CustomEquipment necrosChestplate = new CustomEquipment(NECROS_CHESTPLATE, Material.DIAMOND_CHESTPLATE);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        necrosChestplate.addSource(ItemSource.MARKET);
        necrosChestplate.addSource(ItemSource.PATIENT_X);
        necrosChestplate.addUse("Set Effect: Necrotic Armor");
        necrosChestplate.addUse("Repaired with XP");
        addItem(necrosChestplate);

        CustomEquipment necrosLeggings = new CustomEquipment(NECROS_LEGGINGS, Material.DIAMOND_LEGGINGS);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        necrosLeggings.addSource(ItemSource.MARKET);
        necrosLeggings.addSource(ItemSource.PATIENT_X);
        necrosLeggings.addUse("Set Effect: Necrotic Armor");
        necrosLeggings.addUse("Repaired with XP");
        addItem(necrosLeggings);

        CustomEquipment necrosBoots = new CustomEquipment(NECROS_BOOTS, Material.DIAMOND_BOOTS);
        necrosBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        necrosBoots.addSource(ItemSource.MARKET);
        necrosBoots.addSource(ItemSource.PATIENT_X);
        necrosBoots.addUse("Set Effect: Necrotic Armor");
        necrosBoots.addUse("Repaired with XP");
        addItem(necrosBoots);

        // Master Weapons
        CustomWeapon masterSword = new CustomWeapon(MASTER_SWORD, Material.DIAMOND_SWORD, 2);
        masterSword.addSource(ItemSource.GIANT_BOSS);
        masterSword.addSource(ItemSource.MARKET);
        masterSword.addUse("Repairable at any Sacrificial Pit");
        masterSword.addUse("Conditional Effects");
        addItem(masterSword);

        CustomWeapon masterBow = new CustomWeapon(MASTER_BOW, Material.BOW, 2);
        masterBow.addSource(ItemSource.GIANT_BOSS);
        masterBow.addSource(ItemSource.MARKET);
        masterBow.addUse("Repairable at any Sacrificial Pit");
        masterBow.addUse("Conditional Effects");
        addItem(masterBow);

        // Unleashed Weapons
        CustomWeapon unleashedSword = new CustomWeapon(UNLEASHED_SWORD, Material.DIAMOND_SWORD, 2.25);
        unleashedSword.addSource(ItemSource.GRAVE_YARD);
        unleashedSword.addSource(ItemSource.MARKET);
        unleashedSword.addUse("Repairable at any Sacrificial Pit, but requires 2 Imbued Crystals " +
                "for every 11% damage, or 1 Imbued Crystal if repaired inside of the Grave Yard rewards room.");
        unleashedSword.addUse("Global Effects");
        addItem(unleashedSword);

        CustomWeapon unleashedBow = new CustomWeapon(UNLEASHED_BOW, Material.BOW, 2.25);
        unleashedBow.addSource(ItemSource.GRAVE_YARD);
        unleashedBow.addSource(ItemSource.MARKET);
        unleashedBow.addUse("Repairable at any Sacrificial Pit, but requires 2 Imbued Crystals " +
                "for every 11% damage, or 1 Imbued Crystal if repaired inside of the Grave Yard rewards room.");
        unleashedBow.addUse("Global Effects");
        addItem(unleashedBow);

        // Fear Weapons
        CustomWeapon fearSword = new CustomWeapon(FEAR_SWORD, Material.DIAMOND_SWORD, 2.25);
        fearSword.addSource(ItemSource.GRAVE_YARD);
        fearSword.addSource(ItemSource.MARKET);
        fearSword.addUse("Repairable at any Sacrificial Pit, but requires 2 Gems of Darkness " +
                "for every 11% damage, or 1 Gem of Darkness if repaired inside of the Grave Yard rewards room.");
        fearSword.addUse("Global Effects");
        addItem(fearSword);

        CustomWeapon fearBow = new CustomWeapon(FEAR_BOW, Material.BOW, 2.25);
        fearBow.addSource(ItemSource.GRAVE_YARD);
        fearBow.addSource(ItemSource.MARKET);
        fearBow.addUse("Repairable at any Sacrificial Pit, but requires 2 Gems of Darkness " +
                "for every 11% damage, or 1 Gem of Darkness if repaired inside of the Grave Yard rewards room.");
        fearBow.addUse("Global Effects");
        addItem(fearBow);

        // Shadow Items
        CustomWeapon shadowSword = new CustomWeapon(SHADOW_SWORD, Material.DIAMOND_SWORD, 5);
        fearBow.addUse("Slows your opponent with every hit.");
        addItem(shadowSword);

        CustomWeapon shadowBow = new CustomWeapon(SHADOW_BOW, Material.BOW, 5);
        fearBow.addUse("Slows your opponent with every hit.");
        addItem(shadowBow);

        // Red Items
        CustomItem redFeather = new CustomItem(RED_FEATHER, Material.FEATHER);
        redFeather.addSource(ItemSource.WILDERNESS_MOBS);
        redFeather.addSource(ItemSource.MARKET);
        redFeather.addUse("Consumes redstone to prevent up to 100% damage, " +
                "but has a cool down based on the amount of damage taken.");
        addItem(redFeather);

        // God Armor
        CustomEquipment godHelmet = new CustomEquipment(GOD_HELMET, Material.DIAMOND_HELMET);
        godHelmet.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        godHelmet.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        godHelmet.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        godHelmet.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        godHelmet.addEnchant(Enchantment.OXYGEN, 3);
        godHelmet.addEnchant(Enchantment.WATER_WORKER, 1);
        godHelmet.addSource(ItemSource.SACRIFICIAL_PIT);
        godHelmet.addSource(ItemSource.GRAVE_YARD);
        godHelmet.addSource(ItemSource.MARKET);
        addItem(godHelmet);

        CustomEquipment godChestplate = new CustomEquipment(GOD_CHESTPLATE, Material.DIAMOND_CHESTPLATE);
        godChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        godChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        godChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        godChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        godChestplate.addSource(ItemSource.SACRIFICIAL_PIT);
        godChestplate.addSource(ItemSource.GRAVE_YARD);
        godChestplate.addSource(ItemSource.MARKET);
        addItem(godChestplate);

        CustomEquipment godLeggings = new CustomEquipment(GOD_LEGGINGS, Material.DIAMOND_LEGGINGS);
        godLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        godLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        godLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        godLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        godLeggings.addSource(ItemSource.SACRIFICIAL_PIT);
        godLeggings.addSource(ItemSource.GRAVE_YARD);
        godLeggings.addSource(ItemSource.MARKET);
        addItem(godLeggings);

        CustomEquipment godBoots = new CustomEquipment(GOD_BOOTS, Material.DIAMOND_BOOTS);
        godBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        godBoots.addSource(ItemSource.SACRIFICIAL_PIT);
        godBoots.addSource(ItemSource.GRAVE_YARD);
        godBoots.addSource(ItemSource.MARKET);
        addItem(godBoots);

        // God Weapons
        CustomWeapon godSword = new CustomWeapon(GOD_SWORD, Material.DIAMOND_SWORD, 1.5);
        godSword.addSource(ItemSource.SACRIFICIAL_PIT);
        godSword.addSource(ItemSource.GRAVE_YARD);
        godSword.addSource(ItemSource.MARKET);
        addItem(godSword);

        CustomWeapon godBow = new CustomWeapon(GOD_BOW, Material.BOW, 1.5);
        godBow.addSource(ItemSource.SACRIFICIAL_PIT);
        godBow.addSource(ItemSource.GRAVE_YARD);
        godBow.addSource(ItemSource.MARKET);
        addItem(godBow);

        // God Tools
        CustomEquipment godAxe = new CustomEquipment(GOD_AXE, Material.DIAMOND_AXE);
        godAxe.addEnchant(Enchantment.DIG_SPEED, 4);
        godAxe.addSource(ItemSource.SACRIFICIAL_PIT);
        godAxe.addSource(ItemSource.GRAVE_YARD);
        godAxe.addSource(ItemSource.MARKET);
        addItem(godAxe);

        CustomEquipment legendaryGodAxe = new CustomEquipment(LEGENDARY_GOD_AXE, Material.DIAMOND_AXE);
        legendaryGodAxe.addEnchant(Enchantment.DAMAGE_ALL, 5);
        legendaryGodAxe.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 5);
        legendaryGodAxe.addEnchant(Enchantment.DAMAGE_UNDEAD, 5);
        legendaryGodAxe.addEnchant(Enchantment.DIG_SPEED, 5);
        legendaryGodAxe.addEnchant(Enchantment.DURABILITY, 3);
        legendaryGodAxe.addSource(ItemSource.SACRIFICIAL_PIT);
        legendaryGodAxe.addSource(ItemSource.GRAVE_YARD);
        legendaryGodAxe.addSource(ItemSource.MARKET);
        addItem(legendaryGodAxe);

        CustomEquipment godPickaxe = new CustomEquipment(GOD_PICKAXE, Material.DIAMOND_PICKAXE);
        godPickaxe.addEnchant(Enchantment.DIG_SPEED, 4);
        godPickaxe.addEnchant(Enchantment.SILK_TOUCH, 1);
        godPickaxe.addSource(ItemSource.SACRIFICIAL_PIT);
        godPickaxe.addSource(ItemSource.GRAVE_YARD);
        godPickaxe.addSource(ItemSource.MARKET);
        addItem(godPickaxe);

        CustomEquipment legendaryGodPickaxe = new CustomEquipment(LEGENDARY_GOD_PICKAXE, Material.DIAMOND_PICKAXE);
        legendaryGodPickaxe.addEnchant(Enchantment.DIG_SPEED, 5);
        legendaryGodPickaxe.addEnchant(Enchantment.DURABILITY, 3);
        legendaryGodPickaxe.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 3);
        legendaryGodPickaxe.addSource(ItemSource.SACRIFICIAL_PIT);
        legendaryGodPickaxe.addSource(ItemSource.GRAVE_YARD);
        legendaryGodPickaxe.addSource(ItemSource.MARKET);
        addItem(legendaryGodPickaxe);

        // Combat Potions
        ItemStack instantDmgPot = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
        CustomPotion divineCombatPotion = new CustomPotion(DIVINE_COMBAT_POTION, instantDmgPot);
        divineCombatPotion.addEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 3);
        divineCombatPotion.addEffect(PotionEffectType.REGENERATION, 20 * 600, 3);
        divineCombatPotion.addEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 3);
        divineCombatPotion.addEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 3);
        divineCombatPotion.addEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 3);
        divineCombatPotion.addSource(ItemSource.SACRIFICIAL_PIT);
        divineCombatPotion.addSource(ItemSource.GRAVE_YARD);
        divineCombatPotion.addSource(ItemSource.MARKET);
        addItem(divineCombatPotion);

        CustomPotion holyCombatPotion = new CustomPotion(HOLY_COMBAT_POTION, instantDmgPot);
        holyCombatPotion.addEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 45, 3);
        holyCombatPotion.addEffect(PotionEffectType.REGENERATION, 20 * 45, 3);
        holyCombatPotion.addEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 45, 3);
        holyCombatPotion.addEffect(PotionEffectType.WATER_BREATHING, 20 * 45, 3);
        holyCombatPotion.addEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 45, 3);
        holyCombatPotion.addSource(ItemSource.SACRIFICIAL_PIT);
        holyCombatPotion.addSource(ItemSource.GRAVE_YARD);
        holyCombatPotion.addSource(ItemSource.MARKET);
        addItem(holyCombatPotion);

        CustomPotion extremeCombatPotion = new CustomPotion(EXTREME_COMBAT_POTION, instantDmgPot);
        extremeCombatPotion.addEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 2);
        extremeCombatPotion.addEffect(PotionEffectType.REGENERATION, 20 * 600, 2);
        extremeCombatPotion.addEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 2);
        extremeCombatPotion.addEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 2);
        extremeCombatPotion.addEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2);
        extremeCombatPotion.addSource(ItemSource.SACRIFICIAL_PIT);
        extremeCombatPotion.addSource(ItemSource.GRAVE_YARD);
        extremeCombatPotion.addSource(ItemSource.MARKET);
        addItem(extremeCombatPotion);

        // Grave Yard Gems
        CustomItem gemOfLife = new CustomItem(GEM_OF_LIFE, Material.DIAMOND);
        gemOfLife.addSource(ItemSource.GRAVE_YARD);
        gemOfLife.addSource(ItemSource.MARKET);
        gemOfLife.addUse("Preserves your inventory when you die in the Grave Yard, " +
                "or when you die during a thunderstorm.");
        addItem(gemOfLife);

        CustomItem gemOfDarkness = new CustomItem(GEM_OF_DARKNESS, Material.EMERALD);
        gemOfDarkness.addSource(ItemSource.GRAVE_YARD);
        gemOfDarkness.addSource(ItemSource.MARKET);
        gemOfDarkness.addUse("Protects you from the Grave Yard's blindness effect.");
        gemOfDarkness.addUse("Used to repair Fear weapons.");
        addItem(gemOfDarkness);

        CustomItem imbuedCrystal = new CustomItem(IMBUED_CRYSTAL, Material.DIAMOND);
        imbuedCrystal.addSource(ItemSource.GRAVE_YARD);
        imbuedCrystal.addSource(ItemSource.MARKET);
        imbuedCrystal.addUse("Compacts gold nuggets into bars.");
        imbuedCrystal.addUse("Compacts gold bars into blocks.");
        imbuedCrystal.addUse("Used to repair Unleashed Weapons.");
        addItem(imbuedCrystal);

        // Phantom Items
        CustomItem phantomGold = new CustomItem(PHANTOM_GOLD, Material.GOLD_INGOT);
        phantomGold.addSource(ItemSource.GOLD_RUSH);
        phantomGold.addSource(ItemSource.GRAVE_YARD);
        phantomGold.addUse("When sacrificed gives 50 Skrin, or 100 Skrin " +
                "if sacrificed in the Grave Yard rewards room.");
        addItem(phantomGold);

        CustomItem phantomClock = new CustomItem(PHANTOM_CLOCK, Material.WATCH);
        phantomClock.addSource(ItemSource.GRAVE_YARD);
        phantomClock.addUse("Teleports the player strait to the rewards room of the Grave Yard.");
        addItem(phantomClock);

        CustomItem phantomHymn = new CustomItem(PHANTOM_HYMN, Material.BOOK);
        phantomHymn.addSource(ItemSource.GOLD_RUSH);
        phantomHymn.addSource(ItemSource.MARKET);
        phantomHymn.addLore(ChatColor.RED + "A hymn of dark origins...");
        phantomHymn.addUse("Teleports the player through directly to the end of the Grave Yard maze.");
        phantomHymn.addUse("Teleports the player between rooms in the Freaky Four fight.");
        phantomHymn.addUse("At the cost of the item, teleports the player into Patient X's room.");
        addItem(phantomHymn);

        // Ninja Guild
        CustomItem ninjaStar = new CustomItem(NINJA_STAR, Material.NETHER_STAR);
        ninjaStar.addSource(ItemSource.NINJA_GUILD);
        ninjaStar.addUse("Teleports the player to the Ninja Guild.");
        addItem(ninjaStar);

        // Flight Items
        CustomItem pixieDust = new CustomItem(PIXIE_DUST, Material.SUGAR);
        pixieDust.addSource(ItemSource.SACRIFICIAL_PIT);
        pixieDust.addSource(ItemSource.GOLD_RUSH);
        pixieDust.addSource(ItemSource.MARKET);
        pixieDust.addUse("Allows the player to fly in permitted areas until " +
                "they have ran out of Pixie Dust items to consume.");
        addItem(pixieDust);

        CustomItem magicbucket = new CustomItem(MAGIC_BUCKET, Material.BUCKET);
        magicbucket.addSource(ItemSource.GIANT_BOSS);
        magicbucket.addSource(ItemSource.MARKET);
        magicbucket.addUse("Allows the player to fly indefinitely in permitted areas.");
        magicbucket.addUse("When used on a cow, it will turn into Mad Milk.");
        addItem(magicbucket);

        // Animal Bows
        CustomEquipment batBow = new CustomEquipment(BAT_BOW, Material.BOW);
        batBow.addSource(ItemSource.GRAVE_YARD);
        batBow.addSource(ItemSource.MARKET);
        batBow.addUse("Creates bats at the point where a fired arrow lands.");
        batBow.addUse("Creates a trail of bats following any fired arrow.");
        addItem(batBow);

        CustomEquipment chickenBow = new CustomEquipment(CHICKEN_BOW, Material.BOW);
        chickenBow.addUse("Creates chickens at the point where a fired arrow lands.");
        chickenBow.addUse("Creates a trail of chickens following any fired arrow.");
        addItem(chickenBow);

        CustomItem chickenHymn = new CustomItem(CHICKEN_HYMN, Material.BOOK);
        chickenHymn.addLore(ChatColor.BLUE + "Cluck cluck!");
        chickenHymn.addUse("Turns nearby items into chickens, and nearby chickens into cooked chicken.");
        addItem(chickenHymn);

        CustomItem godFish = new CustomItem(GOD_FISH, Material.RAW_FISH);
        godFish.addSource(ItemSource.ARROW_FISHING);
        godFish.addSource(ItemSource.MARKET);
        godFish.addUse("On consumption applies 30 seconds of the Hulk prayer.");
        addItem(godFish);

        CustomItem overSeerBow = new CustomItem(OVERSEER_BOW, Material.BOW);
        overSeerBow.addEnchant(Enchantment.ARROW_DAMAGE, 2);
        overSeerBow.addEnchant(Enchantment.ARROW_FIRE, 1);
        overSeerBow.addSource(ItemSource.SACRIFICIAL_PIT);
        overSeerBow.addSource(ItemSource.MARKET);
        addItem(overSeerBow);

        CustomItem barbarianBones = new CustomItem(BARBARIAN_BONE, Material.BONE);
        barbarianBones.addSource(ItemSource.GIANT_BOSS);
        barbarianBones.addSource(ItemSource.GRAVE_YARD);
        barbarianBones.addUse("Improves the drops of the Giant Boss if in a suitable quantity.");
        addItem(barbarianBones);

        CustomPotion potionOfRestitution = new CustomPotion(POTION_OF_RESTITUTION,
                new Potion(PotionType.POISON).toItemStack(1));
        potionOfRestitution.addEffect(PotionEffectType.POISON, 20 * 10, 1);
        potionOfRestitution.addSource(ItemSource.WILDERNESS_MOBS);
        potionOfRestitution.addSource(ItemSource.MARKET);
        potionOfRestitution.addUse("Returns you to your last death point if a teleport can reach the location.");
        addItem(potionOfRestitution);

        CustomItem scrollOfSummation = new CustomItem(SCROLL_OF_SUMMATION, Material.PAPER);
        scrollOfSummation.addSource(ItemSource.WILDERNESS_MOBS);
        scrollOfSummation.addSource(ItemSource.DROP_PARTY);
        scrollOfSummation.addSource(ItemSource.PATIENT_X);
        scrollOfSummation.addSource(ItemSource.MARKET);
        scrollOfSummation.addUse("At the cost of the item, will compact coal, iron, gold, redstone, lapis, diamonds, and emerald.");
        addItem(scrollOfSummation);

        CustomItem hymnOfSummation = new CustomItem(HYMN_OF_SUMMATION, Material.BOOK);
        hymnOfSummation.addSource(ItemSource.PATIENT_X);
        hymnOfSummation.addSource(ItemSource.MARKET);
        hymnOfSummation.addUse("Upon use, will compact coal, iron, gold, redstone, lapis, diamonds, and emerald.");
        addItem(hymnOfSummation);

        CustomItem madMilk = new CustomItem(MAD_MILK, Material.MILK_BUCKET);
        madMilk.addSource(ItemSource.MARKET);
        madMilk.addUse("If thrown into a brewing vat at the factory, a melt down will occur in which all undead creatures die.");
        madMilk.addUse("When drank, it will turn into a Magic Bucket.");
        addItem(madMilk);
    }

    public static Collection<CustomItem> values() {
        return items.values();
    }

    public static ItemStack build(CustomItems item) {
        return items.get(item).build();
    }

    public static ItemStack build(CustomItems item, int amt) {
        ItemStack stack = items.get(item).build();
        if (amt > stack.getMaxStackSize()) throw new IllegalArgumentException();
        stack.setAmount(amt);
        return stack;
    }
}