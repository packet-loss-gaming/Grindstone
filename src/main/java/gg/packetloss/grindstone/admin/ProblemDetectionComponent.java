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
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.functional.TriFunction;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Problem Detector", desc = "Problem detection system.")
@Depend(components = {ChatBridgeComponent.class, ShutdownComponent.class})
public class ProblemDetectionComponent extends BukkitComponent {
    @InjectComponent
    private ChatBridgeComponent chatBridge;
    @InjectComponent
    private ShutdownComponent shutdown;

    private LocalConfiguration config;

    private DebounceHandle<Long> serverLagDebounce;

    private int numOfContiguousTimeChunks = 0;
    private boolean wereMessagesSent = false;

    private int knownHeavyOperationsInProgress = 0;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            CommandBook.registerEvents(new TickMonitor());
            CommandBook.registerEvents(new ChunkMonitor());
        }, 20 * 5);

        CommandBook.server().getScheduler().runTaskTimer(CommandBook.inst(), () -> {
            if (knownHeavyOperationsInProgress > 0) {
                // Allow heavy operations to run through, always.
                return;
            }

            // If messages were sent we continue to have problems, increment the time chunks counter,
            // otherwise, reset the time chunks counter.
            if (wereMessagesSent) {
                wereMessagesSent = false;
                ++numOfContiguousTimeChunks;
            } else {
                numOfContiguousTimeChunks = 0;
            }

            if (numOfContiguousTimeChunks >= config.maxContiguousTimeChunks) {
                shutdown.shutdown(null, 60, ShutdownComponent.DEFAULT_DOWN_TIME);
            }
        }, 0, TimeUtil.convertMinutesToTicks(15));
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
        @Setting("chunk-watcher.live-diagnostics")
        public boolean chunkThrashingLiveDiagnostics = false;
        @Setting("auto-shutdown.max-contiguos-time-chunks")
        public int maxContiguousTimeChunks = 5;
    }

    public void registerHeavyLoad() {
        ++knownHeavyOperationsInProgress;
    }

    public void unregisterHeavyLoad() {
        Validate.isTrue(knownHeavyOperationsInProgress > 0);
        --knownHeavyOperationsInProgress;
    }

    private void reportProblemToMods(String problem) {
        wereMessagesSent = true;
        chatBridge.modBroadcast(problem);
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

                reportProblemToMods(
                    "Server fell behind (by " + millsLost + " ms, " +
                        knownHeavyOperationsInProgress + " known heavy operations)!"
                );
            }
        }
    }

    private class ChunkMonitor implements Listener {
        private final Map<String, Map<BlockVector2, Integer>> worldChunkActivityMapping = new HashMap<>();

        public ChunkMonitor() {
            scheduleNextCheck();
        }

        private void checkIterate(Map<String, Map<BlockVector2, Integer>> chunkData,
                                  TriFunction<String, BlockVector2, Integer, Optional<String>> op) {
            var worldActivityIterator = chunkData.entrySet().iterator();
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
                List<String> messages = new ArrayList<>();
                for (Map.Entry<BlockVector2, Integer> entry : worldActivityMap.entrySet()) {
                    op.accept(world, entry.getKey(), entry.getValue()).ifPresent(messages::add);
                }

                // Report any found problems as one message
                if (!messages.isEmpty()) {
                    reportProblemToMods(StringUtils.join(messages, "\n"));
                }

                // Reset the data on this world
                worldActivityMap.clear();
            }
        }

        private void checkChunkThrashing() {
            checkIterate(worldChunkActivityMapping, (world, pos, activityLevel) -> {
                if (activityLevel < config.chunkActivityLevel) {
                    return Optional.empty();
                }

                return Optional.of("Chunk thrashing: " + activityLevel +
                    " (World: " + world + "; " + pos.getBlockX() + ", " + pos.getBlockZ() + ")!");
            });
        }

        private void scheduleNextCheck() {
            checkChunkThrashing();

            CommandBook.server().getScheduler().runTaskLater(
                CommandBook.inst(),
                this::scheduleNextCheck,
                config.chunkCheckIntervalTicks
            );
        }

        private void registerChunkActivity(Chunk chunk, boolean isLoad) {
            Map<BlockVector2, Integer> mapping = worldChunkActivityMapping.computeIfAbsent(chunk.getWorld().getName(), (ignored) -> {
                return new HashMap<>();
            });

            int newValue = mapping.merge(WorldEditBridge.toBlockVec2(chunk), 1, Integer::sum);
            if (isLoad && config.chunkThrashingLiveDiagnostics && newValue >= config.chunkActivityLevel) {
                (new Exception("Chunk thrashing detected. The cause is likely in the call stack.")).printStackTrace();
            }
        }

        @EventHandler
        public void onServerTick(ChunkLoadEvent event) {
            Chunk chunk = event.getChunk();

            registerChunkActivity(chunk, true);
        }

        @EventHandler
        public void onServerTick(ChunkUnloadEvent event) {
            registerChunkActivity(event.getChunk(), false);
        }
    }
}
