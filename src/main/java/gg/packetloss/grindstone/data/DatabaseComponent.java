/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.data;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

@ComponentInformation(friendlyName = "Database", desc = "MySQL database handler.")
public class DatabaseComponent extends BukkitComponent {
    private final PlayerDatabase playerDatabase = new PlayerDatabase();
    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        updateHandle();
        CommandBook.registerEvents(new PlayerDatabaseListener(playerDatabase));
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
        updateHandle();
    }

    public class LocalConfiguration extends ConfigurationBase {
        @Setting("database")
        public String database = "";
        @Setting("username")
        public String username = "";
        @Setting("password")
        public String password = "";
    }

    public void updateHandle() {
        SQLHandle.setDatabase(config.database);
        SQLHandle.setUsername(config.username);
        SQLHandle.setPassword(config.password);
    }
}
