/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import org.bukkit.plugin.Plugin;

public class APIUtil {
    public static WorldGuardPlugin getWorldGuard() throws UnknownPluginException {
        Plugin plugin = CommandBook.server().getPluginManager().getPlugin("WorldGuard");
        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }
        return (WorldGuardPlugin) plugin;
    }
}
