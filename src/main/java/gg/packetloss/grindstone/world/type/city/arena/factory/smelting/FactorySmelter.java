/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena.factory.smelting;

import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import gg.packetloss.grindstone.world.type.city.arena.factory.FactoryMech;
import gg.packetloss.grindstone.world.type.city.arena.factory.LavaSupply;
import gg.packetloss.grindstone.world.type.city.arena.factory.SmeltableResource;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class FactorySmelter extends FactoryMech {

    private static int count = 0;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private LavaSupply lavaSupply;

    private Map<Material, Integer> items = new HashMap<>();
    private SingleFileFilesystemStateHelper<Map<Material, Integer>> stateHelper;

    private static final List<SmeltableResource> SMELTABLE_RESOURCES = List.of(
        // IRON
        new SmeltableResource(Material.IRON_ORE, Material.IRON_INGOT, 8),
        new SmeltableResource(Material.IRON_INGOT, Material.IRON_INGOT, 1),
        new SmeltableResource(Material.IRON_HELMET, Material.IRON_INGOT, 5),
        new SmeltableResource(Material.IRON_CHESTPLATE, Material.IRON_INGOT, 8),
        new SmeltableResource(Material.IRON_LEGGINGS, Material.IRON_INGOT, 7),
        new SmeltableResource(Material.IRON_BOOTS, Material.IRON_INGOT, 4),
        new SmeltableResource(Material.IRON_AXE, Material.IRON_INGOT, 3),
        new SmeltableResource(Material.IRON_PICKAXE, Material.IRON_INGOT, 3),
        new SmeltableResource(Material.IRON_SWORD, Material.IRON_INGOT, 2),
        new SmeltableResource(Material.IRON_HOE, Material.IRON_INGOT, 2),
        new SmeltableResource(Material.IRON_SHOVEL, Material.IRON_INGOT, 1),
        new SmeltableResource(Material.SHEARS, Material.IRON_INGOT, 2),
        new SmeltableResource(Material.COMPASS, Material.IRON_INGOT, 4),

        // GOLD
        new SmeltableResource(Material.GOLD_ORE, Material.GOLD_INGOT, 8),
        new SmeltableResource(Material.GOLD_INGOT, Material.GOLD_INGOT, 1),
        new SmeltableResource(Material.GOLDEN_HELMET, Material.GOLD_INGOT, 5),
        new SmeltableResource(Material.GOLDEN_CHESTPLATE, Material.GOLD_INGOT, 8),
        new SmeltableResource(Material.GOLDEN_LEGGINGS, Material.GOLD_INGOT, 7),
        new SmeltableResource(Material.GOLDEN_BOOTS, Material.GOLD_INGOT, 4),
        new SmeltableResource(Material.GOLDEN_AXE, Material.GOLD_INGOT, 3),
        new SmeltableResource(Material.GOLDEN_PICKAXE, Material.GOLD_INGOT, 3),
        new SmeltableResource(Material.GOLDEN_SWORD, Material.GOLD_INGOT, 2),
        new SmeltableResource(Material.GOLDEN_HOE, Material.GOLD_INGOT, 2),
        new SmeltableResource(Material.GOLDEN_SHOVEL, Material.GOLD_INGOT, 1),
        new SmeltableResource(Material.CLOCK, Material.GOLD_INGOT, 4)
    );

    private static final List<Material> INGOTS;

    static {
        Set<Material> ingots = new HashSet<>();

        for (SmeltableResource resource : SMELTABLE_RESOURCES) {
            ingots.add(resource.getOutputMaterial());
        }

        INGOTS = List.copyOf(ingots);
    }

    public FactorySmelter(World world, ProtectedRegion region,
                          ProtectedRegion[] lavaChannels, ProtectedRegion lavaZone) {
        super(world, region);
        this.lavaSupply = new LavaSupply(world, lavaChannels, lavaZone);
    }

    @Override
    public String getName() {
        return "Smelter - I";
    }

    private void processLava(Collection<Player> playerList) {
        if (playerList.isEmpty()) {
            return;
        }

        if (ChanceUtil.getChance(5)) {
            lavaSupply.checkDamage();
        }

        if (lavaSupply.tryAddLava()) {
            ChatUtil.sendNotice(playerList, "Adding lava...");
        }
    }

    private int getProducedAmount(ItemStack itemStack, SmeltableResource resource) {
        int producedAmount;
        if (EnvironmentUtil.isOre(itemStack.getType())) {
            producedAmount = resource.getOutputQuantity() * items.size();
            if (ModifierComponent.getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
                producedAmount *= 3;
            }
        } else {
            float remainingDurability = ItemUtil.getRemainingDurability(itemStack).orElse(1f);
            producedAmount = (int) (resource.getOutputQuantity() * remainingDurability);

            // 50% chance of turning extremely damaged things into 1 item
            if (producedAmount == 0 && ChanceUtil.getChance(2)) {
                ++producedAmount;
            }
        }
        return producedAmount;
    }

    private void processNewMaterials(Collection<Player> playerList, Collection<Item> entities) {
        if (entities.isEmpty())  {
            return;
        }

        ChatUtil.sendNotice(playerList, "[" + getName() + "] Processing...");

        boolean processed = false;
        for (Item e : entities) {
            // Find items and destroy those unwanted
            ItemStack workingStack = e.getItemStack();
            e.remove();

            // Check for a match with smeltable resources
            Optional<SmeltableResource> optResource = SMELTABLE_RESOURCES.stream().filter(
                (k) -> k.matches(workingStack)
            ).findFirst();
            if (optResource.isEmpty()) {
                continue;
            }

            processed = true;

            SmeltableResource resource = optResource.get();
            int producedAmount = getProducedAmount(workingStack, resource);
            items.merge(resource.getOutputMaterial(), producedAmount, Integer::sum);

            ItemNameCalculator.computeItemName(workingStack).ifPresent((name) -> {
                int foundAmount = workingStack.getAmount();
                ChatUtil.sendNotice(playerList, "Found: " + foundAmount + " " + name + ".");
            });
            ItemNameCalculator.computeItemName(new ItemStack(resource.getOutputMaterial())).ifPresent((name) -> {
                ChatUtil.sendNotice(playerList, "Smelting: " + producedAmount + " " + name + ".");
            });
        }

        if (processed) {
            markDirty();
        }
    }

    @Override
    public void consume() {
        Collection<Player> playerList = getContained(1, Player.class);
        processLava(playerList);

        Collection<Item> contained = getContained(Item.class);
        processNewMaterials(playerList, contained);
    }

    @Override
    public List<ItemStack> produceUpTo(int amount) {
        if (items.isEmpty()) {
            return List.of();
        }

        List<ItemStack> results = new ArrayList<>();

        CollectionUtil.randomIterateFor(INGOTS, (ingot) -> {
            int ingotsReady = items.getOrDefault(ingot, 0);
            if (ingotsReady == 0) {
                return false;
            }

            // Cap the number of ingots to the smaller amount of what we have ready
            // and how many we are allowed to produce
            int cappedIngots = Math.min(amount - results.size(), ingotsReady);

            // Cap this operation again by how much lava is available
            int lavaRemoved = lavaSupply.removeLava(Math.max(cappedIngots / 8, 1));
            if (lavaRemoved == 0) {
                // Short circuit, there's no lava
                return true;
            }

            // If we only have 3 ingots ready, we still want to remove 1 lava, but we don't
            // want to produce 8 ingots, so cap this to the capped ingots if that's smaller
            // than our lava removal quantity
            int producedIngots = Math.min(lavaRemoved * 8, cappedIngots);

            // Produce the results
            for (int i = 0; i < producedIngots; ++i) {
                results.add(new ItemStack(ingot, 1));
            }

            // Update the mapping
            items.compute(ingot, (ignored, existingCount) -> {
                assert existingCount != null;

                int newCount = existingCount - producedIngots;
                if (newCount == 0) {
                    return null;
                }

                return newCount;
            });

            // Stop this as we've met the maximum amount
            return results.size() == amount;
        });

        if (!results.isEmpty()) {
            markDirty();
        }

        return results;
    }

    @Override
    public void load() {
        try {
            stateHelper = new SingleFileFilesystemStateHelper<>("factory-smelter.json", new TypeToken<>() { });
            stateHelper.load().ifPresent(loadedState -> items = loadedState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void saveImpl() {
        try {
            stateHelper.save(items);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
