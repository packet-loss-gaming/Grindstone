package com.skelril.aurora.util;

import java.util.Random;

/**
 * @author Turtle9598
 */
public class ChanceUtil {

    private static Random r = new Random(1374633257);

    public static int getRandom(int highestValue) {

        return highestValue == 0 ? 1 : highestValue < 0 ? (r.nextInt(highestValue * -1) + 1) * -1 : r.nextInt(highestValue) + 1;
    }

    public static int getRangedRandom(int lowestValue, int highestValue) {

        if (lowestValue == highestValue) return lowestValue;
        return lowestValue + getRandom(highestValue - lowestValue) - 1;
    }

    public static double getRandom(double highestValue) {

        if (highestValue < 0) {
            return (r.nextDouble() * (highestValue * -1) + 1) * -1;
        }
        return (r.nextDouble() * highestValue) + 1;
    }

    public static double getRangedRandom(double lowestValue, double highestValue) {

        if (lowestValue == highestValue) return lowestValue;
        return lowestValue + getRandom(highestValue - lowestValue) - 1;
    }

    public static boolean getChance(int outOf) {

        return getChance(1, outOf);
    }

    public static boolean getChance(int chance, int outOf) {

        return getRandom(outOf) <= chance;
    }

}
