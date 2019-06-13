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
import org.bukkit.*;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "World Border", desc = "A World Border enforcer")
@Depend(components = {AdminComponent.class})
public class WorldBorderComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    AdminComponent adminComponent;

    private LocalConfiguration config;
    private World world;

    public void setWorldBoarder() {
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setSize(config.size);
        worldBorder.setCenter(world.getSpawnLocation());
    }

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        world = Bukkit.getWorld("City");

       setWorldBoarder();
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
        setWorldBoarder();

    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("size")
        public double size = 3250;
    }
}