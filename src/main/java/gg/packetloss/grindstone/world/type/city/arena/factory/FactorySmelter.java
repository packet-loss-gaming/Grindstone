/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena.factory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

public class FactorySmelter extends FactoryMech {

    private static int count = 0;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private LavaSupply lavaSupply;

    private static final Set<Material> WANTED = Set.of(
        Material.IRON_ORE, Material.GOLD_ORE,
        Material.RAW_IRON, Material.RAW_GOLD,
        Material.RAW_IRON_BLOCK, Material.RAW_GOLD_BLOCK
    );

    public FactorySmelter(World world, ProtectedRegion region, YAMLProcessor processor,
                          ProtectedRegion[] lavaChannels, ProtectedRegion lavaZone) {
        super(world, region, processor, "ores-" + count++);
        this.lavaSupply = new LavaSupply(world, lavaChannels, lavaZone);
    }

    @Override
    public String getName() {
        return "Smelter - I";
    }

    private static record Conversion(Material newType, int multiplier) { };

    private static final Map<Material, Conversion> CONVERSION_MAPPING = Map.of(
        Material.IRON_ORE, new Conversion(Material.RAW_IRON, 1),
        Material.GOLD_ORE, new Conversion(Material.RAW_GOLD, 1),
        Material.RAW_IRON_BLOCK, new Conversion(Material.RAW_IRON, 9),
        Material.RAW_GOLD_BLOCK, new Conversion(Material.RAW_GOLD, 9)
    );

    private void processWantedItem(Player thrower, Collection<Player> audible, ItemStack stack) {
        Material type = stack.getType();
        int quantity = stack.getAmount();

        Conversion conversion = CONVERSION_MAPPING.get(type);
        if (conversion != null) {
            type = conversion.newType;
            quantity *= conversion.multiplier;
        }

        if (thrower != null && thrower.hasPermission("aurora.tome.cursedsmelting")) {
            quantity *= ChanceUtil.getRandom(3);
        }

        ChatUtil.sendNotice(audible, "Found: " + quantity + " " + type + ".");
        items.merge(type, quantity, Integer::sum);
    }

    @Override
    public List<ItemStack> process() {
        Collection<Player> playerList = getContained(1, Player.class);

        if (!playerList.isEmpty()) {
            if (ChanceUtil.getChance(5)) {
                lavaSupply.checkDamage();
            }

            if (lavaSupply.tryAddLava()) {
                ChatUtil.sendNotice(playerList, "Adding lava...");
            }
        }

        Collection<Item> contained = getContained(Item.class);
        if (!contained.isEmpty()) ChatUtil.sendNotice(playerList, "[" + getName() + "] Processing...");
        for (Item e : contained) {
            // Find items and destroy those unwanted
            Player thrower = EntityUtil.getThrower(e);
            ItemStack workingStack = e.getItemStack();

            // Add the item to the list
            if (WANTED.contains(workingStack.getType())) {
                processWantedItem(thrower, playerList, workingStack);
            }
            e.remove();
        }

        if (!contained.isEmpty()) {
            save(); // Update save for new Iron & Gold values
        }

        int maxIron = items.getOrDefault(Material.RAW_IRON, 0);
        int maxGold = items.getOrDefault(Material.RAW_GOLD, 0);

        if (maxGold + maxIron < 1) return new ArrayList<>();

        int requestedLava = Math.max(1, Math.max(maxIron, maxGold) / 8);
        int availableLava = lavaSupply.removeLava(requestedLava);

        int ironRemainder = maxIron - (availableLava * 8);
        int goldRemainder = maxGold - (availableLava * 8);

        if (ironRemainder < 1) {
            items.remove(Material.RAW_IRON);
        } else {
            items.put(Material.RAW_IRON, ironRemainder);
        }
        if (goldRemainder < 1) {
            items.remove(Material.RAW_GOLD);
        } else {
            items.put(Material.RAW_GOLD, goldRemainder);
        }
        save(); // Update save for new Iron & Gold values

        if (availableLava < requestedLava) {
            if (maxIron > 0) maxIron = maxIron - ironRemainder;
            if (maxGold > 0) maxGold = maxGold - goldRemainder;
        }

        maxIron *= 4;
        maxGold *= 4;

        if (ModifierComponent.getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
            maxIron *= 3;
            maxGold *= 3;
        }

        // Tell the player what we are making
        if (maxIron > 0) {
            ChatUtil.sendNotice(playerList, "Smelting: "  + maxIron + " iron ingots.");
        }
        if (maxGold > 0) {
            ChatUtil.sendNotice(playerList, "Smelting: " + maxGold + " gold ingots.");
        }
        // Return the product for the que
        List<ItemStack> product = new ArrayList<>();
        for (int i = maxIron; i > 0; --i) product.add(new ItemStack(Material.IRON_INGOT));
        for (int i = maxGold; i > 0; --i) product.add(new ItemStack(Material.GOLD_INGOT));
        return product;
    }
}
