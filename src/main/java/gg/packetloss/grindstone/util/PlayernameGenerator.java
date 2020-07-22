/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import java.util.List;
import java.util.Random;

public class PlayernameGenerator {
    private static final List<String> PRIMARY = List.of(
            "Toxic", "Mad", "Zappy", "Upsidedown",
            "Happy", "Crazy", "Sideways", "Looney",
            "Loopy", "Lax", "Mr_Piggy"
    );

    private static final List<String> SECONDARY = List.of(
            "Duckling", "Cow", "Zim", "Fishman", "Pizzaboy", "Sonic",
            "Clown", "Slayer", "Mobslayer", "Creeper", "Pigman",
            "Steve", "Enderman", "Silverfish", "Ingot", "Bunny"
    );

    private Random rand;

    public PlayernameGenerator(long seed) {
        rand = new Random(seed);
    }

    public String generate() {
        return PRIMARY.get(rand.nextInt(PRIMARY.size())) + "_" + SECONDARY.get(rand.nextInt(SECONDARY.size()));
    }
}
