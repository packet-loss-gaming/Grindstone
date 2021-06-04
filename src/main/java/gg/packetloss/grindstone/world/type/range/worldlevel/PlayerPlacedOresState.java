/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.worldedit.math.BlockVector3;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;

class PlayerPlacedOresState {
    private transient Map<String, Integer> worldToIdCache = new HashMap<>();
    private List<String> worldToId = new ArrayList<>();
    private Map<Integer, Set<BlockVector3>> blocks = new HashMap<>();

    private int getWorldId(World world) {
        String worldName = world.getName();
        Integer result = worldToIdCache.get(worldName);
        if (result != null) {
            return result;
        }

        int resultId = worldToId.indexOf(worldName);
        if (resultId == -1) {
            worldToId.add(worldName);
            resultId = worldToId.size() - 1;
        }
        worldToIdCache.put(worldName, resultId);
        return resultId;
    }

    public boolean isBlockPlayerPlaced(Block block) {
        int worldId = getWorldId(block.getWorld());

        Set<BlockVector3> chunkSet = blocks.get(worldId);
        if (chunkSet == null) {
            return false;
        }
        return chunkSet.contains(WorldEditBridge.toBlockVec3(block));
    }

    public void markPlayerPlaced(Block block) {
        int worldId = getWorldId(block.getWorld());

        Set<BlockVector3> chunkSet = blocks.computeIfAbsent(worldId, (ignored) -> new HashSet<>());
        chunkSet.add(WorldEditBridge.toBlockVec3(block));
    }

    public void clearPlayerPlacement(Block block) {
        int worldId = getWorldId(block.getWorld());

        Set<BlockVector3> chunkSet = blocks.computeIfAbsent(worldId, (ignored) -> new HashSet<>());
        chunkSet.remove(WorldEditBridge.toBlockVec3(block));
    }
}
