/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import gg.packetloss.grindstone.util.ChanceUtil;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class ChanceTest {

    @Test
    public void testInteger() {
        ChanceUtil.reseedWithRandom(new Random(1));

        int x = 100;

        int min = x;
        int max = 1;

        for (int i = 0; i < x * 100; ++i) {

            int result = ChanceUtil.getRandom(x);

            min = Math.min(min, result);
            max = Math.max(max, result);
        }

        assertTrue("Expected min of: " + 1 + ", Received: " + min, min == 1);
        assertTrue("Expected max of: " + x + ", Received: " + max, max == x);
    }

    @Test
    public void testDouble() {
        ChanceUtil.reseedWithRandom(new Random(1));

        double x = 100.2;

        double min = x;
        double max = 1;

        for (int i = 0; i < x * 100; ++i) {

            double result = ChanceUtil.getRandom(x);

            min = Math.min(min, result);
            max = Math.max(max, result);
        }

        // This test is somewhat inaccurate since it would take ages to get a perfect decimal
        min = Math.round(min * 10.0) / 10.0;
        max = Math.round(max * 10.0) / 10.0;

        assertTrue("Expected min of: " + 1 + ", Received: " + min, min == 1);
        assertTrue("Expected max of: " + x + ", Received: " + max, max == x);
    }

    @Test
    public void testRangedRandomEquality() {
        ChanceUtil.reseedWithRandom(new Random(1));

        int x = 10;
        int result = ChanceUtil.getRangedRandom(x, x);

        assertTrue("Gave duplicate x: " + x + ", Received: " + result, result == x);
    }

    @Test
    public void testIntegerRangedRandomMinAndMax() {
        ChanceUtil.reseedWithRandom(new Random(1));

        final int oMin = 1;
        final int oMax = 10;

        int min = oMax;
        int max = oMin;

        for (int i = 0; i < oMax * 100; ++i) {

            int result = ChanceUtil.getRangedRandom(oMin, oMax);

            min = Math.min(min, result);
            max = Math.max(max, result);
        }

        assertTrue("Expected min of: " + oMin + ", Received: " + min, min == oMin);
        assertTrue("Expected max of: " + oMax + ", Received: " + max, max == oMax);
    }

    @Test
    public void testDoubleRangedRandomMinAndMax() {
        ChanceUtil.reseedWithRandom(new Random(1));

        final double oMin = -.7;
        final double oMax = 12.3;

        double min = oMax;
        double max = oMin;

        for (int i = 0; i < oMax * 100; ++i) {

            double result = ChanceUtil.getRangedRandom(oMin, oMax);

            min = Math.min(min, result);
            max = Math.max(max, result);
        }

        // This test is somewhat inaccurate since it would take ages to get a perfect decimal
        min = Math.round(min * 10.0) / 10.0;
        max = Math.round(max * 10.0) / 10.0;

        assertTrue("Expected min of: " + oMin + ", Received: " + min, min == oMin);
        assertTrue("Expected max of: " + oMax + ", Received: " + max, max == oMax);
    }
}
