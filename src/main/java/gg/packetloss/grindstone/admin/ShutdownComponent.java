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
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Shutdown", desc = "Shutdown system")
public class ShutdownComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @Override
    public void enable() {
        CommandBook.getComponentRegistrar().registerTopLevelCommands((commandManager, registration) -> {
            registration.register(commandManager, ShutdownCommandsRegistration.builder(), new ShutdownCommands(this));
        });
    }

    private BukkitTask task = null;

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
    private String expectedDowntime;

    private boolean checkForEarlyShutdown(Player requester) {
        Collection<? extends Player> players = server.getOnlinePlayers();
        if (players.isEmpty() || isRequesterOnlyPlayerOnline(players, requester)) {
            shutdownHandle.setRunsRemaining(0);
            return true;
        }

        return false;
    }

    public void shutdown(@Nullable Player requester, int assignedSeconds, String givenDowntime) {
        if (assignedSeconds < 1) {
            server.shutdown();
            return;
        }

        this.expectedDowntime = givenDowntime;

        if (shutdownHandle != null) {
            shutdownHandle.setRunsRemaining(assignedSeconds);
            return;
        }

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(20);
        taskBuilder.setNumberOfRuns(assignedSeconds);

        taskBuilder.setAction((seconds) -> {
            if (checkForEarlyShutdown(requester)) {
                return true;
            }

            if (TimerUtil.matchesFilter(seconds, 10, 5)) {
                String message = "Shutting down in " + seconds + " seconds - for " + expectedDowntime + " of downtime!";

                Bukkit.getOnlinePlayers().forEach((player) -> {
                    player.sendActionBar(ChatColor.RED + message);
                });

                log.info(message);
            }

            server.getPluginManager().callEvent(new ServerShutdownEvent(seconds));

            return true;
        });
        taskBuilder.setFinishAction(() -> {
            server.getPluginManager().callEvent(new ServerShutdownEvent(0));

            Bukkit.getOnlinePlayers().forEach((player) -> {
                player.sendActionBar(ChatColor.RED + "Shutting down!");
            });

            server.shutdown();
        });

        shutdownHandle = taskBuilder.build();
    }
}
