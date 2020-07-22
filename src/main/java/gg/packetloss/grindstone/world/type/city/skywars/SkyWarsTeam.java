/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.skywars;

import org.bukkit.Color;

import java.util.Arrays;

public enum SkyWarsTeam {
    FREE_FOR_ALL(Color.WHITE),
    RED(Color.RED),
    BLUE(Color.BLUE),
    GREEN(Color.GREEN),
    ORANGE(Color.ORANGE),
    LIME(Color.LIME),
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
