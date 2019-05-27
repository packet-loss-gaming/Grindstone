/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

@Deprecated
public class Home {
    private final UUID playerID;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public Home(UUID playerID, String world, int x, int y, int z) {
        this.playerID = playerID;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public String getWorldName() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
}