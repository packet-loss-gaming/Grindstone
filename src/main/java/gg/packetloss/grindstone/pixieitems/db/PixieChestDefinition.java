/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.db;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Set;

public class PixieChestDefinition {
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final Set<String> itemNames;

    public PixieChestDefinition(String worldName, int x, int y, int z, Set<String> itemNames) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.itemNames = itemNames;
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        return new Location(world, x, y, z);
    }

    public ChestKind getChestKind() {
        return itemNames == null ? ChestKind.SOURCE : ChestKind.SINK;
    }

    public Set<String> getSinkItems() {
        return itemNames;
    }
}
