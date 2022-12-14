/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.portal;

import gg.packetloss.grindstone.firstlogin.FirstLoginComponent;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.warps.WarpsComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.Function;

public class RangeWorldResolver implements WorldResolver {
    private final ManagedWorldComponent managedWorld;
    private final ManagedWorldGetQuery query;
    private final WarpsComponent warps;
    private final FirstLoginComponent firstLogin;
    private final Function<Player, ManagedWorldTimeContext> timeContextLookup;

    public RangeWorldResolver(ManagedWorldComponent managedWorld, ManagedWorldGetQuery query,
                              WarpsComponent warps, FirstLoginComponent firstLogin,
                              Function<Player, ManagedWorldTimeContext> timeContextLookup) {
        this.managedWorld = managedWorld;
        this.query = query;
        this.warps = warps;
        this.firstLogin = firstLogin;
        this.timeContextLookup = timeContextLookup;
    }

    public RangeWorldResolver(ManagedWorldComponent managedWorld, ManagedWorldGetQuery query,
                              WarpsComponent warps, FirstLoginComponent firstLogin) {
        this(managedWorld, query, warps, firstLogin, (player) -> ManagedWorldTimeContext.LATEST);
    }

    @Override
    public boolean accepts(Player player) {
        return true;
    }

    @Override
    public final TaskFuture<Optional<Location>> getLastExitLocation(Player player) {
        ManagedWorldTimeContext timeContext = timeContextLookup.apply(player);
        return TaskFuture.completed(warps.getLastPortalLocation(player, managedWorld.get(query, timeContext)));
    }

    @Override
    public TaskFuture<Location> getDefaultLocationForPlayer(Player player) {
        return firstLogin.getNewPlayerStartingLocation(player).thenApply((loc) -> {
            // Prefer the new player starting location (i.e. "invite location") if it exists for the ranged world
            // under consideration for the current time context. Otherwise, use the world spawn.
            World world = managedWorld.get(ManagedWorldGetQuery.RANGE_OVERWORLD, timeContextLookup.apply(player));
            if (world == loc.getWorld()) {
                return loc;
            }
            return world.getSpawnLocation();
        });
    }
}
