/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena.factory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.logging.Logger;

public class FactoryBrewer extends FactoryMech {

    private static int count = 0;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    public FactoryBrewer(World world, ProtectedRegion region, YAMLProcessor processor) {
        super(world, region, processor, "pot-ingredients-" + count++);
    }

    private static final Set<Material> wanted = Set.of(
        Material.GLASS_BOTTLE,
        Material.NETHER_WART,

        Material.GLOWSTONE_DUST,
        Material.REDSTONE,
        Material.GUNPOWDER,

        Material.MAGMA_CREAM,
        Material.SUGAR,
        Material.GLISTERING_MELON_SLICE,
        Material.SPIDER_EYE,
        Material.GHAST_TEAR,
        Material.BLAZE_POWDER,
        Material.FERMENTED_SPIDER_EYE,
        Material.GOLDEN_CARROT,
        Material.PUFFERFISH,

        // Special - Non-Vanilla
        Material.QUARTZ
    );

    @Override
    public List<ItemStack> process() {

        Collection<Player> playerList = getContained(1, Player.class);

        Collection<Entity> contained = getContained(Entity.class);
        if (!contained.isEmpty()) {
            ChatUtil.sendNotice(playerList, "Processing...");

            for (Entity e : contained) {

                // Kill contained living entities
                if (e instanceof LivingEntity) {
                    ((LivingEntity) e).setHealth(0);
                    continue;
                }

                // Find items and destroy those unwanted
                if (e instanceof Item) {

                    ItemStack workingStack = ((Item) e).getItemStack();

                    // Add the item to the list
                    if (wanted.contains(workingStack.getType())) {
                        int total = workingStack.getAmount();
                        ChatUtil.sendNotice(playerList, "Found: " + total + " " + workingStack.getType().toString() + ".");
                        if (items.containsKey(workingStack.getType())) {
                            total += items.get(workingStack.getType());
                        }
                        items.put(workingStack.getType(), total);
                    } else if (ItemUtil.isItem(workingStack, CustomItems.MAD_MILK)) {
                        FactoryFloor.factInst.madMilk();
                        ChatUtil.sendWarning(playerList, "The milk is too much for the vat to handle, strange things start happening...");
                    }
                }
                e.remove();
            }
            save(); // Update save for new Potion resource values
        }

        // Check these to avoid doing more calculations than need be
        int bottles = items.getOrDefault(Material.GLASS_BOTTLE, 0);
        int max = items.getOrDefault(Material.NETHER_WART, 0);
        if (bottles <= 0 || max <= 0) return new ArrayList<>();

        // Figure out the potion the player is trying to make
        List<Material> using = new ArrayList<>();
        PotionType target;
        if (items.containsKey(Material.MAGMA_CREAM)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.SLOWNESS;
                using.add(Material.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionType.FIRE_RESISTANCE;
            }
            using.add(Material.MAGMA_CREAM);
        } else if (items.containsKey(Material.SUGAR)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.SLOWNESS;
                using.add(Material.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionType.SPEED;
            }
            using.add(Material.SUGAR);
        } else if (items.containsKey(Material.GLISTERING_MELON_SLICE)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.INSTANT_DAMAGE;
                using.add(Material.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionType.INSTANT_HEAL;
            }
            using.add(Material.GLISTERING_MELON_SLICE);
        } else if (items.containsKey(Material.SPIDER_EYE)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.INSTANT_DAMAGE;
                using.add(Material.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionType.POISON;
            }
            using.add(Material.SPIDER_EYE);
        } else if (items.containsKey(Material.GHAST_TEAR)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.WEAKNESS;
                using.add(Material.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionType.REGEN;
            }
            using.add(Material.GHAST_TEAR);
        } else if (items.containsKey(Material.BLAZE_POWDER)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.WEAKNESS;
                using.add(Material.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionType.STRENGTH;
            }
            using.add(Material.BLAZE_POWDER);
        } else if (items.containsKey(Material.GOLDEN_CARROT)) {
            target = PotionType.NIGHT_VISION;
            using.add(Material.GOLDEN_CARROT);
        } else if (items.containsKey(Material.PUFFERFISH)) {
            target = PotionType.WATER_BREATHING;
            using.add(Material.PUFFERFISH);
        } else return new ArrayList<>();

        // Always used
        using.add(Material.GLASS_BOTTLE);
        using.add(Material.NETHER_WART);

        // Setup some important information
        boolean duration, potency, splash, increased;

        duration = items.containsKey(Material.REDSTONE);
        potency = items.containsKey(Material.GLOWSTONE_DUST);
        splash = items.containsKey(Material.GUNPOWDER);
        increased = items.containsKey(Material.QUARTZ);

        // Adapt as needed based on the information above
        if (duration && !target.isInstant()) {
            using.add(Material.REDSTONE);
        } else if (potency) {
            using.add(Material.GLOWSTONE_DUST);
        }

        if (splash) {
            using.add(Material.GUNPOWDER);
        }
        if (increased) {
            using.add(Material.QUARTZ);
        }

        // Find the max amount skipping glass bottles (too be checked later)
        for (Material used : using) {
            if (used == Material.GLASS_BOTTLE) continue;
            max = Math.min(max, items.get(used));
        }

        // This is confusing, essentially we are dividing the bottle count into three pieces
        // That allows us to figure out how many potion sets can be made
        // We will later expand the potion sets again
        max = (int) Math.min(max, Math.floor(bottles / 3));

        if (max <= 0) return new ArrayList<>();

        // Remove the used ingredients from the system
        int newAmt;
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            if (using.contains(entry.getKey())) {
                newAmt = entry.getValue() - (entry.getKey() == Material.GLASS_BOTTLE ? max * 3 : max);
                if (newAmt > 0) items.put(entry.getKey(), newAmt);
                else items.remove(entry.getKey());
            }
        }
        save(); // Update save for new Potion resource values

        // Inflate potion quantity
        max *= (increased ? 6 : 5);
        if (ModifierComponent.getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
            max *= 3;
        }

        // Calculate damage
        boolean upgraded = !duration && potency;
        ItemStack potion = makePotion(target, splash, upgraded, duration);

        // Tell the player what we are making
        ChatUtil.sendNotice(playerList, "Brewing: " + max + " " + target.getEffectType().getName() + " "
                + (upgraded ? "II" : "I") + (splash ? " splash" : "") + " potions.");
        // Return the product for the que
        List<ItemStack> product = new ArrayList<>();
        for (int i = 0; i < max; i++) product.add(potion.clone());
        return product;
    }

    /**
     * Creates a ItemStack for the potion
     *
     * @return The new Item Stack
     */
    public ItemStack makePotion(PotionType type, boolean splash, boolean upgraded, boolean extended) {
        ItemStack potion = new ItemStack(splash ? Material.POTION : Material.SPLASH_POTION);

        PotionMeta pMeta = (PotionMeta) potion.getItemMeta();
        pMeta.setBasePotionData(new PotionData(type, extended, upgraded));
        potion.setItemMeta(pMeta);

        return potion;
    }
}
