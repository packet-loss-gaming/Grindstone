package gg.packetloss.grindstone.items.custom;

import org.bukkit.ChatColor;

public enum WeaponFamily {
    MASTER(ChatColor.DARK_PURPLE, "Master"),
    FEAR(ChatColor.DARK_RED, "Fear"),
    UNLEASHED(ChatColor.DARK_PURPLE, "Unleashed");

    private final ChatColor color;
    private final String properName;

    WeaponFamily(ChatColor color, String properName) {
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
