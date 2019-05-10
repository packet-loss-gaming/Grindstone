/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.items.custom.CustomItems;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();

        if (entity instanceof Player && !ignoredCauses.contains(event.getCause())) {

            Player player = (Player) entity;
            CustomItemSession session = getSession(player);
            ItemStack[] contents = player.getInventory().getContents();

            if (session.canSpec(SpecType.RED_FEATHER) && ItemUtil.hasItem(player, CustomItems.RED_FEATHER)) {

                final int redQD = ItemUtil.countItemsOfType(contents, ItemID.REDSTONE_DUST);
                final int redQB = 9 * ItemUtil.countItemsOfType(contents, BlockID.REDSTONE_BLOCK);

                int redQ = redQD + redQB;

                if (redQ > 0) {

                    contents = ItemUtil.removeItemOfType(contents, ItemID.REDSTONE_DUST);
                    contents = ItemUtil.removeItemOfType(contents, BlockID.REDSTONE_BLOCK);

                    player.getInventory().setContents(contents);

                    final double dmg = event.getDamage();
                    final int k = (dmg > 80 ? 16 : dmg > 40 ? 8 : dmg > 20 ? 4 : 2);

                    final double blockable = redQ * k;
                    final double blocked = blockable - (blockable - dmg);

                    redQ = (int) ((blockable - blocked) / k);

                    World w = player.getWorld();

                    while (redQ / 9 > 0) {
                        ItemStack is = new ItemStack(BlockID.REDSTONE_BLOCK);
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(is);
                        } else {
                            w.dropItem(player.getLocation(), is);
                        }
                        redQ -= 9;
                    }

                    while (redQ > 0) {
                        int r = Math.min(64, redQ);
                        ItemStack is = new ItemStack(ItemID.REDSTONE_DUST, r);
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(is);
                        } else {
                            w.dropItem(player.getLocation(), is);
                        }
                        redQ -= r;
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
    }
}
