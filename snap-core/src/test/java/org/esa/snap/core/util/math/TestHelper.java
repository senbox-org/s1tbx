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
package org.esa.snap.core.util.math;

import org.esa.snap.core.datamodel.ProductData;

public class TestHelper {

    public static final int BYTE = ProductData.TYPE_INT8;
    public static final int UBYTE = ProductData.TYPE_UINT8;
    public static final int SHORT = ProductData.TYPE_INT16;
    public static final int USHORT = ProductData.TYPE_UINT16;
    public static final int INT = ProductData.TYPE_INT32;
    public static final int UINT = ProductData.TYPE_UINT32;
    public static final int FLOAT = ProductData.TYPE_FLOAT32;
    public static final int DOUBLE = ProductData.TYPE_FLOAT64;

    public static byte[] createUBytes(short[] shorts) {
        final byte[] bytes = new byte[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            bytes[i] = (byte) shorts[i];
        }
        return bytes;
    }

    public static short[] createUShorts(int[] ints) {
        final short[] shorts = new short[ints.length];
        for (int i = 0; i < ints.length; i++) {
            shorts[i] = (short) ints[i];
        }
        return shorts;
    }

    public static int[] createUInts(long[] longs) {
        final int[] ints = new int[longs.length];
        for (int i = 0; i < longs.length; i++) {
            ints[i] = (int) longs[i];
        }
        return ints;
    }

    public static DoubleList createArray(final int[] ints) {
        final DoubleList doubleArray = new DoubleList() {
            public int getSize() {
                return ints.length;
            }

            public double getDouble(int index) {
                return ints[index];
            }
        };
        return doubleArray;
    }

    public static ProductData noDataValue(final int type, final double value) {
        final ProductData noDataValue = ProductData.createInstance(type);
        noDataValue.setElemDouble(value);
        return noDataValue;
    }
}
