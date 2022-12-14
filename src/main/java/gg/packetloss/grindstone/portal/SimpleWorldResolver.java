/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.portal;

import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.warps.WarpsComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SimpleWorldResolver implements WorldResolver {
    private final ManagedWorldComponent managedWorld;
    private final ManagedWorldGetQuery query;
    private final WarpsComponent warps;

    public SimpleWorldResolver(ManagedWorldComponent managedWorld, ManagedWorldGetQuery query, WarpsComponent warps) {
        this.managedWorld = managedWorld;
        this.query = query;
        this.warps = warps;
    }

    @Override
    public boolean accepts(Player player) {
        return true;
    }

    private World getWorld() {
        return managedWorld.get(query);
    }

    @Override
    public TaskFuture<Optional<Location>> getLastExitLocation(Player player) {
        return TaskFuture.completed(warps.getLastPortalLocation(player, getWorld()));
    }

    @Override
    public TaskFuture<Location> getDefaultLocationForPlayer(Player player) {
        return TaskFuture.completed(getWorld().getSpawnLocation());
    }
}
