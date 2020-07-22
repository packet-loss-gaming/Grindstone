/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.portal;

import gg.packetloss.grindstone.firstlogin.FirstLoginComponent;
import gg.packetloss.grindstone.warps.WarpsComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.timetravel.TimeTravelComponent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public class RangeWorldResolver implements WorldResolver {
    private final ManagedWorldComponent managedWorld;
    private final ManagedWorldGetQuery query;
    private final WarpsComponent warps;
    private final FirstLoginComponent firstLogin;
    private final TimeTravelComponent timeTravel;

    public RangeWorldResolver(ManagedWorldComponent managedWorld, ManagedWorldGetQuery query,
                              WarpsComponent warps, FirstLoginComponent firstLogin, TimeTravelComponent timeTravel) {
        this.managedWorld = managedWorld;
        this.query = query;
        this.warps = warps;
        this.firstLogin = firstLogin;
        this.timeTravel = timeTravel;
    }

    @Override
    public boolean accepts(Player player) {
        return true;
    }

    private World getWorld(Player player) {
        return managedWorld.get(query, timeTravel.getTimeContextFor(player));
    }

    @Override
    public Optional<Location> getLastExitLocation(Player player) {
        return warps.getLastPortalLocation(player, getWorld(player));
    }

    @Override
    public Location getDefaultLocationForPlayer(Player player) {
        return firstLogin.getNewPlayerStartingLocation(player, timeTravel.getTimeContextFor(player));
    }
}
