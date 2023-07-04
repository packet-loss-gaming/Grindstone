/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.utility;

import com.sk89q.worldedit.math.BlockVector2;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WorldPreGenerator {
    private final int maxConcurrent = Runtime.getRuntime().availableProcessors();
    private final List<CompletableFuture<Chunk>> pendingChunks = new ArrayList<>();

    private final World world;
    private final List<BlockVector2> chunkList;

    public WorldPreGenerator(World world, List<BlockVector2> chunkList) {
        this.world = world;
        this.chunkList = chunkList;
    }

    /**
     * Check for completed jobs in pendingChunks, and fill empty slots with new jobs.
     *
     * @return true if generation has completed; otherwise, return false.
     */
    private boolean updateGeneration() {
        for (int i = pendingChunks.size() - 1; i >= 0; --i) {
            CompletableFuture<Chunk> pendingChunk = pendingChunks.get(i);
            if (pendingChunk.isDone()) {
                pendingChunks.remove(i);
            }
        }

        while (!chunkList.isEmpty() && pendingChunks.size() < maxConcurrent) {
            BlockVector2 chunkCoords = chunkList.remove(chunkList.size() - 1);
            pendingChunks.add(world.getChunkAtAsync(chunkCoords.getX(), chunkCoords.getZ()));
        }

        return pendingChunks.isEmpty();
    }

    public void generate(Consumer<Integer> progressCallback, Runnable finishCallback) {
        TaskBuilder.Countdown genTaskBuilder = TaskBuilder.countdown();

        genTaskBuilder.setAction((times) -> {
            boolean allDone = this.updateGeneration();
            if (!allDone) {
                progressCallback.accept(this.chunkList.size() + this.pendingChunks.size());
            }
            return allDone;
        });
        genTaskBuilder.setFinishAction(finishCallback);

        genTaskBuilder.build();
    }
}
