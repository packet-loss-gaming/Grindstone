/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

public class PixieNetworkDetail implements Comparable<PixieNetworkDetail> {
    private final int networkID;
    private final UUID namespace;
    private final String name;
    private final String worldName;
    private final BlockVector3 origin;

    public PixieNetworkDetail(int networkID, UUID namespace, String name, String worldName, BlockVector3 origin) {
        this.networkID = networkID;
        this.namespace = namespace;
        this.name = name;
        this.worldName = worldName;
        this.origin = origin;
    }

    public int getID() {
        return networkID;
    }

    public UUID getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public Location getOrigin() {
        return new Location(Bukkit.getWorld(worldName), origin.getX(), origin.getY(), origin.getZ());
    }

    @Override
    public int compareTo(PixieNetworkDetail network) {
        if (network == null) return -1;
        return this.getName().compareTo(network.getName());
    }
}
