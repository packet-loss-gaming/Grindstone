/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;

public class Tag {
    private ChatColor color;
    private String key;
    private String prop;

    public Tag(ChatColor color, String key, String prop) {
        this.color = color;
        this.key = key;
        this.prop = prop;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getKey() {
        return key;
    }

    public String getProp() {
        return prop;
    }

}
