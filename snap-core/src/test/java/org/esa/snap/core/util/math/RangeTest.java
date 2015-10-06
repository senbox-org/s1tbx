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

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;

public class RangeTest extends TestCase {

    private final IndexValidator _validator = new IndexValidator() {
        public boolean validateIndex(int index) {
            return index > 1 && index < 6;
        }
    };

    public void testThatRangeIgnoresNaN() {
        double min = 0;
        double max = 1;
        double x;

        x = Double.NaN;
        assertEquals(false, x >= min && x <= max);
        assertEquals(false, x < min || x > max);
        assertEquals(false, x == x);

        x = Math.log(0.0);
        assertEquals(true, Double.isInfinite(x));
        assertEquals(true, x < 0.0);

        x = Math.log(-1.0);
        assertEquals(true, Double.isNaN(x));

        x = Math.sqrt(-1.0);
        assertEquals(true, Double.isNaN(x));
    }

    public void testComputeRangeByte() {
        byte[] bytes = new byte[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeByte(bytes, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeByte(bytes, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeUByte() {
        final byte[] uBytes = TestHelper.createUBytes(new short[]{2, 253, 4, 5, 250, 7, 8, 9});
        Range range = new Range();

        try {
            Range.computeRangeUByte(uBytes, null, range, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because validator is null.");
        } catch (Exception expected) {
        }

        Range.computeRangeUByte(uBytes, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10d);
        assertEquals(253, range.getMax(), 1e-10d);

        Range.computeRangeUByte(uBytes, _validator, range, ProgressMonitor.NULL);

        assertEquals(4, range.getMin(), 1e-10d);
        assertEquals(250, range.getMax(), 1e-10d);
    }

    public void testComputeRangeShort() {
        short[] shorts = new short[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeShort(shorts, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeShort(shorts, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeUShort() {
        final short[] uShorts = TestHelper.createUShorts(new int[]{2, 65533, 4, 5, 65530, 7, 8, 9});
        Range range = new Range();

        Range.computeRangeUShort(uShorts, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10d);
        assertEquals(65533, range.getMax(), 1e-10d);

        Range.computeRangeUShort(uShorts, _validator, range, ProgressMonitor.NULL);

        assertEquals(4, range.getMin(), 1e-10d);
        assertEquals(65530, range.getMax(), 1e-10d);
    }

    public void testComputeRangeInt() {
        int[] ints = new int[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeInt(ints, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeInt(ints, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeUInt() {
        final int[] uInts = TestHelper.createUInts(new long[]{2, 4294967293L, 4, 5, 4294967290L, 7, 8, 9});
        Range range = new Range();

        Range.computeRangeUInt(uInts, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10d);
        assertEquals(4294967293L, range.getMax(), 1e-10d);

        Range.computeRangeUInt(uInts, _validator, range, ProgressMonitor.NULL);

        assertEquals(4, range.getMin(), 1e-10d);
        assertEquals(4294967290L, range.getMax(), 1e-10d);
    }

    public void testComputeRangeFloat() {
        float[] floats = new float[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeFloat(floats, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeFloat(floats, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeDouble() {
        double[] doubles = new double[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeDouble(doubles, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeDouble(doubles, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeDoubleArray() {
        final DoubleList array = TestHelper.createArray(new int[]{2, -3, 4, 5, -6, 7, 8, 9});
        Range range = new Range();

        Range.computeRangeDouble(array, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeDouble(array, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericByte() {
        final boolean unsigned = false;
        byte[] bytes = new byte[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeGeneric(bytes, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(bytes, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericUByte() {
        final boolean unsigned = true;
        final byte[] uBytes = TestHelper.createUBytes(new short[]{2, 253, 4, 5, 250, 7, 8, 9});
        Range range = new Range();

        Range.computeRangeGeneric(uBytes, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10d);
        assertEquals(253, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(uBytes, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(4, range.getMin(), 1e-10d);
        assertEquals(250, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericShort() {
        final boolean unsigned = false;
        short[] shorts = new short[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeGeneric(shorts, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(shorts, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericUShort() {
        final boolean unsigned = true;
        final short[] uShorts = TestHelper.createUShorts(new int[]{2, 65533, 4, 5, 65530, 7, 8, 9});
        Range range = new Range();

        Range.computeRangeGeneric(uShorts, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10d);
        assertEquals(65533, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(uShorts, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(4, range.getMin(), 1e-10d);
        assertEquals(65530, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericInt() {
        final boolean unsigned = false;
        int[] ints = new int[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeGeneric(ints, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(ints, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericUInt() {
        final boolean unsigned = true;
        final int[] uInts = TestHelper.createUInts(new long[]{2, 4294967293L, 4, 5, 4294967290L, 7, 8, 9});
        Range range = new Range();

        Range.computeRangeGeneric(uInts, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10d);
        assertEquals(4294967293L, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(uInts, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(4, range.getMin(), 1e-10d);
        assertEquals(4294967290L, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericFloat() {
        final boolean unsigned = false;
        float[] floats = new float[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeGeneric(floats, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(floats, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericDouble() {
        final boolean unsigned = false;
        double[] doubles = new double[]{2, -3, 4, 5, -6, 7, 8, 9};
        Range range = new Range();

        Range.computeRangeGeneric(doubles, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(doubles, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }

    public void testComputeRangeGenericDoubleArray() {
        final boolean unsigned = false;
        final DoubleList array = TestHelper.createArray(new int[]{2, -3, 4, 5, -6, 7, 8, 9});
        Range range = new Range();

        Range.computeRangeGeneric(array, unsigned, IndexValidator.TRUE, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(9, range.getMax(), 1e-10d);

        Range.computeRangeGeneric(array, unsigned, _validator, range, ProgressMonitor.NULL);

        assertEquals(-6, range.getMin(), 1e-10d);
        assertEquals(7, range.getMax(), 1e-10d);
    }
}
