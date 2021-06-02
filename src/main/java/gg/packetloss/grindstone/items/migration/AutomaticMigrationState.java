/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration;

import com.sk89q.worldedit.math.BlockVector2;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

import static gg.packetloss.grindstone.items.custom.CustomItemCenter.REVISION;

public class AutomaticMigrationState {
    private transient Map<String, Integer> worldToIdCache = new HashMap<>();
    private int itemRevision = REVISION;
    private Set<UUID> playerIds = new HashSet<>();
    private List<String> worldToId = new ArrayList<>();
    private Map<Integer, Set<BlockVector2>> chunks = new HashMap<>();

    public boolean isOutOfDate() {
        return itemRevision != REVISION;
    }

    public void updateRevision() {
        itemRevision = REVISION;

        playerIds.clear();
        worldToIdCache.clear();
        worldToId.clear();
        chunks.clear();
    }

    public boolean isPlayerProcessed(Player player) {
        return playerIds.contains(player.getUniqueId());
    }

    public void markPlayerProcessed(Player player) {
        playerIds.add(player.getUniqueId());
    }

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

    public boolean isChunkProcessed(Chunk chunk) {
        int worldId = getWorldId(chunk.getWorld());

        Set<BlockVector2> chunkSet = chunks.get(worldId);
        if (chunkSet == null) {
            return false;
        }
        return chunkSet.contains(WorldEditBridge.toBlockVec2(chunk));
    }

    public void markChunkProcessed(Chunk chunk) {
        int worldId = getWorldId(chunk.getWorld());

        Set<BlockVector2> chunkSet = chunks.computeIfAbsent(worldId, (ignored) -> new HashSet<>());
        chunkSet.add(WorldEditBridge.toBlockVec2(chunk));
    }
}
