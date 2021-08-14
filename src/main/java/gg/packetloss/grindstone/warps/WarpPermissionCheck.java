/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import gg.packetloss.grindstone.util.NamespaceConstants;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WarpPermissionCheck {
    private WarpPermissionCheck() { }

    public static boolean hasAccessToNamespace(CommandSender sender, UUID namespace) {
        if (namespace.equals(NamespaceConstants.GLOBAL)) {
            if (sender.hasPermission("aurora.warp.access.global")) {
                return true;
            }
        }

        if (sender instanceof Player player && player.getUniqueId().equals(namespace)) {
            if (sender.hasPermission("aurora.warp.access.self")) {
                return true;
            }
        }

        if (sender.hasPermission("aurora.warp.access." + namespace)) {
            return true;
        }

        return false;
    }
}
