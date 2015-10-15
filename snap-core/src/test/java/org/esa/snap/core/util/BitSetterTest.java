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

public class BitSetterTest extends TestCase {

    public void test32BitFlagMethods() {
        int[] indexes = new int[]{
                0, 1, 4, 5,
                7, 8, 14, 16,
                17, 19, 25, 26,
                28, 31
        };
        int[] results = new int[]{
                1, 2, 16, 32,
                128, 256, 16384, 65536,
                131072, 524288, 33554432, 67108864,
                268435456, -2147483648
        };

        for (int i = 0; i < indexes.length; i++) {
            final int index = indexes[i];
            final int result = results[i];
            int flags = BitSetter.setFlag(0, index);
            assertEquals("i = " + i, result, flags);
            assertEquals("i = " + i, true, BitSetter.isFlagSet(flags, index));
        }

        int flags = 0;
        for (int i = 0; i < indexes.length; i++) {
            flags = BitSetter.setFlag(flags, indexes[i]);
        }
        assertEquals(-1777647181, flags);
        for (int i = 0; i < 32; i++) {
            boolean expected = false;
            for (int j = 0; j < indexes.length; j++) {
                if (i == indexes[j]) {
                    expected = true;
                    break;
                }
            }
            assertEquals("i = " + i, expected, BitSetter.isFlagSet(flags, i));
        }
    }

    public void test64BitFlagMethods() {
        int[] indexes = new int[]{
                0, 1, 7, 8,
                14, 16, 17, 26,
                18, 31, 32, 42,
                60, 61
        };
        long[] results = new long[]{
                1L, 2L, 128L, 256L,
                16384L, 65536L, 131072L, 67108864L,
                262144L, 2147483648L, 4294967296L, 4398046511104L,
                1152921504606846976L, 2305843009213693952L
        };

        for (int i = 0; i < indexes.length; i++) {
            final int index = indexes[i];
            final long result = results[i];
            long flags = BitSetter.setFlag(0L, index, true);
            assertEquals("i = " + i, result, flags);
            assertEquals("i = " + i, true, BitSetter.isFlagSet(flags, index));
        }

        long flags = 0;
        for (int i = 0; i < indexes.length; i++) {
            flags = BitSetter.setFlag(flags, indexes[i], true);
        }
        assertEquals(3458768918377087363L, flags);
        for (int i = 0; i < 64; i++) {
            boolean expected = false;
            for (int j = 0; j < indexes.length; j++) {
                if (i == indexes[j]) {
                    expected = true;
                    break;
                }
            }
            assertEquals("i = " + i, expected, BitSetter.isFlagSet(flags, i));
        }
    }

    public void testSetAndReset64BitFlag() throws Exception {
        // test that long flags can be reset properly
        long sampleL = BitSetter.setFlag(0, 2, true);
        assertEquals(4, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 2, false);
        assertEquals(0, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 1, true);
        assertEquals(2, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 1, false);
        assertEquals(0, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 4, true);
        assertEquals(16, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 4, false);
        assertEquals(0, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 3, true);
        assertEquals(8, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 1, true);
        assertEquals(10, sampleL);
        sampleL = BitSetter.setFlag(sampleL, 3, false);
        assertEquals(2, sampleL);
    }

    public void testSetAndReset32BitFlag() throws Exception {
        // test that int flags can be reset properly
        int sampleI = BitSetter.setFlag(0, 2, true);
        assertEquals(4, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 2, false);
        assertEquals(0, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 1, true);
        assertEquals(2, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 1, false);
        assertEquals(0, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 4, true);
        assertEquals(16, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 4, false);
        assertEquals(0, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 3, true);
        assertEquals(8, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 1, true);
        assertEquals(10, sampleI);
        sampleI = BitSetter.setFlag(sampleI, 3, false);
        assertEquals(2, sampleI);
    }

}
