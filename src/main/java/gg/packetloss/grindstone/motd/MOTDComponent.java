/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.motd;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.CollectionUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.List;

@ComponentInformation(friendlyName = "MOTD", desc = "Provides random server ping MOTD")
public class MOTDComponent extends BukkitComponent implements Listener {
    private LocalConfiguration config;
    private String lastUpdateTime;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        loadUpdateTime();

        CommandBook.registerEvents(this);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("motd.lines")
        public List<String> motdLines = List.of(
            "We Build! We Fight! We Live!"
        );
    }

    private void loadUpdateTime() {
        Path file = Paths.get(CommandBook.inst().getDataFolder().getPath(), "last-update.txt");

        try {
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

            SimpleDateFormat dateFormat = new SimpleDateFormat("M/d ha");
            lastUpdateTime = dateFormat.format(attr.lastModifiedTime().toMillis());
        } catch (IOException e) {
            lastUpdateTime = "Unknown";
        }
    }

    private String buildMOTD() {
        return CollectionUtil.getElement(config.motdLines) + " - Updated " + lastUpdateTime;
    }

    @EventHandler
    public void onMOTDRequest(ServerListPingEvent event) {
        event.setMotd(buildMOTD());
    }
}
