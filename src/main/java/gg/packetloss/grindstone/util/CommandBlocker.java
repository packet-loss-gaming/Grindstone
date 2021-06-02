/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandBlocker {
    private List<String> whitelist;

    public CommandBlocker(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    public boolean allowsCommand(String command) {
        command = command.toLowerCase();

        for (String cmd : whitelist) {
            if (command.startsWith("/" + cmd)) {
                return true;
            }
        }

        return false;
    }

    public void handle(PlayerCommandPreprocessEvent event) {
        if (!allowsCommand(event.getMessage())) {
            ChatUtil.sendError(event.getPlayer(), "Command blocked.");
            event.setCancelled(true);
        }
    }
}
