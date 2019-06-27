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
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedFeatherImpl extends AbstractItemFeatureImpl {

    private static Set<EntityDamageEvent.DamageCause> ignoredCauses = new HashSet<>();

    static {
        ignoredCauses.add(EntityDamageEvent.DamageCause.POISON);
        ignoredCauses.add(EntityDamageEvent.DamageCause.WITHER);
    }

    private static List<Value> acceptedValues = new ArrayList<>();

    static {
        acceptedValues.add(new Value(Material.REDSTONE_BLOCK, 9));
        acceptedValues.add(new Value(Material.REDSTONE, 1));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player && !ignoredCauses.contains(event.getCause())) {

            Player player = (Player) entity;
            CustomItemSession session = getSession(player);
            ItemStack[] contents = player.getInventory().getContents();

            if (session.canSpec(SpecType.RED_FEATHER) && ItemUtil.hasItem(player, CustomItems.RED_FEATHER)) {
                int redstoneTally = 0;

                for (int i = 0; i < contents.length; ++i) {
                    ItemStack stack = contents[i];
                    if (stack == null) {
                        continue;
                    }

                    for (Value acceptedValue : acceptedValues) {
                        if (acceptedValue.getType() == stack.getType()) {
                            redstoneTally += stack.getAmount() * acceptedValue.getScale();
                            contents[i] = null;
                            break;
                        }
                    }
                }

                if (redstoneTally == 0) {
                    return;
                }

                final double dmg = event.getDamage();
                final int k = (dmg > 80 ? 16 : dmg > 40 ? 8 : dmg > 20 ? 4 : 2);

                final double blockable = redstoneTally * k;
                final double blocked = blockable - (blockable - dmg);

                int rAmt = (int) ((blockable - blocked) / k);

                World w = player.getWorld();

                addRedstone:
                {
                    for (Value acceptedValue : acceptedValues) {
                        final int scale = acceptedValue.getScale();
                        final Material acceptedMat = acceptedValue.getType();

                        for (int i = 0; i < contents.length; ++i) {
                            // Use an insertion position which prefers to declutter the hotbar.
                            int insertionPos = (i + 9) % contents.length;

                            final ItemStack stack = contents[insertionPos];
                            int startingAmt = stack == null ? 0 : stack.getAmount();
                            int scaledRAmt = rAmt / scale;

                            if (scaledRAmt > 0 && (startingAmt == 0 || acceptedMat == stack.getType())) {
                                int quantity = Math.min(scaledRAmt + startingAmt, acceptedMat.getMaxStackSize());
                                rAmt -= (quantity - startingAmt) * scale;
                                contents[insertionPos] = new ItemStack(acceptedMat, quantity);

                                // Stop early if we no longer have anything to add
                                if (rAmt == 0) {
                                    break addRedstone;
                                }
                            }
                        }
                    }
                }

                player.getInventory().setContents(contents);

                // Drop any remaining redstone on the floor, the inventory overflowed
                for (Value acceptedValue : acceptedValues) {
                    final int scale = acceptedValue.getScale();
                    final Material acceptedMat = acceptedValue.getType();

                    // Drop items while the scaled remainder is larger than the cost per element
                    while (true) {
                        int scaledRAmt = rAmt / scale;
                        if (scaledRAmt == 0) {
                            break;
                        }

                        int quantity = Math.min(scaledRAmt, acceptedMat.getMaxStackSize());
                        ItemStack is = new ItemStack(acceptedMat, quantity);
                        w.dropItem(player.getLocation(), is);

                        rAmt -= quantity * scale;
                    }
                }

                //noinspection deprecation
                player.updateInventory();

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
