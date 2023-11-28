/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.apocalypse;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Region;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import org.bukkit.Location;
import org.bukkit.World;

public class ApocalypseState {
    private final World world;
    private final BlockVector2 centralPoint;
    private final BlockVector2 boundingBox;

    public ApocalypseState(World world, BlockVector2 centralPoint, BlockVector2 boundingBox) {
        this.world = world;
        this.centralPoint = centralPoint;
        this.boundingBox = boundingBox;
    }

    public World getWorld() {
        return world;
    }

    public Location getCentralPoint() {
        return WorldEditBridge.toLocation(world, centralPoint.toBlockVector3());
    }

    public Region getAsRegion() {
        return RegionUtil.makeRegion(world, centralPoint, boundingBox);
    }
}
