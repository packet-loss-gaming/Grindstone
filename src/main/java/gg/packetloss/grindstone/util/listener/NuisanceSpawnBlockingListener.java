/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.listener;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.entity.EntitySpawnBlockedEvent;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.function.Predicate;

public class NuisanceSpawnBlockingListener implements Listener {
    private final Predicate<World> appliesTo;

    public NuisanceSpawnBlockingListener(Predicate<World> appliesTo) {
        this.appliesTo = appliesTo;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDoorBreak(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        if (!appliesTo.test(entity.getWorld())) {
            return;
        }

        if (!EntityUtil.isHostileMob(entity)) {
            return;
        }

        SpawnReason reason = event.getSpawnReason();
        if (reason != SpawnReason.NETHER_PORTAL) {
            return;
        }

        EntitySpawnBlockedEvent blockEvent = new EntitySpawnBlockedEvent(entity);

        CommandBook.callEvent(blockEvent);
        if (blockEvent.isCancelled()) {
            return;
        }

        event.setCancelled(true);
    }
}
