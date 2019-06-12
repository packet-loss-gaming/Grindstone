/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena.factory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.BaseItem;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class FactoryBrewer extends FactoryMech {

    private static int count = 0;

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    public FactoryBrewer(World world, ProtectedRegion region, YAMLProcessor processor) {
        super(world, region, processor, "pot-ingredients-" + count++);
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
        wanted.add(new BaseItem(ItemID.POISONOUS_POTATO));
    }

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
                    if (wanted.contains(new BaseItem(workingStack.getTypeId(), workingStack.getData().getData()))) {
                        int total = workingStack.getAmount();
                        ChatUtil.sendNotice(playerList, "Found: " + total + " " + workingStack.getType().toString() + ".");
                        if (items.containsKey(workingStack.getTypeId())) {
                            total += items.get(workingStack.getTypeId());
                        }
                        items.put(workingStack.getTypeId(), total);
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
        int bottles = items.getOrDefault(ItemID.GLASS_BOTTLE, 0);
        int max = items.getOrDefault(ItemID.NETHER_WART_SEED, 0);
        if (bottles <= 0 || max <= 0) return new ArrayList<>();

        // Figure out the potion the player is trying to make
        List<Integer> using = new ArrayList<>();
        PotionEffectType target;
        if (items.containsKey(ItemID.MAGMA_CREAM)) {
            if (items.containsKey(ItemID.FERMENTED_SPIDER_EYE)) {
                target = PotionEffectType.SLOW;
                using.add(ItemID.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionEffectType.FIRE_RESISTANCE;
            }
            using.add(ItemID.MAGMA_CREAM);
        } else if (items.containsKey(ItemID.SUGAR)) {
            if (items.containsKey(ItemID.FERMENTED_SPIDER_EYE)) {
                target = PotionEffectType.SLOW;
                using.add(ItemID.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionEffectType.SPEED;
            }
            using.add(ItemID.SUGAR);
        } else if (items.containsKey(ItemID.GLISTERING_MELON)) {
            if (items.containsKey(ItemID.FERMENTED_SPIDER_EYE)) {
                target = PotionEffectType.HARM;
                using.add(ItemID.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionEffectType.HEAL;
            }
            using.add(ItemID.GLISTERING_MELON);
        } else if (items.containsKey(ItemID.SPIDER_EYE)) {
            if (items.containsKey(ItemID.FERMENTED_SPIDER_EYE)) {
                target = PotionEffectType.HARM;
                using.add(ItemID.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionEffectType.POISON;
            }
            using.add(ItemID.SPIDER_EYE);
        } else if (items.containsKey(ItemID.GHAST_TEAR)) {
            if (items.containsKey(ItemID.FERMENTED_SPIDER_EYE)) {
                target = PotionEffectType.WEAKNESS;
                using.add(ItemID.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionEffectType.REGENERATION;
            }
            using.add(ItemID.GHAST_TEAR);
        } else if (items.containsKey(ItemID.BLAZE_POWDER)) {
            if (items.containsKey(ItemID.FERMENTED_SPIDER_EYE)) {
                target = PotionEffectType.WEAKNESS;
                using.add(ItemID.FERMENTED_SPIDER_EYE);
            } else {
                target = PotionEffectType.INCREASE_DAMAGE;
            }
            using.add(ItemID.BLAZE_POWDER);
        } else if (items.containsKey(ItemID.GOLDEN_CARROT)) {
            target = PotionEffectType.NIGHT_VISION;
            using.add(ItemID.GOLDEN_CARROT);
        } else if (items.containsKey(ItemID.RAW_FISH)) {
            target = PotionEffectType.WATER_BREATHING;
            using.add(ItemID.RAW_FISH);
        } else if (items.containsKey(ItemID.POISONOUS_POTATO)) {
            target = PotionEffectType.FAST_DIGGING;
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
        save(); // Update save for new Potion resource values

        // Inflate potion quantity
        max *= (increased ? 6 : 5);
        if (ModifierComponent.getModifierCenter().isActive(ModifierType.TRIPLE_FACTORY_PRODUCTION)) {
            max *= 3;
        }

        // Calculate damage
        int level = !duration && potency ? 2 : 1;
        ItemStack potion = makePotion(target, level, splash, duration);

        // Tell the player what we are making
        ChatUtil.sendNotice(playerList, "Brewing: " + max + " " + target.getName() + " "
                + (level == 1 ? "I" : "II") + (splash ? " splash" : "") + " potions.");
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
    public ItemStack makePotion(PotionEffectType type, int level, boolean splash, boolean extended) {
        double ticks = 0;
        if (!type.isInstant()) {
            // Default is 3 minutes worth of ticks
            ticks = 20 * 60 * 3 * type.getDurationModifier();
            // A potion cannot be extended and amplified
            if (extended) {
                ticks *= (double) 8/3; // Cast to double to avoid integer division
            } else if (level == 2) {
                ticks *= (double) 1/2; // Cast to double to avoid integer division
            }

            if (splash) {
                ticks *= (double) 3/4; // Cast to double to avoid integer division
            }
        }

        // Drinkable: 8192
        // Splash: 16384
        ItemStack potion = new ItemStack(ItemID.POTION, 1, (short) (splash ? 16384 : 8192));
        PotionMeta pMeta = (PotionMeta) potion.getItemMeta();
        pMeta.setMainEffect(type);
        pMeta.addCustomEffect(new PotionEffect(type, (int) ticks, level - 1), true);
        potion.setItemMeta(pMeta);
        return potion;
    }
}
