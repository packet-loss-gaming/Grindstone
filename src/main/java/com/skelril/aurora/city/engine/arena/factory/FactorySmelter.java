/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChatUtil;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;

public class FactorySmelter extends FactoryMech {

    private static int count = 0;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private LavaSupply lavaSupply;

    private static final List<Integer> wanted = new ArrayList<>();

    static {
        wanted.add(BlockID.IRON_ORE);
        wanted.add(BlockID.GOLD_ORE);
    }

    public FactorySmelter(World world, ProtectedRegion region, YAMLProcessor processor,
                          ProtectedRegion lavaSupply, ProtectedRegion lavaZone) {
        super(world, region, processor, "ores-" + count++);
        this.lavaSupply = new LavaSupply(world, lavaSupply, lavaZone);
    }

    @Override
    public List<ItemStack> process() {
        Collection<Player> playerList = getContained(1, Player.class);

        Collection<Item> lavaContained = lavaSupply.getContained(Item.class);
        if (lavaContained.size() > 0) ChatUtil.sendNotice(playerList, "Adding lava...");
        int totalLava = items.containsKey(ItemID.LAVA_BUCKET) ? items.get(ItemID.LAVA_BUCKET) : 0;
        for (Item e : lavaContained) {
            // Find items and destroy those unwanted
            ItemStack workingStack = e.getItemStack();

            // Add the item to the list
            if (workingStack.getType().equals(Material.LAVA_BUCKET)) {
                int total = workingStack.getAmount();
                if (items.containsKey(workingStack.getTypeId())) {
                    total += items.get(workingStack.getTypeId());
                }
                items.put(workingStack.getTypeId(), totalLava = total);
            }
            e.remove();
        }
        int lavaRemainder = lavaSupply.addLava(totalLava);
        if (lavaRemainder < 1) {
            items.remove(ItemID.LAVA_BUCKET);
        } else {
            items.put(ItemID.LAVA_BUCKET, lavaRemainder);
        }

        Collection<Item> contained = getContained(Item.class);
        if (!contained.isEmpty()) ChatUtil.sendNotice(playerList, "Processing...");
        for (Item e : contained) {
            // Find items and destroy those unwanted
            ItemStack workingStack = e.getItemStack();

            // Add the item to the list
            if (wanted.contains(workingStack.getTypeId())) {
                int total = workingStack.getAmount();
                ChatUtil.sendNotice(playerList, "Found: " + total + " " + workingStack.getType().toString() + ".");
                if (items.containsKey(workingStack.getTypeId())) {
                    total += items.get(workingStack.getTypeId());
                }
                items.put(workingStack.getTypeId(), total);
            }
            e.remove();
        }

        if (!contained.isEmpty() || !lavaContained.isEmpty()) {
            save(); // Update save for new Iron & Gold values
        }

        int maxIron = items.containsKey(BlockID.IRON_ORE) ? items.get(BlockID.IRON_ORE) : 0;
        int maxGold = items.containsKey(BlockID.GOLD_ORE) ? items.get(BlockID.GOLD_ORE) : 0;

        if (maxGold + maxIron < 1) return new ArrayList<>();

        int requestedLava = Math.max(1, Math.max(maxIron, maxGold) / 8);
        int availableLava = lavaSupply.removeLava(requestedLava);

        int ironRemainder = maxIron - (availableLava * 8);
        int goldRemainder = maxGold - (availableLava * 8);

        if (ironRemainder < 1) {
            items.remove(BlockID.IRON_ORE);
        } else {
            items.put(BlockID.IRON_ORE, ironRemainder);
        }
        if (goldRemainder < 1) {
            items.remove(BlockID.GOLD_ORE);
        } else {
            items.put(BlockID.GOLD_ORE, goldRemainder);
        }
        save(); // Update save for new Iron & Gold values

        if (availableLava < requestedLava) {
            if (maxIron > 0) maxIron = maxIron - ironRemainder;
            if (maxGold > 0) maxGold = maxGold - goldRemainder;
        }

        maxIron *= 8;
        maxGold *= 8;

        if (getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
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
        for (int i = maxIron; i > 0; --i) product.add(new ItemStack(ItemID.IRON_BAR));
        for (int i = maxGold; i > 0; --i) product.add(new ItemStack(ItemID.GOLD_BAR));
        return product;
    }
}
