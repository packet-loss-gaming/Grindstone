package com.skelril.aurora;

import com.skelril.aurora.util.ChanceUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * User: Wyatt Childers
 * Date: 11/16/13
 */
public class ChanceTest {

    @Test
    public void testInteger() {

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

        int x = 10;
        int result = ChanceUtil.getRangedRandom(x, x);

        assertTrue("Gave duplicate x: " + x + ", Received: " + result, result == x);
    }

    @Test
    public void testIntegerRangedRandomMinAndMax() {

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
