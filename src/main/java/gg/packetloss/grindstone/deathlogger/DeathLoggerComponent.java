/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.deathlogger;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.PlayerDeathDropRedirectEvent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Death Logger", desc = "Logs information about player deaths.")
@Depend(components = {PlayerStateComponent.class})
public class DeathLoggerComponent extends BukkitComponent implements Listener {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final Logger deathLogger = Logger.getLogger("Minecraft.Grindstone.Death");

    @InjectComponent
    private PlayerStateComponent playerState;

    @Override
    public void enable() {
        loadLogger();

        CommandBook.registerEvents(this);
    }

    @Override
    public void disable() {
        unloadLogger();
    }

    private void loadLogger() {
        try {
            deathLogger.setUseParentHandlers(false);

            Path deathLogDir = Path.of(CommandBook.inst().getDataFolder().getPath(), "death-logs");
            if (!Files.exists(deathLogDir)) Files.createDirectories(deathLogDir);
            Path filePath = deathLogDir.resolve("player-deaths.txt");

            // This block configure the logger with handler and formatter
            FileHandler handler = new FileHandler(filePath.toAbsolutePath().toString(), true);
            handler.setFormatter(new java.util.logging.Formatter() {

                @Override
                public String format(LogRecord record) {
                    return "[" + DATE_FORMAT.format(new Date()) + "] " + record.getMessage() + "\r\n";
                }
            });

            deathLogger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void unloadLogger() {
        for (Handler handler : deathLogger.getHandlers()) {
            if (handler instanceof FileHandler) {
                handler.flush();
                handler.close();
                deathLogger.removeHandler(handler);
            }
        }
    }

    private String getPlayerIdentifier(Player player) {
        String playerName = player.getName();
        String playerId = player.getUniqueId().toString();
        return playerName + " (" + playerId + ")";
    }

    private String getLocation(Location loc) {
        return " [" + loc.getWorld().getName() + "; " +
            loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "]";
    }

    private String getHasTempKind(Player player) {
        try {
            return playerState.hasTempKind(player) ? " - TEMP STATE" : "";
        } catch (IOException e) {
            e.printStackTrace();
            return " - MAYBE TEMP STATE";
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerId = getPlayerIdentifier(player);
        String gameMode = player.getGameMode().name();
        String tempKind = getHasTempKind(player);
        String deathLocation = getLocation(player.getLocation());

        deathLogger.info(playerId + " - " + gameMode + tempKind + deathLocation);
        for (ItemStack drop : event.getDrops()) {
            String itemName = ItemNameCalculator.computeItemName(drop)
                .map(String::toUpperCase)
                .orElse("UNKNOWN");

            deathLogger.info(itemName + " " + drop.getMaxStackSize());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathDropRedirect(PlayerDeathDropRedirectEvent event) {
        Player player = event.getPlayer();
        String playerId = getPlayerIdentifier(player);
        String graveLocation = getLocation(event.getDropLocation());

        deathLogger.info("GRAVE CREATE - " + playerId + graveLocation);
    }
}
