package com.skelril.aurora.util;

import java.util.Calendar;

/**
 * User: Wyatt Childers
 * Date: 9/20/13
 */
public class TimeUtil {

    private static Calendar calendar = Calendar.getInstance();

    public static long getTicksTillHour() {

        Calendar localCalendar = Calendar.getInstance();
        long returnValue;

        localCalendar.set(Calendar.MINUTE, 0);
        localCalendar.add(Calendar.HOUR_OF_DAY, 1);

        returnValue = localCalendar.getTimeInMillis() - calendar.getTimeInMillis();
        returnValue = (returnValue / 1000) * 20; // To Ticks

        return returnValue;
    }

    public static long getTicksTill(int hour) {

        return getTicksTill(hour, -1);
    }

    public static long getTicksTill(int hour, int dayofweek) {

        Calendar localCalendar = Calendar.getInstance();
        long returnValue;

        localCalendar.set(Calendar.MINUTE, 0);

        while (localCalendar.get(Calendar.HOUR_OF_DAY) != hour) {
            localCalendar.add(Calendar.HOUR_OF_DAY, 1);
        }
        if (dayofweek != -1) {
            while (localCalendar.get(Calendar.DAY_OF_WEEK) != dayofweek) {
                localCalendar.add(Calendar.DAY_OF_WEEK, 1);
            }
        }

        returnValue = localCalendar.getTimeInMillis() - calendar.getTimeInMillis();
        returnValue = (returnValue / 1000) * 20; // To Ticks

        return returnValue;
    }
}