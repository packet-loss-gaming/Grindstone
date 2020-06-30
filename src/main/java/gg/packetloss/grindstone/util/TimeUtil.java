/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;
import java.util.function.Predicate;

public class TimeUtil {

    private static Calendar calendar = Calendar.getInstance();

    /**
     * Gets the ticks till the start of the next hour
     *
     * @return the number of ticks till the next hour
     */
    public static long getTicksTillHour() {

        Calendar localCalendar = Calendar.getInstance();
        long returnValue;

        localCalendar.set(Calendar.MINUTE, 0);
        localCalendar.add(Calendar.HOUR_OF_DAY, 1);

        returnValue = localCalendar.getTimeInMillis() - calendar.getTimeInMillis();
        returnValue = (returnValue / 1000) * 20; // To Ticks

        return returnValue;
    }

    /**
     * Gets the ticks till a given base 24 hour
     *
     * @param hour The hour, for example 13 is 1 P.M.
     * @return the number of ticks till the given time
     */
    public static long getTicksTill(int hour) {

        return getTicksTill(hour, -1);
    }

    /**
     * Gets the ticks till a given base 24 hour on a day of the week
     *
     * @param hour      The hour, for example 13 is 1 P.M.
     * @param dayofweek The day, for example 7 is Saturday
     * @return the number of ticks till the given time
     */
    public static long getTicksTill(int hour, int dayofweek) {
        Calendar localCalendar = Calendar.getInstance();
        long returnValue;

        localCalendar.add(Calendar.MINUTE, 60 - localCalendar.get(Calendar.MINUTE));

        while (localCalendar.get(Calendar.HOUR_OF_DAY) != hour) {
            localCalendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        if (dayofweek != -1) {
            while (localCalendar.get(Calendar.DAY_OF_WEEK) != dayofweek) {
                localCalendar.add(Calendar.DAY_OF_WEEK, 1);
            }
        }

        returnValue = localCalendar.getTimeInMillis() - calendar.getTimeInMillis();
        returnValue /= 1000; // To seconds
        returnValue *= 20; // To Ticks

        return returnValue;
    }

    public static long convertSecondsToTicks(int seconds) {
        return seconds * 20;
    }

    public static long convertMinutesToTicks(int minutes) {
        return convertSecondsToTicks(minutes * 60);
    }

    public static long convertHoursToTicks(int hours) {
        return convertMinutesToTicks(hours * 60);
    }

    public static int getNextHour(Predicate<Integer> test) {
        int hour = LocalTime.now().getHour();

        do {
            hour = (hour + 1) % 23;
        } while (!test.test(hour));

        return hour;
    }

    public static String getPrettyTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);

        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        String ampm = calendar.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";

        String minuteString;
        if (minute < 10) {
            minuteString = "0" + minute;
        } else {
            minuteString = String.valueOf(minute);
        }

        return hour + ":" + minuteString + " " + ampm;
    }

    private static final DateTimeFormatter PRETTY_END_DATE_FORMATTER = DateTimeFormatter.ofPattern(
            "MMMM d yyyy 'at' h':'mma"
    ).withLocale(Locale.US).withZone(ZoneId.systemDefault());

    public static String getPrettyEndDate(long time) {
        StringBuilder builder = new StringBuilder();

        if (time != 0) {
            builder.append("until ");

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);

            builder.append(PRETTY_END_DATE_FORMATTER.format(calendar.toInstant()));
        } else {
            builder.append("indefinitely");
        }

        return builder.toString();
    }
}
