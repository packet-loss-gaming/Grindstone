/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Cancellable;

public interface ApocalypseEvent extends Cancellable {
    Location getLocation();

    default World getWorld() {
        return getLocation().getWorld();
    }
}
