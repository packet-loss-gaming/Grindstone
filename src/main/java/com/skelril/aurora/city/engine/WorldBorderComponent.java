package com.skelril.aurora.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "World Border", desc = "A World Border enforcer")
public class WorldBorderComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

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
            int z = pLoc.getBlockZ();
            int bz = player.getWorld().equals(world) ? config.maxZ : 10000;
            int sz = player.getWorld().equals(world) ? config.minZ : -10000;

            if (x > bx) {
                pLoc.setX(bx);
            } else if (x < sx) {
                pLoc.setX(sx);
            }

            if (z > bz) {
                pLoc.setZ(bz);
            } else if (z < sz) {
                pLoc.setZ(sz);
            }

            if (!pLoc.equals(origin)) {
                player.teleport(pLoc);
                ChatUtil.sendNotice(player, "You have reached the end of the accessible area of this world.");
            }
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("max-x")
        public int maxX = 100;
        @Setting("min-x")
        public int minX = -100;
        @Setting("max-z")
        public int maxZ = 100;
        @Setting("min-z")
        public int minZ = -100;
    }
}