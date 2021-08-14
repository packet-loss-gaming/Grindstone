/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.backup;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.mcmailbox.MailBoxUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ComponentInformation(friendlyName = "World Backup", desc = "World backup utility.")
public class WorldBackupComponent extends BukkitComponent implements Listener {
    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    private List<UUID> previousAutoSave = new ArrayList<>();

    @EventHandler
    public void onMCMailBoxUpdate(MailBoxUpdateEvent event) {
        if (!event.getKey().equals("BACKUP_IN_PROGRESS")) {
            return;
        }

        if (event.isNowTrue()) {
            Bukkit.broadcastMessage(ChatColor.GRAY + "[BACKUP BUDDY] World backup in progress...");
            for (World world : Bukkit.getWorlds()) {
                // We don't care about worlds that are already not auto saving
                if (!world.isAutoSave()) {
                    continue;
                }

                previousAutoSave.add(world.getUID());
                world.setAutoSave(false);
            }
        } else {
            // Restore previous world saving states
            for (UUID worldID : previousAutoSave) {
                World world = Bukkit.getWorld(worldID);
                if (world == null) {
                    continue;
                }
                world.setAutoSave(true);
            }
            previousAutoSave.clear();

            Bukkit.broadcastMessage(ChatColor.GRAY + "[BACKUP BUDDY] World backup completed.");
        }
    }
}
