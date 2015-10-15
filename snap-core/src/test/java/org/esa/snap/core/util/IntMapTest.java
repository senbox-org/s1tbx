/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.util;

import junit.framework.TestCase;

public class IntMapTest extends TestCase {

    public void testIntMap() {
        IntMap intMap = new IntMap(-10, 110);

        assertEquals(0, intMap.getSize());

        intMap.putValue(-10000, -4);
        intMap.putValue(-1000, -3);
        intMap.putValue(-100, -2);
        intMap.putValue(-10, -1);
        intMap.putValue(1, 0);
        intMap.putValue(10, 1);
        intMap.putValue(100, 2);
        intMap.putValue(1000, 3);
        intMap.putValue(10000, 4);

        assertEquals(9, intMap.getSize());

        assertEquals(-4, intMap.getValue(-10000));
        assertEquals(-3, intMap.getValue(-1000));
        assertEquals(-2, intMap.getValue(-100));
        assertEquals(-1, intMap.getValue(-10));
        assertEquals(0, intMap.getValue(1));
        assertEquals(1, intMap.getValue(10));
        assertEquals(2, intMap.getValue(100));
        assertEquals(3, intMap.getValue(1000));
        assertEquals(4, intMap.getValue(10000));

        assertEquals(IntMap.NULL, intMap.getValue(-10000 + 1));
        assertEquals(IntMap.NULL, intMap.getValue(-1000 + 1));
        assertEquals(IntMap.NULL, intMap.getValue(-100 + 1));
        assertEquals(IntMap.NULL, intMap.getValue(-10 + 1));
        assertEquals(IntMap.NULL, intMap.getValue(1 - 1));
        assertEquals(IntMap.NULL, intMap.getValue(10 - 1));
        assertEquals(IntMap.NULL, intMap.getValue(100 - 1));
        assertEquals(IntMap.NULL, intMap.getValue(1000 - 1));
        assertEquals(IntMap.NULL, intMap.getValue(10000 - 1));

        intMap.removeValue(1);
        assertEquals(8, intMap.getSize());
        assertEquals(IntMap.NULL, intMap.getValue(1));

        intMap.removeValue(1000);
        assertEquals(7, intMap.getSize());
        assertEquals(IntMap.NULL, intMap.getValue(1000));

        intMap.removeValue(10000 - 1);
        assertEquals(7, intMap.getSize());

        try {
            intMap.putValue(1, IntMap.NULL);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testSequentialAccess() {
        IntMap intMap = new IntMap();
        assertEquals(0, intMap.getSize());
        for (int i = -10000; i <= 10000; i++) {
            intMap.putValue(i, i + 99);
        }
        for (int i = -10000; i <= 10000; i++) {
            assertEquals(i + 99, intMap.getValue(i));
        }
        assertEquals(20001, intMap.getSize());
        for (int i = -10000; i <= 10000; i++) {
            intMap.removeValue(i);
        }
        assertEquals(0, intMap.getSize());
    }

    public void testKeys() {
        IntMap intMap = new IntMap(10, 100);
        intMap.putValue(234, 5);
        intMap.putValue(2, 9);
        intMap.putValue(534, 1);
        intMap.putValue(22, 43);
        intMap.putValue(-8, 8232);
        final int[] keys = intMap.getKeys();
        assertEquals(5, keys.length);
        assertEquals(-8, keys[0]);
        assertEquals(2, keys[1]);
        assertEquals(22, keys[2]);
        assertEquals(234, keys[3]);
        assertEquals(534, keys[4]);
    }

    public void testPairs() {
        IntMap intMap = new IntMap(10, 100);
        intMap.putValue(234, 5);
        intMap.putValue(2, 9);
        intMap.putValue(534, 1);
        intMap.putValue(22, 43);
        intMap.putValue(-8, 8232);
        final int[][] pairs = intMap.getPairs();
        assertEquals(5, pairs.length);
        assertEquals(-8, pairs[0][0]);
        assertEquals(2, pairs[1][0]);
        assertEquals(22, pairs[2][0]);
        assertEquals(234, pairs[3][0]);
        assertEquals(534, pairs[4][0]);
        assertEquals(8232, pairs[0][1]);
        assertEquals(9, pairs[1][1]);
        assertEquals(43, pairs[2][1]);
        assertEquals(5, pairs[3][1]);
        assertEquals(1, pairs[4][1]);
    }

    public void testRanges() {
        IntMap intMap = new IntMap(10, 100);
        intMap.putValue(234, 5);
        intMap.putValue(2, 9);
        intMap.putValue(534, 1);
        intMap.putValue(22, 43);
        intMap.putValue(-8, 8232);
        final int[][] ranges = intMap.getRanges();
        assertEquals(2, ranges.length);
        assertEquals(-8, ranges[0][0]);
        assertEquals(534, ranges[0][1]);
        assertEquals(1, ranges[1][0]);
        assertEquals(8232, ranges[1][1]);
    }


    public void testClone() {
        IntMap intMap = new IntMap(10, 100);
        intMap.putValue(234, 5);
        intMap.putValue(2, 9);
        intMap.putValue(534, 1);
        intMap.putValue(22, 43);
        intMap.putValue(-8, 8232);
        final int[][] pairs = ((IntMap)intMap.clone()).getPairs();
        assertEquals(5, pairs.length);
        assertEquals(-8, pairs[0][0]);
        assertEquals(2, pairs[1][0]);
        assertEquals(22, pairs[2][0]);
        assertEquals(234, pairs[3][0]);
        assertEquals(534, pairs[4][0]);
        assertEquals(8232, pairs[0][1]);
        assertEquals(9, pairs[1][1]);
        assertEquals(43, pairs[2][1]);
        assertEquals(5, pairs[3][1]);
        assertEquals(1, pairs[4][1]);
    }
}
