/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "World Border", desc = "A World Border enforcer")
@Depend(components = {AdminComponent.class})
public class WorldBorderComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    AdminComponent adminComponent;

    private LocalConfiguration config;
    private World world;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());

        this.world = Bukkit.getWorld("City");
        server.getScheduler().runTaskTimer(inst, this, 0, 20 * 8);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    @Override
    public void run() {

        Location pLoc = new Location(world, 0, 0, 0);
        Location origin;

        for (Player player : server.getOnlinePlayers()) {

            pLoc = player.getLocation(pLoc);
            origin = pLoc.clone();

            int x = pLoc.getBlockX();
            int bx = player.getWorld().equals(world) ? config.maxX : 10000;
            int sx = player.getWorld().equals(world) ? config.minX : -10000;
            int y = pLoc.getBlockY();
            int by = config.maxY;
            int z = pLoc.getBlockZ();
            int bz = player.getWorld().equals(world) ? config.maxZ : 10000;
            int sz = player.getWorld().equals(world) ? config.minZ : -10000;

            if (x > bx) {
                pLoc.setX(bx);
            } else if (x < sx) {
                pLoc.setX(sx);
            }

            if (y > by && player.getAllowFlight() && !adminComponent.isAdmin(player)) {
                pLoc.setY(by);
            }

            if (z > bz) {
                pLoc.setZ(bz);
            } else if (z < sz) {
                pLoc.setZ(sz);
            }

            if (!pLoc.equals(origin)) {
                Entity v = player.getVehicle();
                if (v == null) {
                    player.teleport(pLoc);
                } else {
                    v.eject();
                    v.teleport(pLoc);
                    player.teleport(v);
                    v.setPassenger(player);
                }
                ChatUtil.sendNotice(player, "You have reached the end of the accessible area of this world.");
            }
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("max.x")
        public int maxX = 100;
        @Setting("min.x")
        public int minX = -100;
        @Setting("max.y")
        public int maxY = 300;
        @Setting("max.z")
        public int maxZ = 100;
        @Setting("min.z")
        public int minZ = -100;
    }
}