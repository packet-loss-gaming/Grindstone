/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.ServerShutdownEvent;
import gg.packetloss.grindstone.util.task.CountdownHandle;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.grindstone.util.timer.TimerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;

@ComponentInformation(friendlyName = "Shutdown", desc = "Shutdown system")
public class ShutdownComponent extends BukkitComponent {
    public static final String DEFAULT_DOWN_TIME = "30 seconds";

    @Override
    public void enable() {
        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            registrar.register(ShutdownCommandsRegistration.builder(), new ShutdownCommands(this));
        });
    }

    private boolean isRequesterOnlyPlayerOnline(Collection<? extends Player> players, @Nullable Player requester) {
        if (requester == null) {
            return false;
        }

        if (players.size() != 1) {
            return false;
        }

        Player player = players.iterator().next();
        return player.getUniqueId() == requester.getUniqueId();
    }

    private CountdownHandle shutdownHandle;

    private boolean checkForEarlyShutdown(@Nullable Player requester) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty() || isRequesterOnlyPlayerOnline(players, requester)) {
            return true;
        }

        return false;
    }

    private void doFinalShutdown() {
        CommandBook.callEvent(new ServerShutdownEvent(0));

        Bukkit.getOnlinePlayers().forEach((player) -> {
            player.sendActionBar(ChatColor.RED + "Shutting down!");
        });

        Bukkit.shutdown();
    }

    public void shutdown(@Nullable Player requester, int assignedSeconds, String givenDowntime) {
        if (assignedSeconds < 1) {
            Bukkit.shutdown();
            return;
        }

        if (shutdownHandle != null) {
            shutdownHandle.cancel();
        }

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(20);
        taskBuilder.setNumberOfRuns(assignedSeconds);

        taskBuilder.setAction((seconds) -> {
            if (checkForEarlyShutdown(requester)) {
                shutdownHandle.setRunsRemaining(0);
                return true;
            }

            if (TimerUtil.matchesFilter(seconds, 10, 5)) {
                String message = "Shutting down in " + seconds + " seconds - for " + givenDowntime + " of downtime!";

                Bukkit.getOnlinePlayers().forEach((player) -> {
                    player.sendActionBar(ChatColor.RED + message);
                });

                CommandBook.logger().info(message);
            }

            CommandBook.callEvent(new ServerShutdownEvent(seconds));

            return true;
        });
        taskBuilder.setFinishAction(this::doFinalShutdown);

        shutdownHandle = taskBuilder.build();
    }

    public void idleShutdown() {
        if (shutdownHandle != null) {
            shutdownHandle.cancel();
        }

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(20);
        taskBuilder.setNumberOfRuns(30);

        taskBuilder.setAction((seconds) -> {
            if (checkForEarlyShutdown(null)) {
                String message = "Honoring idle shutdown request in " + seconds + " seconds.";
                CommandBook.logger().info(message);
                return true;
            }

            shutdownHandle.setRunsRemaining(30);
            return false;
        });
        taskBuilder.setFinishAction(this::doFinalShutdown);

        shutdownHandle = taskBuilder.build();
    }
}
