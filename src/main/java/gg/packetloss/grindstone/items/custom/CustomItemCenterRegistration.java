/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import gg.packetloss.grindstone.items.implementations.MagicBucketImpl;
import gg.packetloss.grindstone.util.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static gg.packetloss.grindstone.items.custom.CustomItems.*;

public class CustomItemCenterRegistration {
    public static void register(Consumer<CustomItem> itemConsumer) {
        // Admin Weapons
        CustomWeapon pwngShortSword = new CustomWeapon(PWNG_SHORT_SWORD, Material.DIAMOND_SWORD, 10000, 4);
        itemConsumer.accept(pwngShortSword);

        CustomWeapon pwngBow = new CustomWeapon(PWNG_BOW, Material.BOW, 10000);
        itemConsumer.accept(pwngBow);

        // Ancient Armor
        CustomEquipment ancientCrown = new CustomEquipment(ANCIENT_CROWN, Material.GOLDEN_HELMET);
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
        ancientCrown.addUse("Set Effect: Ancient Royal Armor");
        ancientCrown.addUse("Double Health Regen");
        ancientCrown.addUse("Double XP Gain");
        ancientCrown.addUse("Acts as an Imbued Crystal");
        ancientCrown.addUse("Acts as a Gem of Darkness");
        ancientCrown.addUse("Acts as an Ancient Helmet");
        ancientCrown.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientCrown);

