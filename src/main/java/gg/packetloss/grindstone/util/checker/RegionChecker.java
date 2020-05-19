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

public class RegionChecker extends Checker<Region, BlockVector3> {

    public RegionChecker(Region region) {
        super(region);
    }

    public RegionChecker(ProtectedRegion region) {
        this(RegionUtil.convert(region).orElseThrow());
    }

    @Override
    public Boolean evaluate(BlockVector3 vector) {
        return get().contains(vector);
    }
}
