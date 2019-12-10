package gg.packetloss.grindstone.city.engine.skywars;

import org.bukkit.Color;

import java.util.Arrays;

public enum SkyWarsTeam {
    FREE_FOR_ALL(Color.WHITE),
    RED(Color.RED),
    BLUE(Color.BLUE),
    GREEN(Color.GREEN),
    ORANGE(Color.ORANGE),
    MAROON(Color.MAROON),
    PURPLE(Color.PURPLE),
    GRAY(Color.GRAY),
    YELLOW(Color.YELLOW),
    BLACK(Color.BLACK);

    private final Color color;

    private SkyWarsTeam(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public static SkyWarsTeam[] all() {
        return values();
    }

    public static SkyWarsTeam[] normal() {
        SkyWarsTeam[] all = all();
        return Arrays.copyOfRange(all, 1, all.length);
    }
}
