/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
}