        CustomEquipment ancientRoyalHelmet = new CustomEquipment(ANCIENT_ROYAL_HELMET, Material.CHAINMAIL_HELMET);
        ancientRoyalHelmet.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientRoyalHelmet.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientRoyalHelmet.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientRoyalHelmet.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientRoyalHelmet.addEnchant(Enchantment.OXYGEN, 3);
        ancientRoyalHelmet.addEnchant(Enchantment.WATER_WORKER, 1);
        ancientRoyalHelmet.addSource(ItemSource.FREAKY_FOUR);
        ancientRoyalHelmet.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientRoyalHelmet.addSource(ItemSource.GRAVE_YARD);
        ancientRoyalHelmet.addSource(ItemSource.MARKET);
        ancientRoyalHelmet.addUse("Set Effect: Ancient Armor");
        ancientRoyalHelmet.addUse("Set Effect: Ancient Royal Armor");
        ancientRoyalHelmet.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientRoyalHelmet);

        CustomEquipment ancientRoyalChestplate = new CustomEquipment(ANCIENT_ROYAL_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE);
        ancientRoyalChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientRoyalChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientRoyalChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientRoyalChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientRoyalChestplate.addSource(ItemSource.FREAKY_FOUR);
        ancientRoyalChestplate.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientRoyalChestplate.addSource(ItemSource.GRAVE_YARD);
        ancientRoyalChestplate.addSource(ItemSource.MARKET);
        ancientRoyalChestplate.addUse("Set Effect: Ancient Armor");
        ancientRoyalChestplate.addUse("Set Effect: Ancient Royal Armor");
        ancientRoyalChestplate.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientRoyalChestplate);

        CustomEquipment ancientRoyalLeggings = new CustomEquipment(ANCIENT_ROYAL_LEGGINGS, Material.CHAINMAIL_LEGGINGS);
        ancientRoyalLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientRoyalLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientRoyalLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientRoyalLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientRoyalLeggings.addSource(ItemSource.FREAKY_FOUR);
        ancientRoyalLeggings.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientRoyalLeggings.addSource(ItemSource.GRAVE_YARD);
        ancientRoyalLeggings.addSource(ItemSource.MARKET);
        ancientRoyalLeggings.addUse("Set Effect: Ancient Armor");
        ancientRoyalLeggings.addUse("Set Effect: Ancient Royal Armor");
        ancientRoyalLeggings.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientRoyalLeggings);

        CustomEquipment ancientRoyalBoots = new CustomEquipment(ANCIENT_ROYAL_BOOTS, Material.CHAINMAIL_BOOTS);
        ancientRoyalBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientRoyalBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientRoyalBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientRoyalBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientRoyalBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        ancientRoyalBoots.addSource(ItemSource.FREAKY_FOUR);
        ancientRoyalBoots.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientRoyalBoots.addSource(ItemSource.GRAVE_YARD);
        ancientRoyalBoots.addSource(ItemSource.MARKET);
        ancientRoyalBoots.addUse("Set Effect: Ancient Armor");
        ancientRoyalBoots.addUse("Set Effect: Ancient Royal Armor");
        ancientRoyalBoots.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientRoyalBoots);

        CustomEquipment ancientHelmet = new CustomEquipment(ANCIENT_HELMET, Material.CHAINMAIL_HELMET);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientHelmet.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientHelmet.addEnchant(Enchantment.OXYGEN, 3);
        ancientHelmet.addEnchant(Enchantment.WATER_WORKER, 1);
        ancientHelmet.addSource(ItemSource.FREAKY_FOUR);
        ancientHelmet.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientHelmet.addSource(ItemSource.GRAVE_YARD);
        ancientHelmet.addSource(ItemSource.MARKET);
        ancientHelmet.addUse("Set Effect: Ancient Armor");
        ancientHelmet.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientHelmet);

        CustomEquipment ancientChestplate = new CustomEquipment(ANCIENT_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientChestplate.addSource(ItemSource.FREAKY_FOUR);
        ancientChestplate.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientChestplate.addSource(ItemSource.GRAVE_YARD);
        ancientChestplate.addSource(ItemSource.MARKET);
        ancientChestplate.addUse("Set Effect: Ancient Armor");
        ancientChestplate.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientChestplate);

        CustomEquipment ancientLeggings = new CustomEquipment(ANCIENT_LEGGINGS, Material.CHAINMAIL_LEGGINGS);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientLeggings.addSource(ItemSource.FREAKY_FOUR);
        ancientLeggings.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientLeggings.addSource(ItemSource.GRAVE_YARD);
        ancientLeggings.addSource(ItemSource.MARKET);
        ancientLeggings.addUse("Set Effect: Ancient Armor");
        ancientLeggings.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientLeggings);

        CustomEquipment ancientBoots = new CustomEquipment(ANCIENT_BOOTS, Material.CHAINMAIL_BOOTS);
        ancientBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        ancientBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        ancientBoots.addSource(ItemSource.FREAKY_FOUR);
        ancientBoots.addSource(ItemSource.SACRIFICIAL_PIT);
        ancientBoots.addSource(ItemSource.GRAVE_YARD);
        ancientBoots.addSource(ItemSource.MARKET);
        ancientBoots.addUse("Set Effect: Ancient Armor");
        ancientBoots.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(ancientBoots);

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
        itemConsumer.accept(nectricHelmet);

        CustomEquipment nectricChestplate = new CustomEquipment(NECTRIC_CHESTPLATE, Material.DIAMOND_CHESTPLATE);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        nectricChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        nectricChestplate.addSource(ItemSource.MARKET);
        nectricChestplate.addSource(ItemSource.PATIENT_X);
        nectricChestplate.addUse("Set Effect: Necrotic Armor");
        itemConsumer.accept(nectricChestplate);

        CustomEquipment nectricLeggings = new CustomEquipment(NECTRIC_LEGGINGS, Material.DIAMOND_LEGGINGS);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        nectricLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        nectricLeggings.addSource(ItemSource.MARKET);
        nectricLeggings.addSource(ItemSource.PATIENT_X);
        nectricLeggings.addUse("Set Effect: Necrotic Armor");
        itemConsumer.accept(nectricLeggings);

        CustomEquipment nectricBoots = new CustomEquipment(NECTRIC_BOOTS, Material.DIAMOND_BOOTS);
        nectricBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        nectricBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        nectricBoots.addSource(ItemSource.MARKET);
        nectricBoots.addSource(ItemSource.PATIENT_X);
        nectricBoots.addUse("Set Effect: Necrotic Armor");
        itemConsumer.accept(nectricBoots);

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
        necrosHelmet.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(necrosHelmet);

        CustomEquipment necrosChestplate = new CustomEquipment(NECROS_CHESTPLATE, Material.DIAMOND_CHESTPLATE);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        necrosChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        necrosChestplate.addSource(ItemSource.MARKET);
        necrosChestplate.addSource(ItemSource.PATIENT_X);
        necrosChestplate.addUse("Set Effect: Necrotic Armor");
        necrosChestplate.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(necrosChestplate);

        CustomEquipment necrosLeggings = new CustomEquipment(NECROS_LEGGINGS, Material.DIAMOND_LEGGINGS);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        necrosLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        necrosLeggings.addSource(ItemSource.MARKET);
        necrosLeggings.addSource(ItemSource.PATIENT_X);
        necrosLeggings.addUse("Set Effect: Necrotic Armor");
        necrosLeggings.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(necrosLeggings);

        CustomEquipment necrosBoots = new CustomEquipment(NECROS_BOOTS, Material.DIAMOND_BOOTS);
        necrosBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        necrosBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        necrosBoots.addSource(ItemSource.MARKET);
        necrosBoots.addSource(ItemSource.PATIENT_X);
        necrosBoots.addUse("Set Effect: Necrotic Armor");
        necrosBoots.addUse("Repaired when worn while attacking creatures or players");
        itemConsumer.accept(necrosBoots);

        // Master Weapons
        CustomWeapon masterSword = new CustomWeapon(MASTER_SWORD, Material.DIAMOND_SWORD, 3.5);
        masterSword.addSource(ItemSource.GIANT_BOSS);
        masterSword.addSource(ItemSource.MARKET);
        masterSword.addUse("Repairable at any Sacrificial Pit");
        masterSword.addUse("Conditional Effects");
        itemConsumer.accept(masterSword);

        CustomWeapon masterShortSword = new CustomWeapon(MASTER_SHORT_SWORD, Material.DIAMOND_SWORD, 2, 4);
        masterShortSword.addSource(ItemSource.GIANT_BOSS);
        masterShortSword.addSource(ItemSource.MARKET);
        masterShortSword.addUse("Repairable at any Sacrificial Pit");
        masterShortSword.addUse("Conditional Effects");
        itemConsumer.accept(masterShortSword);

        CustomWeapon masterBow = new CustomWeapon(MASTER_BOW, Material.BOW, 2);
        masterBow.addSource(ItemSource.GIANT_BOSS);
        masterBow.addSource(ItemSource.MARKET);
        masterBow.addUse("Repairable at any Sacrificial Pit");
        masterBow.addUse("Conditional Effects");
        itemConsumer.accept(masterBow);

        // Unleashed Weapons
        CustomWeapon unleashedSword = new CustomWeapon(UNLEASHED_SWORD, Material.DIAMOND_SWORD, 4);
        unleashedSword.addSource(ItemSource.GRAVE_YARD);
        unleashedSword.addSource(ItemSource.MARKET);
        unleashedSword.addUse("Repairable at any Sacrificial Pit, but requires 2 Imbued Crystals " +
            "for every 11% damage, or 1 Imbued Crystal if repaired inside of the Grave Yard rewards room.");
        unleashedSword.addUse("Global Effects");
        itemConsumer.accept(unleashedSword);

        CustomWeapon unleashedShortSword = new CustomWeapon(UNLEASHED_SHORT_SWORD, Material.DIAMOND_SWORD, 2.25, 4);
        unleashedShortSword.addSource(ItemSource.GRAVE_YARD);
        unleashedShortSword.addSource(ItemSource.MARKET);
        unleashedShortSword.addUse("Repairable at any Sacrificial Pit, but requires 2 Imbued Crystals " +
            "for every 11% damage, or 1 Imbued Crystal if repaired inside of the Grave Yard rewards room.");
        unleashedShortSword.addUse("Global Effects");
        itemConsumer.accept(unleashedShortSword);

        CustomWeapon unleashedBow = new CustomWeapon(UNLEASHED_BOW, Material.BOW, 2.25);
        unleashedBow.addSource(ItemSource.GRAVE_YARD);
        unleashedBow.addSource(ItemSource.MARKET);
        unleashedBow.addUse("Repairable at any Sacrificial Pit, but requires 2 Imbued Crystals " +
            "for every 11% damage, or 1 Imbued Crystal if repaired inside of the Grave Yard rewards room.");
        unleashedBow.addUse("Global Effects");
        itemConsumer.accept(unleashedBow);

        // Fear Weapons
        CustomWeapon fearSword = new CustomWeapon(FEAR_SWORD, Material.DIAMOND_SWORD, 4);
        fearSword.addSource(ItemSource.GRAVE_YARD);
        fearSword.addSource(ItemSource.MARKET);
        fearSword.addUse("Repairable at any Sacrificial Pit, but requires 2 Gems of Darkness " +
            "for every 11% damage, or 1 Gem of Darkness if repaired inside of the Grave Yard rewards room.");
        fearSword.addUse("Global Effects");
        itemConsumer.accept(fearSword);

        CustomWeapon fearShortSword = new CustomWeapon(FEAR_SHORT_SWORD, Material.DIAMOND_SWORD, 2.25, 4);
        fearShortSword.addSource(ItemSource.GRAVE_YARD);
        fearShortSword.addSource(ItemSource.MARKET);
        fearShortSword.addUse("Repairable at any Sacrificial Pit, but requires 2 Imbued Crystals " +
            "for every 11% damage, or 1 Imbued Crystal if repaired inside of the Grave Yard rewards room.");
        fearShortSword.addUse("Global Effects");
        itemConsumer.accept(fearShortSword);

        CustomWeapon fearBow = new CustomWeapon(FEAR_BOW, Material.BOW, 2.25);
        fearBow.addSource(ItemSource.GRAVE_YARD);
        fearBow.addSource(ItemSource.MARKET);
        fearBow.addUse("Repairable at any Sacrificial Pit, but requires 2 Gems of Darkness " +
            "for every 11% damage, or 1 Gem of Darkness if repaired inside of the Grave Yard rewards room.");
        fearBow.addUse("Global Effects");
        itemConsumer.accept(fearBow);

        // Shadow Items
        CustomWeapon shadowSword = new CustomWeapon(SHADOW_SWORD, Material.DIAMOND_SWORD, 5);
        shadowSword.addUse("Slows your opponent with every hit.");
        itemConsumer.accept(shadowSword);

        CustomWeapon shadowBow = new CustomWeapon(SHADOW_BOW, Material.BOW, 5);
        shadowBow.addUse("Slows your opponent with every hit.");
        itemConsumer.accept(shadowBow);

        // Red Items
        CustomWeapon redSword = new CustomWeapon(RED_SWORD, Material.DIAMOND_SWORD, 1.75);
        redSword.addSource(ItemSource.RITUAL_TOMB);
        redSword.addSource(ItemSource.MARKET);
        redSword.addUse("Global Effects.");
        itemConsumer.accept(redSword);

        CustomWeapon redBow = new CustomWeapon(RED_BOW, Material.BOW, 1.75);
        redBow.addSource(ItemSource.RITUAL_TOMB);
        redBow.addSource(ItemSource.MARKET);
        redBow.addUse("Global Effects.");
        itemConsumer.accept(redBow);

        CustomItem redFeather = new CustomItem(RED_FEATHER, Material.FEATHER);
        redFeather.addSource(ItemSource.RITUAL_TOMB);
        redFeather.addSource(ItemSource.MARKET);
        redFeather.addUse("Consumes redstone to prevent up to 100% damage, " +
            "but has a cool down based on the amount of damage taken.");
        itemConsumer.accept(redFeather);

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
        itemConsumer.accept(godHelmet);

        CustomEquipment godChestplate = new CustomEquipment(GOD_CHESTPLATE, Material.DIAMOND_CHESTPLATE);
        godChestplate.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        godChestplate.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        godChestplate.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        godChestplate.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        godChestplate.addSource(ItemSource.SACRIFICIAL_PIT);
        godChestplate.addSource(ItemSource.GRAVE_YARD);
        godChestplate.addSource(ItemSource.MARKET);
        itemConsumer.accept(godChestplate);

        CustomEquipment godLeggings = new CustomEquipment(GOD_LEGGINGS, Material.DIAMOND_LEGGINGS);
        godLeggings.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        godLeggings.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        godLeggings.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        godLeggings.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        godLeggings.addSource(ItemSource.SACRIFICIAL_PIT);
        godLeggings.addSource(ItemSource.GRAVE_YARD);
        godLeggings.addSource(ItemSource.MARKET);
        itemConsumer.accept(godLeggings);

        CustomEquipment godBoots = new CustomEquipment(GOD_BOOTS, Material.DIAMOND_BOOTS);
        godBoots.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_PROJECTILE, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_FIRE, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 4);
        godBoots.addEnchant(Enchantment.PROTECTION_FALL, 4);
        godBoots.addSource(ItemSource.SACRIFICIAL_PIT);
        godBoots.addSource(ItemSource.GRAVE_YARD);
        godBoots.addSource(ItemSource.MARKET);
        itemConsumer.accept(godBoots);

        // God Weapons
        CustomWeapon godSword = new CustomWeapon(GOD_SWORD, Material.DIAMOND_SWORD, 2.5);
        godSword.addSource(ItemSource.SACRIFICIAL_PIT);
        godSword.addSource(ItemSource.GRAVE_YARD);
        godSword.addSource(ItemSource.MARKET);
        itemConsumer.accept(godSword);

        CustomWeapon godShortSword = new CustomWeapon(GOD_SHORT_SWORD, Material.DIAMOND_SWORD, 1.5, 4);
        godShortSword.addSource(ItemSource.SACRIFICIAL_PIT);
        godShortSword.addSource(ItemSource.GRAVE_YARD);
        godShortSword.addSource(ItemSource.MARKET);
        itemConsumer.accept(godShortSword);

        CustomWeapon godBow = new CustomWeapon(GOD_BOW, Material.BOW, 1.5);
        godBow.addSource(ItemSource.SACRIFICIAL_PIT);
        godBow.addSource(ItemSource.GRAVE_YARD);
        godBow.addSource(ItemSource.MARKET);
        itemConsumer.accept(godBow);

        // God Tools
        CustomEquipment godAxe = new CustomEquipment(GOD_AXE, Material.DIAMOND_AXE);
        godAxe.addEnchant(Enchantment.DIG_SPEED, 4);
        godAxe.addSource(ItemSource.SACRIFICIAL_PIT);
        godAxe.addSource(ItemSource.GRAVE_YARD);
        godAxe.addSource(ItemSource.MARKET);
        itemConsumer.accept(godAxe);

        CustomEquipment legendaryGodAxe = new CustomEquipment(LEGENDARY_GOD_AXE, Material.DIAMOND_AXE);
        legendaryGodAxe.addEnchant(Enchantment.DAMAGE_ALL, 5);
        legendaryGodAxe.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 5);
        legendaryGodAxe.addEnchant(Enchantment.DAMAGE_UNDEAD, 5);
        legendaryGodAxe.addEnchant(Enchantment.DIG_SPEED, 5);
        legendaryGodAxe.addEnchant(Enchantment.DURABILITY, 3);
        legendaryGodAxe.addSource(ItemSource.SACRIFICIAL_PIT);
        legendaryGodAxe.addSource(ItemSource.GRAVE_YARD);
        legendaryGodAxe.addSource(ItemSource.MARKET);
        itemConsumer.accept(legendaryGodAxe);

        CustomEquipment godPickaxe = new CustomEquipment(GOD_PICKAXE, Material.DIAMOND_PICKAXE);
        godPickaxe.addEnchant(Enchantment.DIG_SPEED, 4);
        godPickaxe.addEnchant(Enchantment.SILK_TOUCH, 1);
        godPickaxe.addSource(ItemSource.SACRIFICIAL_PIT);
        godPickaxe.addSource(ItemSource.GRAVE_YARD);
        godPickaxe.addSource(ItemSource.MARKET);
        itemConsumer.accept(godPickaxe);

        CustomEquipment legendaryGodPickaxe = new CustomEquipment(LEGENDARY_GOD_PICKAXE, Material.DIAMOND_PICKAXE);
        legendaryGodPickaxe.addEnchant(Enchantment.DIG_SPEED, 5);
        legendaryGodPickaxe.addEnchant(Enchantment.DURABILITY, 3);
        legendaryGodPickaxe.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 3);
        legendaryGodPickaxe.addSource(ItemSource.SACRIFICIAL_PIT);
        legendaryGodPickaxe.addSource(ItemSource.GRAVE_YARD);
        legendaryGodPickaxe.addSource(ItemSource.MARKET);
        itemConsumer.accept(legendaryGodPickaxe);

        // Combat Potions
        CustomPotion divineCombatPotion = new CustomPotion(DIVINE_COMBAT_POTION, Color.BLACK);
        divineCombatPotion.addEffect(PotionEffectType.REGENERATION, TimeUtil.convertSecondsToTicks(30), 1);
        divineCombatPotion.addEffect(PotionEffectType.ABSORPTION, TimeUtil.convertMinutesToTicks(5), 2);
        divineCombatPotion.addEffect(PotionEffectType.INCREASE_DAMAGE, TimeUtil.convertMinutesToTicks(5), 1);
        divineCombatPotion.addEffect(PotionEffectType.DAMAGE_RESISTANCE, TimeUtil.convertMinutesToTicks(5), 1);
        divineCombatPotion.addEffect(PotionEffectType.SPEED, TimeUtil.convertMinutesToTicks(5), 1);
        divineCombatPotion.addSource(ItemSource.SACRIFICIAL_PIT);
        divineCombatPotion.addSource(ItemSource.GRAVE_YARD);
        divineCombatPotion.addSource(ItemSource.MARKET);
        itemConsumer.accept(divineCombatPotion);

        CustomPotion holyCombatPotion = new CustomPotion(HOLY_COMBAT_POTION, Color.BLACK);
        holyCombatPotion.addEffect(PotionEffectType.REGENERATION, TimeUtil.convertSecondsToTicks(30), 1);
        holyCombatPotion.addEffect(PotionEffectType.ABSORPTION, TimeUtil.convertMinutesToTicks(1), 2);
        holyCombatPotion.addEffect(PotionEffectType.INCREASE_DAMAGE, TimeUtil.convertMinutesToTicks(1), 1);
        holyCombatPotion.addEffect(PotionEffectType.DAMAGE_RESISTANCE, TimeUtil.convertMinutesToTicks(1), 1);
        holyCombatPotion.addEffect(PotionEffectType.SPEED, TimeUtil.convertMinutesToTicks(1), 1);
        holyCombatPotion.addSource(ItemSource.SACRIFICIAL_PIT);
        holyCombatPotion.addSource(ItemSource.GRAVE_YARD);
        holyCombatPotion.addSource(ItemSource.MARKET);
        itemConsumer.accept(holyCombatPotion);

        CustomPotion extremeCombatPotion = new CustomPotion(EXTREME_COMBAT_POTION, Color.BLACK);
        extremeCombatPotion.addEffect(PotionEffectType.ABSORPTION, TimeUtil.convertMinutesToTicks(5), 1);
        extremeCombatPotion.addEffect(PotionEffectType.INCREASE_DAMAGE, TimeUtil.convertMinutesToTicks(5), 0);
        extremeCombatPotion.addEffect(PotionEffectType.DAMAGE_RESISTANCE, TimeUtil.convertMinutesToTicks(5), 0);
        extremeCombatPotion.addEffect(PotionEffectType.SPEED, TimeUtil.convertMinutesToTicks(5), 0);
        extremeCombatPotion.addSource(ItemSource.SACRIFICIAL_PIT);
        extremeCombatPotion.addSource(ItemSource.GRAVE_YARD);
        extremeCombatPotion.addSource(ItemSource.MARKET);
        itemConsumer.accept(extremeCombatPotion);

        // Grave Yard Gems
        CustomItem gemOfLife = new CustomItem(GEM_OF_LIFE, Material.DIAMOND);
        gemOfLife.addSource(ItemSource.GRAVE_YARD);
        gemOfLife.addSource(ItemSource.MARKET);
        gemOfLife.addUse("Preserves your inventory when you die in the Grave Yard, " +
            "or when you die during a thunderstorm.");
        itemConsumer.accept(gemOfLife);

        CustomItem gemOfDarkness = new CustomItem(GEM_OF_DARKNESS, Material.EMERALD);
        gemOfDarkness.addSource(ItemSource.GRAVE_YARD);
        gemOfDarkness.addSource(ItemSource.MARKET);
        gemOfDarkness.addUse("Protects you from the Grave Yard's blindness effect.");
        gemOfDarkness.addUse("Used to repair Fear weapons.");
        itemConsumer.accept(gemOfDarkness);

        CustomItem imbuedCrystal = new CustomItem(IMBUED_CRYSTAL, Material.DIAMOND);
        imbuedCrystal.addSource(ItemSource.GRAVE_YARD);
        imbuedCrystal.addSource(ItemSource.MARKET);
        imbuedCrystal.addUse("Compacts gold nuggets into bars.");
        imbuedCrystal.addUse("Compacts gold bars into blocks.");
        imbuedCrystal.addUse("Used to repair Unleashed Weapons.");
        itemConsumer.accept(imbuedCrystal);

        // Peaceful Warrior
        CustomEquipment peacefulWarriorHelmet = new CustomEquipment(PEACEFUL_WARRIOR_HELMET, Material.IRON_HELMET);
        peacefulWarriorHelmet.addSource(ItemSource.APOCALYPSE);
        peacefulWarriorHelmet.addSource(ItemSource.SACRIFICIAL_PIT);
        peacefulWarriorHelmet.addSource(ItemSource.MARKET);
        peacefulWarriorHelmet.addUse("Set Effect: Way of The Peaceful Warrior");
        itemConsumer.accept(peacefulWarriorHelmet);

        CustomEquipment peacefulWarriorChestplate = new CustomEquipment(PEACEFUL_WARRIOR_CHESTPLATE, Material.IRON_CHESTPLATE);
        peacefulWarriorChestplate.addSource(ItemSource.APOCALYPSE);
        peacefulWarriorChestplate.addSource(ItemSource.SACRIFICIAL_PIT);
        peacefulWarriorChestplate.addSource(ItemSource.MARKET);
        peacefulWarriorChestplate.addUse("Set Effect: Way of The Peaceful Warrior");
        itemConsumer.accept(peacefulWarriorChestplate);

        CustomEquipment peacefulWarriorLeggings = new CustomEquipment(PEACEFUL_WARRIOR_LEGGINGS, Material.IRON_LEGGINGS);
        peacefulWarriorLeggings.addSource(ItemSource.APOCALYPSE);
        peacefulWarriorLeggings.addSource(ItemSource.SACRIFICIAL_PIT);
        peacefulWarriorLeggings.addSource(ItemSource.MARKET);
        peacefulWarriorLeggings.addUse("Set Effect: Way of The Peaceful Warrior");
        itemConsumer.accept(peacefulWarriorLeggings);

        CustomEquipment peacefulWarriorBoots = new CustomEquipment(PEACEFUL_WARRIOR_BOOTS, Material.IRON_BOOTS);
        peacefulWarriorBoots.addSource(ItemSource.APOCALYPSE);
        peacefulWarriorBoots.addSource(ItemSource.SACRIFICIAL_PIT);
        peacefulWarriorBoots.addSource(ItemSource.MARKET);
        peacefulWarriorBoots.addUse("Set Effect: Way of The Peaceful Warrior");
        itemConsumer.accept(peacefulWarriorBoots);

        // Phantom Items
        CustomItem phantomGold = new CustomItem(PHANTOM_GOLD, Material.GOLD_INGOT);
        phantomGold.addSource(ItemSource.GOLD_RUSH);
        phantomGold.addSource(ItemSource.GRAVE_YARD);
        phantomGold.addUse("When sacrificed gives 50 Skrin, or 100 Skrin " +
            "if sacrificed in the Grave Yard rewards room.");
        itemConsumer.accept(phantomGold);

        CustomItem phantomDiamond = new CustomItem(PHANTOM_DIAMOND, Material.DIAMOND);
        phantomDiamond.addSource(ItemSource.RITUAL_TOMB);
        phantomDiamond.addSource(ItemSource.FREAKY_FOUR);
        phantomDiamond.addUse("When sacrificed gives 15,000 Skrin, or 17,500 Skrin " +
            "if sacrificed in the Grave Yard rewards room.");
        itemConsumer.accept(phantomDiamond);

        CustomItem phantomSabre = new CustomWeapon(PHANTOM_SABRE, Material.NETHERITE_SWORD, 3, 4);
        phantomSabre.addSource(ItemSource.FREAKY_FOUR);
        phantomSabre.addUse("Gives its owner unbound fortune.");
        itemConsumer.accept(phantomSabre);

        CustomItem phantomClock = new CustomItem(PHANTOM_CLOCK, Material.CLOCK);
        phantomClock.addSource(ItemSource.GRAVE_YARD);
        phantomClock.addSource(ItemSource.FREAKY_FOUR);
        phantomClock.addUse("Teleports the player strait to the rewards room of the Grave Yard.");
        itemConsumer.accept(phantomClock);

        CustomItem phantomLink = new CustomItem(PHANTOM_LINK, Material.PHANTOM_MEMBRANE);
        phantomLink.addSource(ItemSource.FREAKY_FOUR);
        phantomLink.addUse("Allows remote access to a container once punched.");
        itemConsumer.accept(phantomLink);

        CustomItem phantomHymn = new CustomItem(PHANTOM_HYMN, Material.BOOK);
        phantomHymn.addSource(ItemSource.GOLD_RUSH);
        phantomHymn.addSource(ItemSource.FREAKY_FOUR);
        phantomHymn.addSource(ItemSource.MARKET);
        phantomHymn.addLore(ChatColor.RED + "A hymn of dark origins...");
        phantomHymn.addUse("Teleports the player through directly to the end of the Grave Yard maze.");
        phantomHymn.addUse("Teleports the player between rooms in the Freaky Four fight.");
        phantomHymn.addUse("At the cost of the item, teleports the player into Patient X's room.");
        itemConsumer.accept(phantomHymn);

        CustomPotion phantomPotion = new CustomPotion(PHANTOM_POTION, Color.RED);
        phantomPotion.addEffect(PotionEffectType.INVISIBILITY, 20 * 30, 0);
        phantomPotion.addSource(ItemSource.GRAVE_YARD);
        phantomPotion.addSource(ItemSource.FREAKY_FOUR);
        phantomPotion.addSource(ItemSource.MARKET);
        phantomPotion.addUse("Returns you to your lost items if a teleport can reach the location.");
        itemConsumer.accept(phantomPotion);

        CustomGiftItem newbiePhantomPotion = new CustomExpiringGift(
            NEWBIE_PHANTOM_POTION,
            phantomPotion,
            TimeUnit.MINUTES.toMillis(15)
        );
        itemConsumer.accept(newbiePhantomPotion);

        CustomItem phantomEssence = new CustomItem(PHANTOM_ESSENCE, Material.GHAST_TEAR);
        phantomEssence.addSource(ItemSource.GRAVE_YARD);
        phantomEssence.addSource(ItemSource.FREAKY_FOUR);
        phantomEssence.addUse("Currency for bargaining with spirits.");
        itemConsumer.accept(phantomEssence);

        // Linear Tools
        CustomItem linearAxe = new CustomItem(LINEAR_AXE, Material.DIAMOND_AXE);
        linearAxe.addTag(ChatColor.RED, "Distance", "3");
        linearAxe.addTag(ChatColor.RED, "Max Distance", "9");
        itemConsumer.accept(linearAxe);

        CustomItem linearPickaxe = new CustomItem(LINEAR_PICKAXE, Material.DIAMOND_PICKAXE);
        linearPickaxe.addTag(ChatColor.RED, "Distance", "3");
        linearPickaxe.addTag(ChatColor.RED, "Max Distance", "9");
        itemConsumer.accept(linearPickaxe);

        CustomItem linearShovel = new CustomItem(LINEAR_SHOVEL, Material.DIAMOND_SHOVEL);
        linearShovel.addTag(ChatColor.RED, "Distance", "3");
        linearShovel.addTag(ChatColor.RED, "Max Distance", "9");
        itemConsumer.accept(linearShovel);

        CustomItem linearBlockPlacer = new CustomItem(LINEAR_BLOCK_PLACER, Material.DIAMOND_HOE);
        linearBlockPlacer.addTag(ChatColor.RED, "Distance", "3");
        linearBlockPlacer.addTag(ChatColor.RED, "Max Distance", "9");
        itemConsumer.accept(linearBlockPlacer);

        // Radial Tools
        CustomItem radialAxe = new CustomItem(RADIAL_AXE, Material.DIAMOND_AXE);
        radialAxe.addTag(ChatColor.RED, "Radius", "1");
        radialAxe.addTag(ChatColor.RED, "Max Radius", "1");
        itemConsumer.accept(radialAxe);

        CustomItem radialPickaxe = new CustomItem(RADIAL_PICKAXE, Material.DIAMOND_PICKAXE);
        radialPickaxe.addTag(ChatColor.RED, "Radius", "1");
        radialPickaxe.addTag(ChatColor.RED, "Max Radius", "1");
        itemConsumer.accept(radialPickaxe);

        CustomItem radialShovel = new CustomItem(RADIAL_SHOVEL, Material.DIAMOND_SHOVEL);
        radialShovel.addTag(ChatColor.RED, "Radius", "1");
        radialShovel.addTag(ChatColor.RED, "Max Radius", "1");
        itemConsumer.accept(radialShovel);

        // Ninja Guild
        CustomItem ninjaStar = new CustomItem(NINJA_STAR, Material.NETHER_STAR);
        ninjaStar.addSource(ItemSource.NINJA_GUILD);
        ninjaStar.addUse("Teleports the player to the Ninja Guild.");
        itemConsumer.accept(ninjaStar);

        CustomItem ninjaOath = new CustomItem(NINJA_OATH, Material.PAPER);
        ninjaOath.addSource(ItemSource.NINJA_GUILD);
        ninjaOath.addUse("Allows entry into the Ninja Guild.");
        itemConsumer.accept(ninjaOath);

        // Rogue Guild
        CustomItem rogueOath = new CustomItem(ROGUE_OATH, Material.PAPER);
        rogueOath.addSource(ItemSource.NINJA_GUILD);
        rogueOath.addUse("Allows entry into the Rogue Guild.");
        itemConsumer.accept(rogueOath);

        // Magical Items
        CustomItem odeToTheFrozenKing = new CustomItem(ODE_TO_THE_FROZEN_KING, Material.PAPER);
        odeToTheFrozenKing.addSource(ItemSource.DROP_PARTY);
        odeToTheFrozenKing.addSource(ItemSource.PATIENT_X);
        odeToTheFrozenKing.addSource(ItemSource.MARKET);
        odeToTheFrozenKing.addUse("Provides entry to Frostborn's layer.");
        itemConsumer.accept(odeToTheFrozenKing);

        // Flight Items
        CustomItem pixieDust = new CustomItem(PIXIE_DUST, Material.SUGAR);
        pixieDust.addSource(ItemSource.SACRIFICIAL_PIT);
        pixieDust.addSource(ItemSource.GOLD_RUSH);
        pixieDust.addSource(ItemSource.MARKET);
        pixieDust.addUse("Allows the player to fly in permitted areas until " +
            "they have ran out of Pixie Dust items to consume.");
        itemConsumer.accept(pixieDust);

        CustomItem magicbucket = new CustomItem(MAGIC_BUCKET, Material.BUCKET);
        magicbucket.addTag(ChatColor.GREEN, "Speed", MagicBucketImpl.DEFAULT_SPEED.name());
        magicbucket.addSource(ItemSource.GIANT_BOSS);
        magicbucket.addSource(ItemSource.MARKET);
        magicbucket.addUse("Allows the player to fly indefinitely in permitted areas.");
        magicbucket.addUse("When used on a cow, it will turn into Mad Milk.");
        itemConsumer.accept(magicbucket);

        // Animal Bows
        CustomEquipment batBow = new CustomEquipment(BAT_BOW, Material.BOW);
        batBow.addSource(ItemSource.GRAVE_YARD);
        batBow.addSource(ItemSource.MARKET);
        batBow.addUse("Creates bats at the point where a fired arrow lands.");
        batBow.addUse("Creates a trail of bats following any fired arrow.");
        itemConsumer.accept(batBow);

        CustomEquipment chickenBow = new CustomEquipment(CHICKEN_BOW, Material.BOW);
        chickenBow.addUse("Creates chickens at the point where a fired arrow lands.");
        chickenBow.addUse("Creates a trail of chickens following any fired arrow.");
        itemConsumer.accept(chickenBow);

        CustomItem chickenHymn = new CustomItem(CHICKEN_HYMN, Material.BOOK);
        chickenHymn.addLore(ChatColor.BLUE + "Cluck cluck!");
        chickenHymn.addUse("Turns nearby items into chickens, and nearby chickens into cooked chicken.");
        itemConsumer.accept(chickenHymn);

        CustomItem godFish = new CustomItem(GOD_FISH, Material.COD);
        godFish.addSource(ItemSource.FREAKY_FOUR);
        godFish.addSource(ItemSource.SACRIFICIAL_PIT);
        godFish.addSource(ItemSource.MARKET);
        godFish.addUse("On consumption applies 30 seconds of the Hulk prayer.");
        itemConsumer.accept(godFish);

        CustomItem overSeerBow = new CustomItem(OVERSEER_BOW, Material.BOW);
        overSeerBow.addEnchant(Enchantment.ARROW_DAMAGE, 2);
        overSeerBow.addEnchant(Enchantment.ARROW_FIRE, 1);
        overSeerBow.addSource(ItemSource.SACRIFICIAL_PIT);
        overSeerBow.addSource(ItemSource.MARKET);
        itemConsumer.accept(overSeerBow);

        CustomItem barbarianBones = new CustomItem(BARBARIAN_BONE, Material.BONE);
        barbarianBones.addSource(ItemSource.GIANT_BOSS);
        barbarianBones.addSource(ItemSource.GRAVE_YARD);
        barbarianBones.addUse("Improves the drops of the Giant Boss if in a suitable quantity.");
        barbarianBones.addUse("Sacrifice to store world levels.");
        itemConsumer.accept(barbarianBones);

        CustomPotion potionOfRestitution = new CustomPotion(POTION_OF_RESTITUTION, Color.GREEN);
        potionOfRestitution.addEffect(PotionEffectType.POISON, 20 * 10, 1);
        potionOfRestitution.addSource(ItemSource.MARKET);
        potionOfRestitution.addUse("Returns you to your last death point if a teleport can reach the location.");
        itemConsumer.accept(potionOfRestitution);

        CustomItem scrollOfSummation = new CustomItem(SCROLL_OF_SUMMATION, Material.PAPER);
        scrollOfSummation.addSource(ItemSource.DROP_PARTY);
        scrollOfSummation.addSource(ItemSource.PATIENT_X);
        scrollOfSummation.addSource(ItemSource.MARKET);
        scrollOfSummation.addUse("At the cost of the item, will compact coal, iron, gold, redstone, lapis, diamonds, and emerald.");
        itemConsumer.accept(scrollOfSummation);

        CustomItem calmingCrystal = new CustomItem(CALMING_CRYSTAL, Material.DIAMOND);
        calmingCrystal.addSource(ItemSource.PATIENT_X);
        calmingCrystal.addSource(ItemSource.MARKET);
        calmingCrystal.addUse("When present in the inventory, provides calming effects.");
        itemConsumer.accept(calmingCrystal);

        CustomItem therapistNotes = new CustomItem(PATIENT_X_THERAPY_NOTES, Material.PAPER);
        therapistNotes.addSource(ItemSource.PATIENT_X);
        therapistNotes.addSource(ItemSource.MARKET);
        therapistNotes.addUse("Upon use, calms Patient X using information provided by his therapist.");
        itemConsumer.accept(therapistNotes);

        CustomItem hymnOfSummation = new CustomItem(HYMN_OF_SUMMATION, Material.BOOK);
        hymnOfSummation.addSource(ItemSource.FROSTBORN);
        hymnOfSummation.addSource(ItemSource.MARKET);
        hymnOfSummation.addUse("Upon use, will compact coal, iron, gold, redstone, lapis, diamonds, and emerald.");
        itemConsumer.accept(hymnOfSummation);

        CustomItem tomeOfShadows = new CustomItem(HYMN_OF_HARVEST, Material.BOOK);
        tomeOfShadows.addSource(ItemSource.FROSTBORN);
        tomeOfShadows.addSource(ItemSource.MARKET);
        tomeOfShadows.addUse("Upon use will immediately grow a crop.");
        itemConsumer.accept(tomeOfShadows);

        // Party Box
        CustomItem whitePartyBox = new CustomItem(WHITE_PARTY_BOX, Material.WHITE_SHULKER_BOX);
        itemConsumer.accept(whitePartyBox);

        CustomItem orangePartyBox = new CustomItem(ORANGE_PARTY_BOX, Material.ORANGE_SHULKER_BOX);
        itemConsumer.accept(orangePartyBox);

        CustomItem magentaPartyBox = new CustomItem(MAGENTA_PARTY_BOX, Material.MAGENTA_SHULKER_BOX);
        itemConsumer.accept(magentaPartyBox);

        CustomItem lightBluePartyBox = new CustomItem(LIGHT_BLUE_PARTY_BOX, Material.LIGHT_BLUE_SHULKER_BOX);
        itemConsumer.accept(lightBluePartyBox);

        CustomItem yellowPartyBox = new CustomItem(YELLOW_PARTY_BOX, Material.YELLOW_SHULKER_BOX);
        itemConsumer.accept(yellowPartyBox);

        CustomItem limePartyBox = new CustomItem(LIME_PARTY_BOX, Material.LIME_SHULKER_BOX);
        itemConsumer.accept(limePartyBox);

        CustomItem pinkPartyBox = new CustomItem(PINK_PARTY_BOX, Material.PINK_SHULKER_BOX);
        itemConsumer.accept(pinkPartyBox);

        CustomItem grayPartyBox = new CustomItem(GRAY_PARTY_BOX, Material.GRAY_SHULKER_BOX);
        itemConsumer.accept(grayPartyBox);

        CustomItem lightGrayPartyBox = new CustomItem(LIGHT_GRAY_PARTY_BOX, Material.LIGHT_GRAY_SHULKER_BOX);
        itemConsumer.accept(lightGrayPartyBox);

        CustomItem cyanPartyBox = new CustomItem(CYAN_PARTY_BOX, Material.CYAN_SHULKER_BOX);
        itemConsumer.accept(cyanPartyBox);

        CustomItem purplePartyBox = new CustomItem(PURPLE_PARTY_BOX, Material.PURPLE_SHULKER_BOX);
        itemConsumer.accept(purplePartyBox);

        CustomItem bluePartyBox = new CustomItem(BLUE_PARTY_BOX, Material.BLUE_SHULKER_BOX);
        itemConsumer.accept(bluePartyBox);

        CustomItem brownPartyBox = new CustomItem(BROWN_PARTY_BOX, Material.BROWN_SHULKER_BOX);
        itemConsumer.accept(brownPartyBox);

        CustomItem greenPartyBox = new CustomItem(GREEN_PARTY_BOX, Material.GREEN_SHULKER_BOX);
        itemConsumer.accept(greenPartyBox);

        CustomItem redPartyBox = new CustomItem(RED_PARTY_BOX, Material.RED_SHULKER_BOX);
        itemConsumer.accept(redPartyBox);

        CustomItem blackPartyBox = new CustomItem(BLACK_PARTY_BOX, Material.BLACK_SHULKER_BOX);
        itemConsumer.accept(blackPartyBox);

        // Miscellaneous
        CustomItem executionerAxe = new CustomItem(EXECUTIONER_AXE, Material.GOLDEN_AXE);
        executionerAxe.addSource(ItemSource.APOCALYPSE);
        executionerAxe.addSource(ItemSource.MARKET);
        executionerAxe.addUse("Deals damage based on the number of zombies around.");
        itemConsumer.accept(executionerAxe);

        CustomItem demonicAshes = new CustomItem(DEMONIC_ASHES, Material.BLAZE_POWDER);
        demonicAshes.addSource(ItemSource.SACRIFICIAL_EXCHANGE);
        demonicAshes.addSource(ItemSource.RANGED_WORLD_MINING);
        demonicAshes.addUse("Consumable to increase world level.");
        itemConsumer.accept(demonicAshes);

        CustomItem madMilk = new CustomItem(MAD_MILK, Material.MILK_BUCKET);
        madMilk.addSource(ItemSource.MARKET);
        madMilk.addUse("If thrown into a brewing vat at the factory, a melt down will occur in which all undead creatures die.");
        madMilk.addUse("When drank, it will turn into a Magic Bucket.");
        itemConsumer.accept(madMilk);

        CustomItem tomeOfTheRiftSplitter = new CustomItem(TOME_OF_THE_RIFT_SPLITTER, Material.BOOK);
        tomeOfTheRiftSplitter.addSource(ItemSource.FROSTBORN);
        tomeOfTheRiftSplitter.addSource(ItemSource.APOCALYPSE);
        tomeOfTheRiftSplitter.addSource(ItemSource.MARKET);
        tomeOfTheRiftSplitter.addUse("Consumable to create a new warp.");
        itemConsumer.accept(tomeOfTheRiftSplitter);

        CustomItem tomeOfCursedSmelting = new CustomItem(TOME_OF_CURSED_SMELTING, Material.BOOK);
        tomeOfCursedSmelting.addSource(ItemSource.CURSED_MINE);
        tomeOfCursedSmelting.addSource(ItemSource.MARKET);
        tomeOfCursedSmelting.addUse("Consumable to permanently improve smelting results at the factory.");
        itemConsumer.accept(tomeOfCursedSmelting);

        CustomItem tomeOfPoison = new CustomItem(TOME_OF_POISON, Material.BOOK);
        tomeOfPoison.addSource(ItemSource.FROSTBORN);
        tomeOfPoison.addSource(ItemSource.MARKET);
        tomeOfPoison.addUse("Consumable to permanently add a chance that poison becomes regeneration.");
        itemConsumer.accept(tomeOfPoison);

        CustomItem tomeOfTheCleanly = new CustomItem(TOME_OF_THE_CLEANLY, Material.BOOK);
        tomeOfTheCleanly.addSource(ItemSource.FROSTBORN);
        tomeOfTheCleanly.addSource(ItemSource.MARKET);
        tomeOfTheCleanly.addUse("Consumable to permanently remove junk from the sacrificial pit.");
        itemConsumer.accept(tomeOfTheCleanly);

        CustomItem tomeOfSacrifice = new CustomItem(TOME_OF_SACRIFICE, Material.BOOK);
        tomeOfSacrifice.addSource(ItemSource.FROSTBORN);
        tomeOfSacrifice.addSource(ItemSource.MARKET);
        tomeOfSacrifice.addUse("Consumable to permanently increase reward value from the sacrificial pit.");
        itemConsumer.accept(tomeOfSacrifice);

        CustomItem tomeOfDivinity = new CustomItem(TOME_OF_DIVINITY, Material.BOOK);
        tomeOfDivinity.addSource(ItemSource.FROSTBORN);
        tomeOfDivinity.addSource(ItemSource.MARKET);
        tomeOfDivinity.addUse("Consumable to permanently unlock the ability to use prayers on yourself.");
        tomeOfDivinity.addUse("Consumable to permanently unlock divine favours.");
        itemConsumer.accept(tomeOfDivinity);

        CustomItem tomeOfTheUndead = new CustomItem(TOME_OF_THE_UNDEAD, Material.BOOK);
        tomeOfTheUndead.addSource(ItemSource.FROSTBORN);
        tomeOfTheUndead.addSource(ItemSource.MARKET);
        tomeOfTheUndead.addUse("Consumable to permanently protect yourself from targeted apocalyptic spawns.");
        itemConsumer.accept(tomeOfTheUndead);

        CustomItem tomeOfLegends = new CustomItem(TOME_OF_LEGENDS, Material.BOOK);
        tomeOfLegends.addSource(ItemSource.FROSTBORN);
        tomeOfLegends.addSource(ItemSource.MARKET);
        tomeOfLegends.addUse("Consumable to permanently reduce special attack cooldowns by 10%.");
        itemConsumer.accept(tomeOfLegends);

        CustomItem tomeOfLife = new CustomItem(TOME_OF_LIFE, Material.BOOK);
        tomeOfLife.addSource(ItemSource.FROSTBORN);
        tomeOfLife.addSource(ItemSource.MARKET);
        tomeOfLife.addUse("Consumable to permanently protect items on death.");
        itemConsumer.accept(tomeOfLife);
    }
}
