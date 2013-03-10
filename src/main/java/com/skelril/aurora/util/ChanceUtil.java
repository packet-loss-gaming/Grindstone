package com.skelril.aurora.util;

/**
 * @author Turtle9598
 */
public class ChanceUtil {

    public static int getRandom(int highestValue) {

        if (highestValue < 0) {
            return (int) ((Math.random() * (highestValue * -1) + 1) * -1);
        }
        return (int) ((Math.random() * highestValue) + 1);
    }

    public static int getRangedRandom(int lowestValue, int highestValue) {

        if (lowestValue == highestValue) return lowestValue;
        return lowestValue + getRandom(highestValue - lowestValue) - 1;
    }

    public static double getRandom(double highestValue) {

        if (highestValue < 0) {
            return (Math.random() * (highestValue * -1) + 1) * -1;
        }
        return (Math.random() * highestValue) + 1;
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
