/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;

public class SkullPlacer {
    public static void placePlayerSkullOnGround(Location location, BlockFace facingDirection, OfflinePlayer player) {
        Block block = location.getBlock();
        block.setType(Material.PLAYER_HEAD);

        if (block.getState() instanceof Skull) {
            Skull state = (Skull) block.getState();
            state.setOwningPlayer(player);

            Rotatable rotatable = (Rotatable) state.getBlockData();
            rotatable.setRotation(facingDirection);
            state.setBlockData(rotatable);

            state.update(true);
        }
    }

    public static void placePlayerSkullOnWall(Location location, BlockFace facingDirection, OfflinePlayer player) {
        Block block = location.getBlock();
        block.setType(Material.PLAYER_WALL_HEAD);

        if (block.getState() instanceof Skull) {
            Skull state = (Skull) block.getState();
            state.setOwningPlayer(player);

            Directional rotatable = (Directional) state.getBlockData();
            rotatable.setFacing(facingDirection);
            state.setBlockData(rotatable);

            state.update(true);
        }
    }
}
