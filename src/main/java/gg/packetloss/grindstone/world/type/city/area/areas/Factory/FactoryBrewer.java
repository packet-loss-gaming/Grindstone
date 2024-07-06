/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.functional.TriConsumer;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FactoryBrewer implements FactoryMachine {
    private final FactoryArea parent;
    private final ProtectedRegion vatRegion;

    public FactoryBrewer(FactoryArea parent, ProtectedRegion vatRegion) {
        this.parent = parent;
        this.vatRegion = vatRegion;
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

    private void killLivingEntitiesInVat() {
        parent.getContained(vatRegion, LivingEntity.class).forEach((entity) -> entity.setHealth(0));
    }

    private Map<UUID, Map<Material, Integer>> getWantedItemMap() {
        Map<UUID, Map<Material, Integer>> items = new HashMap<>();
        for (Item e : parent.getContained(vatRegion, Item.class)) {
            UUID thrower = e.getThrower();
            if (thrower == null) {
                continue;
            }

            ItemStack workingStack = e.getItemStack();
            Material itemType = workingStack.getType();

            if (wanted.contains(itemType)) {
                Map<Material, Integer> playerItems = items.computeIfAbsent(thrower, k -> new HashMap<>());
                playerItems.merge(itemType, workingStack.getAmount(), Integer::sum);
            }
        }
        return items;
    }

    private Map<Material, List<Item>> getItemsToModify(UUID owner) {
        Map<Material, List<Item>> items = new HashMap<>();
        for (Item item : parent.getContained(vatRegion, Item.class)) {
            if (!owner.equals(item.getThrower())) {
                continue;
            }

            Material itemType = item.getItemStack().getType();
            if (wanted.contains(itemType)) {
                items.merge(itemType, List.of(item), (a, b) -> {
                    return Stream.of(a, b).flatMap(Collection::stream).collect(Collectors.toList());
                });
            }
        }
        return items;
    }

    private void detectNewJobsForPlayer(UUID playerID, Map<Material, Integer> items,
                                        TriConsumer<UUID, String, Integer> jobDeclarationConsumer) {
        if (items.isEmpty()) {
            return;
        }

        // Check these to avoid doing more calculations than need be
        int bottles = items.getOrDefault(Material.GLASS_BOTTLE, 0);
        int max = items.getOrDefault(Material.NETHER_WART, 0);
        if (bottles <= 0 || max <= 0) {
            return;
        }

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
                target = PotionType.SWIFTNESS;
            }
            using.add(Material.SUGAR);
        } else if (items.containsKey(Material.GLISTERING_MELON_SLICE)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.HARMING;
                using.add(Material.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionType.HEALING;
            }
            using.add(Material.GLISTERING_MELON_SLICE);
        } else if (items.containsKey(Material.SPIDER_EYE)) {
            if (items.containsKey(Material.FERMENTED_SPIDER_EYE)) {
                target = PotionType.HARMING;
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
                target = PotionType.REGENERATION;
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
        } else {
            return;
        }

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

        // Find the max amount skipping glass bottles (to be checked later)
        for (Material used : using) {
            if (used == Material.GLASS_BOTTLE) continue;
            max = Math.min(max, items.get(used));
        }

        // This is confusing, essentially we are dividing the bottle count into three pieces
        // That allows us to figure out how many potion sets can be made
        // We will later expand the potion sets again
        max = Math.min(max, bottles / 3);

        if (max <= 0) {
            return;
        }

        // Remove the used ingredients from the system
        Map<Material, List<Item>> itemsToModify = getItemsToModify(playerID);
        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            Material itemType = entry.getKey();
            if (!using.contains(itemType)) {
                continue;
            }

            int amountToRemove = itemType == Material.GLASS_BOTTLE ? max * 3 : max;
            for (Item item : itemsToModify.get(itemType)) {
                if (amountToRemove == 0) {
                    break;
                }

                ItemStack itemStack = item.getItemStack();

                // Figure out how much we're moving
                int amountRemoved = Math.min(itemStack.getAmount(), amountToRemove);
                int amountRemaining = itemStack.getAmount() - amountRemoved;

                // Update amount to remove
                amountToRemove -= amountRemoved;

                // Update the itemstack
                if (amountRemaining == 0) {
                    item.remove();
                } else {
                    // The API under the hood does cloning on this anyways, but since it's not clear
                    // about this, explicitly copy, update the amount, and reassign for stability.
                    itemStack = itemStack.clone();
                    itemStack.setAmount(amountRemaining);
                    item.setItemStack(itemStack);
                }
            }
        }

        // Inflate to the correct number of potions.
        max *= 3;

        // Inflate potion quantity
        int modifier = (increased ? 3 : 2);
        if (ModifierComponent.getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
            modifier *= 3;
        }

        // Calculate damage
        boolean upgraded = !duration && potency;
        ItemStack potionStack = makePotion(target, splash, upgraded, duration);
        String itemName = ItemNameCalculator.computeItemName(potionStack).orElseThrow();

        // Tell the player what we are making
        Collection<Player> playerList = parent.getAudiblePlayers();
        parent.sendProductionMessage(playerList, "Brewing", max, itemName, modifier);

        jobDeclarationConsumer.accept(playerID, itemName, max * modifier);
    }

    @Override
    public void detectNewJobs(TriConsumer<UUID, String, Integer> jobDeclarationConsumer) {
        killLivingEntitiesInVat();

        Map<UUID, Map<Material, Integer>> items = getWantedItemMap();
        for (Map.Entry<UUID, Map<Material, Integer>> playerItemMapping : items.entrySet()) {
            detectNewJobsForPlayer(playerItemMapping.getKey(), playerItemMapping.getValue(), jobDeclarationConsumer);
        }
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
