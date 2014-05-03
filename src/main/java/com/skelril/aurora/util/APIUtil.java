/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.skelril.aurora.exceptions.UnknownPluginException;
import org.bukkit.plugin.Plugin;

public class APIUtil {
    public static WorldGuardPlugin getWorldGuard() throws UnknownPluginException {
        Plugin plugin = CommandBook.server().getPluginManager().getPlugin("WorldGuard");
        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }
        return (WorldGuardPlugin) plugin;
    }
}
