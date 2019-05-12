/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import java.util.Random;

public class ChanceUtil {

    private static Random r = new Random(System.currentTimeMillis());

    public static int getRandom(int highestValue) {

        return highestValue == 0 ? 1 : highestValue < 0 ? (r.nextInt(highestValue * -1) + 1) * -1 : r.nextInt(highestValue) + 1;
    }

    public static int getRangedRandom(int lowestValue, int highestValue) {

        if (lowestValue == highestValue) return lowestValue;
        return lowestValue + getRandom((highestValue + 1) - lowestValue) - 1;
    }

    public static double getRandom(double highestValue) {

        if (highestValue < 0) {
            return (r.nextDouble() * (highestValue * -1)) * -1;
        }
        return (r.nextDouble() * (highestValue - 1)) + 1;
    }

    public static double getRangedRandom(double lowestValue, double highestValue) {

        if (lowestValue == highestValue) return lowestValue;
        return lowestValue + getRandom((highestValue + 1) - lowestValue) - 1;
    }

    public static boolean getChance(Number number) {
        return getChance(number.intValue());
    }

    public static boolean getChance(int outOf) {

        return getChance(1, outOf);
    }

    public static boolean getChance(int chance, int outOf) {

        return getRandom(outOf) <= chance;
    }

}