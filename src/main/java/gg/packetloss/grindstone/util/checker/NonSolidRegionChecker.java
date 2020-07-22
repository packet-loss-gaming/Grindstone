/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.checker;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.RegionUtil;
import org.bukkit.Location;
import org.bukkit.World;

public class NonSolidRegionChecker extends RegionChecker {
    private World world;

    public NonSolidRegionChecker(Region region, World world) {
        super(region);
        this.world = world;
    }

    public NonSolidRegionChecker(ProtectedRegion region, World world) {
        this(RegionUtil.convert(region).orElseThrow(), world);
    }

    @Override
    public Boolean evaluate(BlockVector3 v) {
        Location l = new Location(world, v.getX(), v.getY(), v.getZ());
        return super.evaluate(v) && !l.getBlock().getType().isSolid();
    }
}
