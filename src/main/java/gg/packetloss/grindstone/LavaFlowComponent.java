/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

@ComponentInformation(friendlyName = "Lava Flow", desc = "Law flow stopper.")
@Depend(plugins = "WorldGuard")
public class LavaFlowComponent extends BukkitComponent implements Listener {
    private WorldGuardPlugin worldGuard;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {

        if (EnvironmentUtil.isLava(event.getBlock())) {
            try {
                WorldConfiguration wcfg = WorldGuardBridge.getWorldConfig(event.getBlock().getWorld());
                if (wcfg.preventWaterDamage.size() > 0) {
                    if (wcfg.preventWaterDamage.contains(BukkitAdapter.asBlockType(event.getToBlock().getType()).getId())) {
                        event.setCancelled(true);
                    }
                }
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                event.setCancelled(true);
            }
        }
    }
}
