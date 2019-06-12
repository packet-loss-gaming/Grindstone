/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Impersonal", desc = "It's just business.")
@Depend(plugins = {"WorldGuard"})
public class ImpersonalComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private WorldGuardPlugin WG;

    @Override
    public void enable() {

        setUpWG();
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    /*
     * @returns true if the block is allowed to be there
     */
    public boolean check(Block block, boolean breakIt) {

        ApplicableRegionSet ars = WG.getGlobalRegionManager().get(block.getWorld())
                .getApplicableRegions(block.getLocation());

        if (ars.size() < 1) return false;
        for (ProtectedRegion ar : ars) {
            if (ar.getId().endsWith("-house")) {
                if (breakIt) block.breakNaturally();
                return false;
            }
        }
        return true;
    }

    private void setUpWG() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) return;

        WG = (WorldGuardPlugin) plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSacrifice(PlayerSacrificeItemEvent event) {

        if (!check(event.getBlock(), false)) {
            event.setCancelled(true);
        }
    }
}
