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

public class RangeAggregatingTest extends TestCase {

    private final IndexValidator _validator = new IndexValidator() {
        public boolean validateIndex(int index) {
            return !(index == 1 || index == 5);
        }
    };

    public void testAggregatingRangeByte() {
        final boolean unsigned = false;
        final byte[] bytes = new byte[]{2, -5, 7, 20, 40, 100};

        final Range range = new Range(6, 15);
        range.aggregate(bytes, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(-5, range.getMin(), 1e-10);
        assertEquals(100, range.getMax(), 1e-10);

        range.setMinMax(6, 15);
        range.aggregate(bytes, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10);
        assertEquals(40, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeUByte() {
        final boolean unsigned = true;
        final byte[] uBytes = TestHelper.createUBytes(new short[]{40, 20, 50, 60, 200, 230});

        final Range range = new Range(87, 100);
        range.aggregate(uBytes, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(20, range.getMin(), 1e-10);
        assertEquals(230, range.getMax(), 1e-10);

        range.setMinMax(87, 100);
        range.aggregate(uBytes, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(40, range.getMin(), 1e-10);
        assertEquals(200, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeShort() {
        final boolean unsigned = false;
        final short[] shorts = new short[]{2, -5, 7, 20, 40, 100};

        final Range range = new Range(6, 15);
        range.aggregate(shorts, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(-5, range.getMin(), 1e-10);
        assertEquals(100, range.getMax(), 1e-10);

        range.setMinMax(6, 15);
        range.aggregate(shorts, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10);
        assertEquals(40, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeUShort() {
        final boolean unsigned = true;
        final short[] uShorts = TestHelper.createUShorts(new int[]{40, 20, 50, 30000, 40000, 50000});

        final Range range = new Range(87, 100);
        range.aggregate(uShorts, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(20, range.getMin(), 1e-10);
        assertEquals(50000, range.getMax(), 1e-10);

        range.setMinMax(87, 100);
        range.aggregate(uShorts, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(40, range.getMin(), 1e-10);
        assertEquals(40000, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeInt() {
        final boolean unsigned = false;
        final int[] ints = new int[]{2, -5, 7, 20, 40, 100};

        final Range range = new Range(6, 15);
        range.aggregate(ints, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(-5, range.getMin(), 1e-10);
        assertEquals(100, range.getMax(), 1e-10);

        range.setMinMax(6, 15);
        range.aggregate(ints, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10);
        assertEquals(40, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeUInt() {
        final boolean unsigned = true;
        final int[] uInts = TestHelper.createUInts(new long[]{40, 20, 50, 2000000000, 3000000000L, 3500000000L});

        final Range range = new Range(87, 100);
        range.aggregate(uInts, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(20, range.getMin(), 1e-10);
        assertEquals(3500000000L, range.getMax(), 1e-10);

        range.setMinMax(87, 100);
        range.aggregate(uInts, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(40, range.getMin(), 1e-10);
        assertEquals(3000000000L, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeFloat() {
        final boolean unsigned = false;
        final float[] floats = new float[]{2, -5, 7, 20, 40, 100};

        final Range range = new Range(6, 15);
        range.aggregate(floats, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(-5, range.getMin(), 1e-10);
        assertEquals(100, range.getMax(), 1e-10);

        range.setMinMax(6, 15);
        range.aggregate(floats, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10);
        assertEquals(40, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeDouble() {
        final boolean unsigned = false;
        final double[] doubles = new double[]{2, -5, 7, 20, 40, 100};

        final Range range = new Range(6, 15);
        range.aggregate(doubles, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(-5, range.getMin(), 1e-10);
        assertEquals(100, range.getMax(), 1e-10);

        range.setMinMax(6, 15);
        range.aggregate(doubles, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10);
        assertEquals(40, range.getMax(), 1e-10);
    }

    public void testAggregatingRangeDoubleArray() {
        final boolean unsigned = false;
        final DoubleList array = TestHelper.createArray(new int[]{2, -5, 7, 20, 40, 100});


        final Range range = new Range(6, 15);
        range.aggregate(array, unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);

        assertEquals(-5, range.getMin(), 1e-10);
        assertEquals(100, range.getMax(), 1e-10);

        range.setMinMax(6, 15);
        range.aggregate(array, unsigned, _validator, ProgressMonitor.NULL);

        assertEquals(2, range.getMin(), 1e-10);
        assertEquals(40, range.getMax(), 1e-10);
    }

    public void testIllegalArguments() {
        final Range range = new Range();
        try {
            range.aggregate(null, false, IndexValidator.TRUE, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because values are null");
        } catch (IllegalArgumentException expected) {
        }

        try {
            range.aggregate(new long[3], false, IndexValidator.TRUE, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because long[] is an illegal type for values");
        } catch (IllegalArgumentException expected) {
        }

        try {
            range.aggregate(new Object[3], false, IndexValidator.TRUE, ProgressMonitor.NULL);
            fail("IllegalArgumentException expected because Object[] is an illegal type for values");
        } catch (IllegalArgumentException expected) {
        }
    }
}
