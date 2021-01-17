/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector2;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Problem Detector", desc = "Problem detection system.")
@Depend(components = ChatBridgeComponent.class)
public class ProblemDetectionComponent extends BukkitComponent {
    @InjectComponent
    private ChatBridgeComponent chatBridge;

    private LocalConfiguration config;

    private DebounceHandle<Long> serverLagDebounce;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            CommandBook.registerEvents(new TickMonitor());
            CommandBook.registerEvents(new ChunkMonitor());
        }, 20 * 5);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("tick-watcher.min-report-ms")
        public long minFallback = 500;
        @Setting("chunk-watcher.check-interval-ticks")
        public int chunkCheckIntervalTicks = 20;
        @Setting("chunk-watcher.min-activity-level")
        public int chunkActivityLevel = 3;
    }

    private class TickMonitor implements Listener {
        @EventHandler
        public void onServerTick(ServerTickEndEvent event) {
            long timeRemaining = event.getTimeRemaining();
            if (timeRemaining < 0) {
                long millsLost = TimeUnit.NANOSECONDS.toMillis(Math.abs(timeRemaining));
                if (millsLost < config.minFallback) {
                    return;
                }

                chatBridge.modBroadcast("Server fell behind (by " + millsLost + " ms)!");
            }
        }
    }

    private class ChunkMonitor implements Listener {
        private final Map<String, Map<BlockVector2, Integer>> worldChunkActivityMapping = new HashMap<>();

        public ChunkMonitor() {
            scheduleNextCheck();
        }

        private void scheduleNextCheck() {
            var worldActivityIterator = worldChunkActivityMapping.entrySet().iterator();
            while (worldActivityIterator.hasNext()) {
                Map.Entry<String, Map<BlockVector2, Integer>> worldChunkActivity = worldActivityIterator.next();

                String world = worldChunkActivity.getKey();
                Map<BlockVector2, Integer> worldActivityMap = worldChunkActivity.getValue();

                // If there's been no activity since last check, drop the world
                if (worldActivityMap.isEmpty()) {
                    worldActivityIterator.remove();
                    continue;
                }

                // Check the chunks of this world for problems
                for (Map.Entry<BlockVector2, Integer> entry : worldActivityMap.entrySet()) {
                    int activityLevel = entry.getValue();
                    if (activityLevel < config.chunkActivityLevel) {
                        continue;
                    }

                    BlockVector2 pos = entry.getKey();
                    chatBridge.modBroadcast("Chunk thrashing: " + activityLevel +
                        " (World: " + world + "; " + pos.getBlockX() + ", " + pos.getBlockZ() + ")!");
                }

                // Reset the data on this world
                worldActivityMap.clear();
            }

            CommandBook.server().getScheduler().runTaskLater(
                CommandBook.inst(),
                this::scheduleNextCheck,
                config.chunkCheckIntervalTicks
            );
        }

        private void registerChunkActivity(Chunk chunk) {
            Map<BlockVector2, Integer> mapping = worldChunkActivityMapping.computeIfAbsent(chunk.getWorld().getName(), (ignored) -> {
                return new HashMap<>();
            });

            mapping.merge(WorldEditBridge.toBlockVec2(chunk), 1, Integer::sum);
        }

        @EventHandler
        public void onServerTick(ChunkLoadEvent event) {
            registerChunkActivity(event.getChunk());
        }

        @EventHandler
        public void onServerTick(ChunkUnloadEvent event) {
            registerChunkActivity(event.getChunk());
        }
    }
}
