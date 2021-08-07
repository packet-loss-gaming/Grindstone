/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemMergeEvent;

public class FactoryListener extends AreaListener<FactoryArea> {
    public FactoryListener(FactoryArea parent) {
        super(parent);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        if (!parent.contains(placedBlock)) {
            return;
        }

        if (event.getBlockPlaced().getType() != Material.IRON_BLOCK) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        final Entity entity = event.getEntity();

        if (!(entity instanceof Player) || !parent.contains(entity)) return;

        if (((Player) entity).isFlying() && event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
            DamageUtil.multiplyFinalDamage(event, 2);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemMege(ItemMergeEvent event) {
        for (ProtectedRegion region : parent.smeltersOutputChannels) {
            if (LocationUtil.isInRegion(region, event.getEntity()) || LocationUtil.isInRegion(region, event.getTarget())) {
                event.setCancelled(true);
            }
        }
    }
}
