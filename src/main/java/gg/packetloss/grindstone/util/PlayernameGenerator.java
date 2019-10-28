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
