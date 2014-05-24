/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.data;

import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

@ComponentInformation(friendlyName = "Database", desc = "MySQL database handler.")
public class DataBaseComponent extends BukkitComponent {

    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        updateHandle();
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
        MySQLHandle.setDatabase(config.database);
        MySQLHandle.setUsername(config.username);
        MySQLHandle.setPassword(config.password);
    }
}
