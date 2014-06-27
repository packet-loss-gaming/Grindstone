/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.BaseItem;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;

public class FactoryBrewer extends FactoryMech {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    public FactoryBrewer(World world, ProtectedRegion region) {
        super(world, region);
    }

    private static final List<BaseItem> wanted = new ArrayList<>();

    static {
        wanted.add(new BaseItem(ItemID.GLASS_BOTTLE));
        wanted.add(new BaseItem(ItemID.NETHER_WART_SEED));

        wanted.add(new BaseItem(ItemID.LIGHTSTONE_DUST));
        wanted.add(new BaseItem(ItemID.REDSTONE_DUST));
        wanted.add(new BaseItem(ItemID.SULPHUR));

        wanted.add(new BaseItem(ItemID.MAGMA_CREAM));
        wanted.add(new BaseItem(ItemID.SUGAR));
        wanted.add(new BaseItem(ItemID.GLISTERING_MELON));
        wanted.add(new BaseItem(ItemID.SPIDER_EYE));
        wanted.add(new BaseItem(ItemID.GHAST_TEAR));
        wanted.add(new BaseItem(ItemID.BLAZE_POWDER));
        wanted.add(new BaseItem(ItemID.FERMENTED_SPIDER_EYE));
        wanted.add(new BaseItem(ItemID.GOLDEN_CARROT));
        wanted.add(new BaseItem(ItemID.RAW_FISH, 3));

        // Special - Non-Vanilla
        wanted.add(new BaseItem(ItemID.NETHER_QUARTZ));
    }

    @Override
    public List<ItemStack> process() {

        Collection<Player> playerList = getContained(1, Player.class);

        Collection<Entity> contained = getContained(Entity.class);
        if (!contained.isEmpty()) ChatUtil.sendNotice(playerList, "Processing...");

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
                if (wanted.contains(new BaseItem(workingStack.getTypeId(), workingStack.getData().getData()))) {
                    int total = workingStack.getAmount();
                    ChatUtil.sendNotice(playerList, "Found: " + total + " " + workingStack.getType().toString() + ".");
                    if (items.containsKey(workingStack.getTypeId())) {
                        total += items.get(workingStack.getTypeId());
                    }
                    items.put(workingStack.getTypeId(), total);
                }
            }
            e.remove();
        }

        // Check these to avoid doing more calculations than need be
        int bottles = items.containsKey(ItemID.GLASS_BOTTLE) ? items.get(ItemID.GLASS_BOTTLE) : 0;
        int max = items.containsKey(ItemID.NETHER_WART_SEED) ? items.get(ItemID.NETHER_WART_SEED) : 0;
        if (bottles <= 0 || max <= 0) return new ArrayList<>();

        // Figure out the potion the player is trying to make
        List<Integer> using = new ArrayList<>();
        PotionType target;
        if (items.containsKey(ItemID.MAGMA_CREAM)) {
            target = PotionType.FIRE_RESISTANCE;
            using.add(ItemID.MAGMA_CREAM);
        } else if (items.containsKey(ItemID.SUGAR)) {
            target = PotionType.SPEED;
            using.add(ItemID.SUGAR);
        } else if (items.containsKey(ItemID.GLISTERING_MELON)) {
            target = PotionType.INSTANT_HEAL;
            using.add(ItemID.GLISTERING_MELON);
        } else if (items.containsKey(ItemID.SPIDER_EYE)) {
            target = PotionType.POISON;
            using.add(ItemID.SPIDER_EYE);
        } else if (items.containsKey(ItemID.GHAST_TEAR)) {
            target = PotionType.REGEN;
            using.add(ItemID.GHAST_TEAR);
        } else if (items.containsKey(ItemID.BLAZE_POWDER)) {
            target = PotionType.STRENGTH;
            using.add(ItemID.BLAZE_POWDER);
        } else if (items.containsKey(ItemID.FERMENTED_SPIDER_EYE)) {
            target = PotionType.WEAKNESS;
            using.add(ItemID.FERMENTED_SPIDER_EYE);
        } else if (items.containsKey(ItemID.GOLDEN_CARROT)) {
            target = PotionType.NIGHT_VISION;
            using.add(ItemID.GOLDEN_CARROT);
        } else if (items.containsKey(ItemID.RAW_FISH)) {
            target = PotionType.WATER_BREATHING;
            using.add(ItemID.RAW_FISH);
        } else return new ArrayList<>();

        // Always used
        using.add(ItemID.GLASS_BOTTLE);
        using.add(ItemID.NETHER_WART_SEED);

        // Setup some important information
        boolean duration, potency, splash, increased;

        duration = items.keySet().contains(ItemID.REDSTONE_DUST);
        potency = items.keySet().contains(ItemID.LIGHTSTONE_DUST);
        splash = items.keySet().contains(ItemID.SULPHUR);
        increased = items.keySet().contains(ItemID.NETHER_QUARTZ);

        // Adapt as needed based on the information above
        if (duration && !target.isInstant()) {
            using.add(ItemID.REDSTONE_DUST);
        } else if (potency) {
            using.add(ItemID.LIGHTSTONE_DUST);
        }

        if (splash) {
            using.add(ItemID.SULPHUR);
        }
        if (increased) {
            using.add(ItemID.NETHER_QUARTZ);
        }

        // Find the max amount skipping glass bottles (too be checked later)
        for (Integer used : using) {
            if (used == ItemID.GLASS_BOTTLE) continue;
            max = Math.min(max, items.get(used));
        }

        // This is confusing, essentially we are dividing the bottle count into three pieces
        // That allows us to figure out how many potion sets can be made
        // We will later expand the potion sets again
        max = (int) Math.min(max, Math.floor(bottles / 3));

        if (max <= 0) return new ArrayList<>();

        // Remove the used ingredients from the system
        int newAmt;
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            if (using.contains(entry.getKey())) {
                newAmt = entry.getValue() - (entry.getKey() == ItemID.GLASS_BOTTLE ? max * 3 : max);
                if (newAmt > 0) items.put(entry.getKey(), newAmt);
                else items.remove(entry.getKey());
            }
        }

        // Inflate potion quantity
        max *= (increased ? 15 : 10) * 3;
        if (getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
            max *= 3;
        }

        // Calculate damage
        int level = !duration && potency ? 2 : 1;
        short dmg = toDamageValue(target, level, splash, duration && !target.isInstant());

        // Tell the player what we are making
        ChatUtil.sendNotice(playerList, "Brewing: " + max + " " + target.toString() + " "
                + (level == 1 ? "I" : "II") + " " + (splash ? "splash" : "") + " potions.");
        // Return the product for the que
        List<ItemStack> product = new ArrayList<>();
        for (int i = 0; i < max; i++) product.add(new ItemStack(ItemID.POTION, 1, dmg));
        return product;
    }

    /**
     * Copied from the Bukkit potion class
     * <p>
     * Converts this potion to a valid potion damage short, usable for potion
     * item stacks.
     *
     * @return The damage value of this potion
     */
    public short toDamageValue(PotionType type, int level, boolean splash, boolean extended) {
        short damage;
        if (type == PotionType.WATER) {
            return 0;
        } else if (type == null) {
            damage = 0;
        } else {
            damage = (short) (level - 1);
            damage <<= 5;
            damage |= (short) type.getDamageValue();
        }
        if (splash) {
            damage |= 0x4000;
        }
        if (extended) {
            damage |= 0x40;
        }
        return damage;
    }
}
