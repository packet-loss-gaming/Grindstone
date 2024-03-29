/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import java.util.HashMap;
import java.util.TreeMap;

public class RomanNumeralUtil {
    private final static TreeMap<Integer, String> INT_TO_CHAR = new TreeMap<>();

    static {
        INT_TO_CHAR.put(1000, "M");
        INT_TO_CHAR.put(900, "CM");
        INT_TO_CHAR.put(500, "D");
        INT_TO_CHAR.put(400, "CD");
        INT_TO_CHAR.put(100, "C");
        INT_TO_CHAR.put(90, "XC");
        INT_TO_CHAR.put(50, "L");
        INT_TO_CHAR.put(40, "XL");
        INT_TO_CHAR.put(10, "X");
        INT_TO_CHAR.put(9, "IX");
        INT_TO_CHAR.put(5, "V");
        INT_TO_CHAR.put(4, "IV");
        INT_TO_CHAR.put(1, "I");
    }

    private final static HashMap<Character, Integer> CHAR_TO_INT = new HashMap<>();

    static {
        CHAR_TO_INT.put('M', 1000);
        CHAR_TO_INT.put('D', 500);
        CHAR_TO_INT.put('C', 100);
        CHAR_TO_INT.put('L', 50);
        CHAR_TO_INT.put('X', 10);
        CHAR_TO_INT.put('V', 5);
        CHAR_TO_INT.put('I', 1);
    }

    public static String toRoman(int number) {
        int nearestKey = INT_TO_CHAR.floorKey(number);
        if (nearestKey == number) {
            return INT_TO_CHAR.get(number);
        }
        return INT_TO_CHAR.get(nearestKey) + toRoman(number - nearestKey);
    }

    public static int fromRoman(String roman) {
        int number = 0;
        for (int i = 0; i < roman.length(); ++i) {
            char curChar = roman.charAt(i);
            int curCharValue = CHAR_TO_INT.get(curChar);

            // Handle special case, we're at the end of the string
            if (i == roman.length() - 1) {
                number += curCharValue;
                continue;
            }

            char nextChar = roman.charAt(i + 1);
            int nextCharValue = CHAR_TO_INT.get(nextChar);

            if (curCharValue < nextCharValue) {
                number -= curCharValue;
            } else {
                number += curCharValue;
            }
        }
        return number;
    }
}
