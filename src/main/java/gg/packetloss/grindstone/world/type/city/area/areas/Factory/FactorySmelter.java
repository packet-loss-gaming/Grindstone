/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.functional.TriConsumer;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FactorySmelter implements FactoryMachine {
    private final FactoryArea parent;
    private final ProtectedRegion oreInputRegion;

    public FactorySmelter(FactoryArea parent, ProtectedRegion oreInputRegion) {
        this.parent = parent;
        this.oreInputRegion = oreInputRegion;
    }

    private static record Conversion(Material newType, int multiplier) { };

    private static final Map<Material, Conversion> CONVERSION_MAPPING = Map.of(
        Material.IRON_ORE, new Conversion(Material.RAW_IRON, 1),
        Material.COPPER_ORE, new Conversion(Material.RAW_COPPER, 3),
        Material.GOLD_ORE, new Conversion(Material.RAW_GOLD, 1),
        Material.RAW_IRON_BLOCK, new Conversion(Material.RAW_IRON, 9),
        Material.RAW_COPPER_BLOCK, new Conversion(Material.RAW_COPPER, 9),
        Material.RAW_GOLD_BLOCK, new Conversion(Material.RAW_GOLD, 9)
    );

    private static Map<Material, Material> SMELTING_CONVERSIONS = Map.of(
        Material.RAW_IRON, Material.IRON_INGOT,
        Material.RAW_COPPER, Material.COPPER_INGOT,
        Material.RAW_GOLD, Material.GOLD_INGOT
    );

    private static final Set<Material> WANTED = new HashSet<>();

    static {
        for (Map.Entry<Material, Conversion> entry : CONVERSION_MAPPING.entrySet()) {
            WANTED.add(entry.getKey());
            WANTED.add(entry.getValue().newType());
        }
    }

    private int getInputModifier(Player thrower) {
        if (thrower != null && thrower.hasPermission("aurora.tome.cursedsmelting")) {
            return ChanceUtil.getRandom(3);
        }
        return 1;
    }

    private void sendItemDiscoveryMessage(Collection<Player> playerList, int quantity, String itemName, int modifier) {
        parent.sendProductionMessage(playerList, "Found", quantity, itemName, modifier);
    }

    private void processWantedItem(Player thrower, Collection<Player> audible,
                                   ItemStack stack, Map<Material, Integer> items) {
        Material type = stack.getType();
        int quantity = stack.getAmount();

        Conversion conversion = CONVERSION_MAPPING.get(type);
        if (conversion != null) {
            type = conversion.newType;
            quantity *= conversion.multiplier;
        }

        int modifier = getInputModifier(thrower);
        sendItemDiscoveryMessage(audible, quantity, ItemNameCalculator.computeItemName(stack).orElseThrow(), modifier);

        items.merge(type, quantity * modifier, Integer::sum);
    }

    private Map<UUID, Map<Material, Integer>> getWantedItemMap(Collection<Player> audiblePlayers) {
        Map<UUID, Map<Material, Integer>> items = new HashMap<>();
        for (Item e : parent.getContained(oreInputRegion, Item.class)) {
            UUID thrower = e.getThrower();
            if (thrower == null) {
                continue;
            }

            Player throwingPlayer = Bukkit.getPlayer(thrower);
            if (throwingPlayer == null) {
                continue;
            }

            ItemStack workingStack = e.getItemStack();
            Material itemType = workingStack.getType();

            if (WANTED.contains(itemType)) {
                Map<Material, Integer> playerItems = items.computeIfAbsent(thrower, k -> new HashMap<>());
                processWantedItem(throwingPlayer, audiblePlayers, workingStack, playerItems);
            }

            e.remove();
        }
        return items;
    }

    private int getSmeltingModifier() {
        int modifier = 4;

        if (ModifierComponent.getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
            modifier *= 3;
        }
        return modifier;
    }

    private void sendSmeltingNotice(Collection<Player> audiblePlayers, int quantity, String itemName, int modifier) {
        parent.sendProductionMessage(audiblePlayers, "Smelting", quantity, itemName, modifier);
    }

    private void detectNewJobsForPlayer(Collection<Player> audiblePlayers, UUID playerID,
                                        Material itemType, int maxQuantity,
                                        TriConsumer<UUID, String, Integer> jobDeclarationConsumer) {
        if (maxQuantity < 1) {
            return;
        }

        // Figure out what we're making and how much
        Material producedItemType = SMELTING_CONVERSIONS.get(itemType);
        String itemName = ItemNameCalculator.computeItemName(new ItemStack(producedItemType)).orElseThrow();
        int smeltingModifier = getSmeltingModifier();

        // Tell the player what we are making
        sendSmeltingNotice(audiblePlayers, maxQuantity, itemName, smeltingModifier);

        jobDeclarationConsumer.accept(playerID, itemName, maxQuantity * smeltingModifier);
    }

    private void detectNewJobsForPlayer(Collection<Player> audiblePlayers, UUID playerID, Map<Material, Integer> items,
                                        TriConsumer<UUID, String, Integer> jobDeclarationConsumer) {
        for (Map.Entry<Material, Integer> itemEntry : items.entrySet()) {
            detectNewJobsForPlayer(
                audiblePlayers,
                playerID,
                itemEntry.getKey(),
                itemEntry.getValue(),
                jobDeclarationConsumer
            );
        }
    }

    @Override
    public void detectNewJobs(TriConsumer<UUID, String, Integer> jobDeclarationConsumer) {
        Collection<Player> audiblePlayers = parent.getAudiblePlayers();
        Map<UUID, Map<Material, Integer>> items = getWantedItemMap(audiblePlayers);
        for (Map.Entry<UUID, Map<Material, Integer>> playerItemMapping : items.entrySet()) {
            detectNewJobsForPlayer(
                audiblePlayers,
                playerItemMapping.getKey(),
                playerItemMapping.getValue(),
                jobDeclarationConsumer
            );
        }
    }
}
