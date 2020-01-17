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
import gg.packetloss.grindstone.util.timer.CountdownTask;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
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

    private TimedRunnable shutdownRunnable;
    private String expectedDowntime;

    private int checkForEarlyShutdown(Player requester, int seconds) {
        Collection<? extends Player> players = server.getOnlinePlayers();
        if (players.isEmpty() || isRequesterOnlyPlayerOnline(players, requester)) {
            shutdownRunnable.setTimes(0);
            return 0;
        }

        return seconds;
    }

    public void shutdown(@Nullable Player requester, int assignedSeconds, String givenDowntime) {
        if (assignedSeconds < 1) {
            server.shutdown();
            return;
        }

        this.expectedDowntime = givenDowntime;

        if (shutdownRunnable != null) {
            shutdownRunnable.setTimes(assignedSeconds);
            return;
        }

        CountdownTask shutdownTask = new CountdownTask() {
            @Override
            public boolean matchesFilter(int seconds) {
                return seconds > 0 && (seconds % 5 == 0 || seconds <= 10);
            }

            @Override
            public void performStep(int seconds) {
                String message = "Shutting down in " + seconds + " seconds - for " + expectedDowntime + " of downtime!";

                Bukkit.getOnlinePlayers().forEach((player) -> {
                    player.sendActionBar(ChatColor.RED + message);
                });

                log.info(message);
            }

            @Override
            public void performFinal() {
                Bukkit.getOnlinePlayers().forEach((player) -> {
                    player.sendActionBar(ChatColor.RED + "Shutting down!");
                });

                server.shutdown();
            }

            @Override
            public void performEvery(int seconds) {
                seconds = checkForEarlyShutdown(requester, seconds);
                server.getPluginManager().callEvent(new ServerShutdownEvent(seconds));
            }
        };

        shutdownRunnable = new TimedRunnable(shutdownTask, assignedSeconds);
        checkForEarlyShutdown(requester, assignedSeconds);

        server.getScheduler().runTaskTimer(inst, shutdownRunnable, 0, 20);
    }
}
