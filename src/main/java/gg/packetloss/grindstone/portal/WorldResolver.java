/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface WorldResolver {
    public boolean accepts(Player player);
    public Optional<Location> getLastExitLocation(Player player);
    public Location getDefaultLocationForPlayer(Player player);
    default public Location getDestinationFor(Player player) {
        Optional<Location> optLastExit = getLastExitLocation(player);
        return optLastExit.orElseGet(() -> getDefaultLocationForPlayer(player));

    }
}
