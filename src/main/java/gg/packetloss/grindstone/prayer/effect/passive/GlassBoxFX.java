/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GlassBoxFX implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        LocationUtil.toGround(player);

        List<Location> queList = new ArrayList<>();
        for (Location loc : Arrays.asList(player.getLocation(), player.getEyeLocation())) {
            for (BlockFace face : EnvironmentUtil.getNearbyBlockFaces()) {
                if (face == BlockFace.SELF) continue;
                queList.add(loc.getBlock().getRelative(face).getLocation());
            }
        }

        for (Location loc : queList) {
            player.sendBlockChange(loc, Material.GLASS.createBlockData());
        }
    }

    @Override
    public void strip(Player player) { }
}
