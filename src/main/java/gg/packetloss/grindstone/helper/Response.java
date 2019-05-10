/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.helper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Response {

    private final Pattern pattern;
    private final List<String> response;

    public Response(Pattern pattern, List<String> response) {
        this.pattern = pattern;
        this.response = response;
    }

    public String getPattern() {
        return pattern.pattern();
    }

    public List<String> getResponse() {
        return Collections.unmodifiableList(response);
    }

    public boolean accept(Player player, String string) {
        if (!pattern.matcher(string).matches()) return false;

        Bukkit.broadcastMessage(ChatColor.YELLOW + "[Auto Reply] @" + player.getName());
        response.forEach(msg -> {
            String finalMessage = msg
                    .replaceAll("%player%", player.getName())
                    .replaceAll("%world%", player.getWorld().getName());
            Bukkit.broadcastMessage("   " + ChatColor.YELLOW + finalMessage);
        });
        return true;
    }
}
