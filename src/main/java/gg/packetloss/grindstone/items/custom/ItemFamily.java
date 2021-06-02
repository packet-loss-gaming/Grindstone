/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;

public enum ItemFamily {
    PWNG(ChatColor.DARK_RED, "Pwng"),
    MASTER(ChatColor.DARK_PURPLE, "Master"),
    FEAR(ChatColor.DARK_RED, "Fear"),
    UNLEASHED(ChatColor.DARK_PURPLE, "Unleashed"),
    PARTY_BOX(ChatColor.GOLD, "Party Box");

    private final ChatColor color;
    private final String properName;

    ItemFamily(ChatColor color, String properName) {
        this.color = color;
        this.properName = properName;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getProperName() {
        return properName;
    }

    public String getPrefix() {
        return getColor() + getProperName();
    }
}
