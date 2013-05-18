package com.skelril.aurora.util.timer;

public class TimerUtil {

    public static boolean matchesFilter(int entry, int min, int divisible) {

        return entry > 0 && entry % divisible == 0 || entry <= min && entry > 0;
    }
}
