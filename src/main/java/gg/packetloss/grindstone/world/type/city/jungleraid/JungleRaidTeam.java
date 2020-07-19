package gg.packetloss.grindstone.world.type.city.jungleraid;

import org.bukkit.Color;

import java.util.Arrays;

public enum JungleRaidTeam {
    FREE_FOR_ALL(Color.WHITE),
    RED(Color.RED),
    BLUE(Color.BLUE);

    private final Color color;

    private JungleRaidTeam(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public static JungleRaidTeam[] all() {
        return values();
    }

    public static JungleRaidTeam[] normal() {
        JungleRaidTeam[] all = all();
        return Arrays.copyOfRange(all, 1, all.length);
    }
}
