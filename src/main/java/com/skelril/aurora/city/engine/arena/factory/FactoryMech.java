package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.city.engine.arena.AbstractRegionedArena;
import com.skelril.aurora.util.ChatUtil;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FactoryMech extends AbstractRegionedArena {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    public FactoryMech(World world, ProtectedRegion region) {
        super(world, region);
    }

    ConcurrentHashMap<Integer, Integer> typeAmtHash = new ConcurrentHashMap<>();

    private static final List<Integer> wanted = new ArrayList<>();

    static {
        wanted.add(ItemID.GLASS_BOTTLE);
        wanted.add(ItemID.NETHER_WART_SEED);

        wanted.add(ItemID.LIGHTSTONE_DUST);
        wanted.add(ItemID.REDSTONE_DUST);
        wanted.add(ItemID.SULPHUR);

        wanted.add(ItemID.MAGMA_CREAM);
        wanted.add(ItemID.SUGAR);
        wanted.add(ItemID.GLISTERING_MELON);
        wanted.add(ItemID.SPIDER_EYE);
        wanted.add(ItemID.GHAST_TEAR);
        wanted.add(ItemID.BLAZE_POWDER);
        wanted.add(ItemID.FERMENTED_SPIDER_EYE);
        wanted.add(ItemID.GOLDEN_CARROT);
    }

    public List<ItemStack> process() {

        Player[] playerList = getContainedPlayers(1);
        ItemStack workingStack;
        int total;

        Entity[] contained = getContainedEntities();
        if (contained.length > 0) ChatUtil.sendNotice(playerList, "Processing...");

        for (Entity e : contained) {

            // Kill contained living entities
            if (e instanceof LivingEntity) {
                ((LivingEntity) e).setHealth(0);
                continue;
            }

            // Find items and destroy those unwanted
            if (e instanceof Item) {

                workingStack = ((Item) e).getItemStack();

                // Add the item to the list
                if (wanted.contains(workingStack.getTypeId())) {
                    total = workingStack.getAmount();
                    ChatUtil.sendNotice(playerList, "Found: " + total + " " + workingStack.getType().toString() + ".");
                    if (typeAmtHash.containsKey(workingStack.getTypeId()))  {
                        total += typeAmtHash.get(workingStack.getTypeId());
                    }
                    typeAmtHash.put(workingStack.getTypeId(), total);
                }
            }
            e.remove();
        }

        // Check these to avoid doing more calculations than need be
        int bottles = typeAmtHash.containsKey(ItemID.GLASS_BOTTLE) ? typeAmtHash.get(ItemID.GLASS_BOTTLE) : 0;
        int max = typeAmtHash.containsKey(ItemID.NETHER_WART_SEED) ? typeAmtHash.get(ItemID.NETHER_WART_SEED) : 0;
        if (bottles <= 0 || max <= 0) return new ArrayList<>();

        // Figure out the potion the player is trying to make
        List<Integer> using = new ArrayList<>();
        PotionType target;
        if (typeAmtHash.containsKey(ItemID.MAGMA_CREAM)) {
            target = PotionType.FIRE_RESISTANCE;
            using.add(ItemID.MAGMA_CREAM);
        } else if (typeAmtHash.containsKey(ItemID.SUGAR)) {
            target = PotionType.SPEED;
            using.add(ItemID.SUGAR);
        } else if (typeAmtHash.containsKey(ItemID.GLISTERING_MELON)) {
            target = PotionType.INSTANT_HEAL;
            using.add(ItemID.GLISTERING_MELON);
        } else if (typeAmtHash.containsKey(ItemID.SPIDER_EYE)) {
            target = PotionType.POISON;
            using.add(ItemID.SPIDER_EYE);
        } else if (typeAmtHash.containsKey(ItemID.GHAST_TEAR)) {
            target = PotionType.REGEN;
            using.add(ItemID.GHAST_TEAR);
        } else if (typeAmtHash.containsKey(ItemID.BLAZE_POWDER))  {
            target = PotionType.STRENGTH;
            using.add(ItemID.BLAZE_POWDER);
        } else if (typeAmtHash.containsKey(ItemID.FERMENTED_SPIDER_EYE))  {
            target = PotionType.WEAKNESS;
            using.add(ItemID.FERMENTED_SPIDER_EYE);
        } else if (typeAmtHash.containsKey(ItemID.GOLDEN_CARROT)) {
            target = PotionType.NIGHT_VISION;
            using.add(ItemID.GOLDEN_CARROT);
        } else return new ArrayList<>();

        // Always used
        using.add(ItemID.GLASS_BOTTLE);
        using.add(ItemID.NETHER_WART_SEED);

        // Setup some important information
        boolean duration, potency, splash;

        duration = typeAmtHash.keySet().contains(ItemID.REDSTONE_DUST);
        potency = typeAmtHash.keySet().contains(ItemID.LIGHTSTONE_DUST);
        splash = typeAmtHash.keySet().contains(ItemID.SULPHUR);

        // Adapt as needed based on the information above
        if (duration && !target.isInstant()) {
            using.add(ItemID.REDSTONE_DUST);
        } else if (potency) {
            using.add(ItemID.LIGHTSTONE_DUST);
        }

        if (splash) {
            using.add(ItemID.SULPHUR);
        }

        // Find the max amount skipping glass bottles (too be checked later)
        for (Integer used : using) {
            if (used == ItemID.GLASS_BOTTLE) continue;
            max = Math.min(max, typeAmtHash.get(used));
        }

        // This is confusing, essentially we are dividing the bottle count into three pieces
        // That allows us to figure out how many potion sets can be made
        // We will later expand the potion sets again
        max = (int) Math.min(max, Math.floor(bottles / 3));

        if (max <= 0) return new ArrayList<>();

        // Remove the used ingredients from the system
        int newAmt;
        for (Map.Entry<Integer, Integer> entry : typeAmtHash.entrySet()) {

            if (using.contains(entry.getKey())) {
                newAmt = entry.getValue() - (entry.getKey() == ItemID.GLASS_BOTTLE ? max * 3 : max);
                if (newAmt > 0) typeAmtHash.put(entry.getKey(), newAmt);
                else typeAmtHash.remove(entry.getKey());
            }
        }

        // Inflate potion quantity
        max *= 3;

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
     *
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
