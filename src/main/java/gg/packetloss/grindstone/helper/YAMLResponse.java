/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.helper;

import gg.packetloss.bukkittext.Text;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class YAMLResponse implements Response {

    private final Pattern pattern;
    private final List<String> response;

    public YAMLResponse(Pattern pattern, List<String> response) {
        this.pattern = pattern;
        this.response = response;
    }

    public String getPattern() {
        return pattern.pattern();
    }

    public List<String> getResponse() {
        return Collections.unmodifiableList(response);
    }

    @Override
    public boolean accept(Player player, Collection<Player> recipients, String string) {
        if (!pattern.matcher(string).matches()) {
            return false;
        }

        recipients.forEach((recipient) -> {
            recipient.sendMessage(Text.of(getNamePlate(), "Hey, ", player.getName(), "!").build());
            response.forEach(msg -> {
                String finalMessage = msg
                        .replaceAll("%player%", player.getName())
                        .replaceAll("%world%", player.getWorld().getName());
                recipient.sendMessage(Text.of(getNamePlate(), finalMessage).build());
            });
        });
        return true;
    }
}
