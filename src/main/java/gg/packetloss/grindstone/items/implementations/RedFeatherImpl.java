/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.util.ItemPointTranslator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class RedFeatherImpl extends AbstractItemFeatureImpl {

    private static Set<EntityDamageEvent.DamageCause> ignoredCauses = new HashSet<>();

    static {
        ignoredCauses.add(EntityDamageEvent.DamageCause.POISON);
        ignoredCauses.add(EntityDamageEvent.DamageCause.WITHER);
    }

    private static ItemPointTranslator redstoneConverter = new ItemPointTranslator();

    static {
        redstoneConverter.addMapping(new ItemStack(Material.REDSTONE_BLOCK), 9);
        redstoneConverter.addMapping(new ItemStack(Material.REDSTONE), 1);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player && !ignoredCauses.contains(event.getCause())) {

            Player player = (Player) entity;
            CustomItemSession session = getSession(player);

            InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);


            if (session.canSpec(SpecType.RED_FEATHER) && ItemUtil.hasItem(player, CustomItems.RED_FEATHER)) {
                int redstoneTally = redstoneConverter.calculateValue(adapter, true);
                if (redstoneTally == 0) {
                    return;
                }

                final double dmg = event.getDamage();
                final int k = (dmg > 80 ? 16 : dmg > 40 ? 8 : dmg > 20 ? 4 : 2);

                final double blockable = redstoneTally * k;
                final double blocked = blockable - (blockable - dmg);

                int rAmt = (int) ((blockable - blocked) / k);

                // Assign items, and update the remainder
                rAmt = redstoneConverter.assignValue(adapter, rAmt);

                // Update the player inventory
                adapter.applyChanges();

                // Drop any remaining redstone on the floor, if the inventory overflowed
                World w = player.getWorld();
                redstoneConverter.streamValue(rAmt, (stack) -> {
                    w.dropItem(player.getLocation(), stack);
                });

                event.setDamage(Math.max(0, dmg - blocked));
                player.setFireTicks(0);

                // Update the session
                session.updateSpec(SpecType.RED_FEATHER, (long) (blocked * 75));
            }
        }
    }

    private static class Value {
        private final Material type;
        private int scale;

        public Value(Material type, int scale) {
            this.type = type;
            this.scale = scale;
        }

        public Material getType() {
            return type;
        }

        public int getScale() {
            return scale;
        }
    }
}
