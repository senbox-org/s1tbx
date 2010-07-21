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
package org.esa.beam.util.math;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;

public class StatisticsTest extends TestCase {

    final byte[] bvalues = new byte[]{90, 95, 100, 105, 110};
    final byte[] ubvalues = new byte[]{(byte) 190, (byte) 195, (byte) 200, (byte) 205, (byte) 210};
    final short[] svalues = new short[]{-2, 0, 3};
    final float[] fvalues = new float[]{1.9f, 1.94f, 1.95f, 2.0f, 2.05f, 2.06f, 2.1f};
    final double[] dvalues = new double[]{1.9, 1.94, 1.95, 2.0, 2.05, 2.06, 2.1};

    public void testAllTypes() {
        final Statistics bstat = Statistics.computeStatisticsGeneric(bvalues, false, IndexValidator.TRUE, null,
                                                                     ProgressMonitor.NULL);
        assertEquals(5, bstat.getNumTotal());
        assertEquals(5, bstat.getNum());
        assertEquals(90, bstat.getMin(), 1e-10);
        assertEquals(110, bstat.getMax(), 1e-10);
        assertEquals(100, bstat.getMean(), 1e-10);
        assertEquals(62.5, bstat.getVar(), 1e-10);
        assertEquals(7.90569415, bstat.getStdDev(), 1e-5);

        final Statistics ubstat = Statistics.computeStatisticsGeneric(ubvalues, true, IndexValidator.TRUE, null,
                                                                      ProgressMonitor.NULL);
        assertEquals(5, ubstat.getNumTotal());
        assertEquals(5, ubstat.getNum());
        assertEquals(190, ubstat.getMin(), 1e-10);
        assertEquals(210, ubstat.getMax(), 1e-10);
        assertEquals(200, ubstat.getMean(), 1e-10);
        assertEquals(62.5, ubstat.getVar(), 1e-10);
        assertEquals(7.90569415, bstat.getStdDev(), 1e-5);

        final Statistics sstat = Statistics.computeStatisticsGeneric(svalues, false, IndexValidator.TRUE, null,
                                                                     ProgressMonitor.NULL);
        assertEquals(3, sstat.getNumTotal());
        assertEquals(3, sstat.getNum());
        assertEquals(-2, sstat.getMin(), 1e-10);
        assertEquals(3, sstat.getMax(), 1e-10);
        assertEquals(0.333333333333333, sstat.getMean(), 1e-10);
        assertEquals(6.333333333333333, sstat.getVar(), 1e-10);
        assertEquals(2.516611478423583, sstat.getStdDev(), 1e-10);

        final Statistics fstat = Statistics.computeStatisticsGeneric(fvalues, false, IndexValidator.TRUE, null,
                                                                     ProgressMonitor.NULL);
        assertEquals(7, fstat.getNumTotal());
        assertEquals(7, fstat.getNum());
        assertEquals(1.9, fstat.getMin(), 1e-5);
        assertEquals(2.1, fstat.getMax(), 1e-5);
        assertEquals(2.0, fstat.getMean(), 1e-5);
        assertEquals(0.005366666666666, fstat.getVar(), 1e-5);
        assertEquals(0.073257536586119, fstat.getStdDev(), 1e-5);

        final Statistics dstat = Statistics.computeStatisticsGeneric(dvalues, false, IndexValidator.TRUE, null,
                                                                     ProgressMonitor.NULL);
        assertEquals(7, dstat.getNumTotal());
        assertEquals(7, dstat.getNum());
        assertEquals(1.9, dstat.getMin(), 1e-10);
        assertEquals(2.1, dstat.getMax(), 1e-10);
        assertEquals(2.0, dstat.getMean(), 1e-10);
        assertEquals(0.005366666666666, dstat.getVar(), 1e-10);
        assertEquals(0.073257536586119, dstat.getStdDev(), 1e-10);
    }

    public void testIndexValidatorIsUsed() {
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 2 == 0;
            }
        };
        final Statistics dstat = Statistics.computeStatisticsGeneric(dvalues, false, validator, null,
                                                                     ProgressMonitor.NULL);
        assertEquals(7, dstat.getNumTotal());
        assertEquals(4, dstat.getNum());
        assertEquals(1.9, dstat.getMin(), 1e-10);
        assertEquals(2.1, dstat.getMax(), 1e-10);
        assertEquals(2.0, dstat.getMean(), 1e-10);
        assertEquals(0.0083333333333329, dstat.getVar(), 1e-10);
        assertEquals(0.0912870929175251, dstat.getStdDev(), 1e-10);
    }

    public void testReuse() {
        Statistics statistics1 = new Statistics();
        Statistics statistics2;

        statistics2 = Statistics.computeStatisticsGeneric(dvalues, false, IndexValidator.TRUE, statistics1,
                                                          ProgressMonitor.NULL);
        assertSame(statistics2, statistics1);
        statistics2 = Statistics.computeStatisticsGeneric(dvalues, false, IndexValidator.TRUE, null,
                                                          ProgressMonitor.NULL);
        assertNotNull(statistics2);
        assertNotSame(statistics2, statistics1);
        assertEquals(statistics1.getNumTotal(), statistics2.getNumTotal());
        assertTrue(statistics1.getNum() == statistics2.getNum());
        assertTrue(statistics1.getMin() == statistics2.getMin());
        assertTrue(statistics1.getMax() == statistics2.getMax());
        assertTrue(statistics1.getMean() == statistics2.getMean());
        assertTrue(statistics1.getVar() == statistics2.getVar());
        assertTrue(statistics1.getStdDev() == statistics2.getStdDev());
    }
}
