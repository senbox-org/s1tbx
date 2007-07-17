/*
 * $Id: FlagWrapperTest.java,v 1.1 2007/03/27 12:51:06 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util;

import junit.framework.TestCase;

/**
 * Created by marcoz.
 *
 * @author marcoz
 * @version $Revision: 1.1 $ $Date: 2007/03/27 12:51:06 $
 */
public class FlagWrapperTest extends TestCase {

    public void testIntFlagMethods() {
        int[] flagArray = new int[1];
        FlagWrapper flags = new FlagWrapper.Int(flagArray);
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
            flagArray[0] = 0; // clean flags
            final int bitIndex = indexes[i];
            final int result = results[i];
            flags.set(0, bitIndex);
            assertEquals("i = " + i, result, flagArray[0]);
            assertEquals("i = " + i, true, flags.isSet(0, bitIndex));
        }

        flagArray[0] = 0; // clean flags
        for (int i = 0; i < indexes.length; i++) {
            flags.set(0, indexes[i]);
        }
        assertEquals(-1777647181, flagArray[0]);
        for (int i = 0; i < 32; i++) {
            boolean expected = false;
            for (int j = 0; j < indexes.length; j++) {
                if (i == indexes[j]) {
                    expected = true;
                    break;
                }
            }
            assertEquals("i = " + i, expected, flags.isSet(0, i));
        }
    }

    public void test64BitFlagMethods() {
        long[] flagArray = new long[1];
        FlagWrapper flags = new FlagWrapper.Long(flagArray);
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
            flagArray[0] = 0; // clean flags
            final int bitIndex = indexes[i];
            final long result = results[i];
            flags.set(0, bitIndex);
            assertEquals("i = " + i, result, flagArray[0]);
            assertEquals("i = " + i, true, flags.isSet(0, bitIndex));
        }

        flagArray[0] = 0; // clean flags
        for (int i = 0; i < indexes.length; i++) {
            flags.set(0, indexes[i]);
        }
        assertEquals(3458768918377087363L, flagArray[0]);
        for (int i = 0; i < 64; i++) {
            boolean expected = false;
            for (int j = 0; j < indexes.length; j++) {
                if (i == indexes[j]) {
                    expected = true;
                    break;
                }
            }
            assertEquals("i = " + i, expected, flags.isSet(0, i));
        }
    }

}
