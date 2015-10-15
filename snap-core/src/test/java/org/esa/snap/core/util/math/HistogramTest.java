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

public class HistogramTest extends TestCase {

    public void testComputeHistogramByte() {
        byte[] data = new byte[]{1, 2, -3, -3, 40, 5, -6, 7, 8, 9, -10, 11, -120, 120, 121, 122};

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     IndexValidator.TRUE
        //        Range:              null

        Histogram histo = Histogram.computeHistogramByte(data, IndexValidator.TRUE, 5, null, null,
                                                         ProgressMonitor.NULL);

        assertEquals(-120, histo.getMin(), 1e-10);
        assertEquals(122, histo.getMax(), 1e-10);
        assertEquals(3, histo.getBinIndex(40.0));
        assertEquals(11, histo.getMaxBinCount());
        assertEquals(5, histo.getNumBins());
        int[] binValues = histo.getBinCounts();
        assertEquals(1, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(11, binValues[2]);
        assertEquals(1, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramByte(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(4, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(4, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               -10 ... 40

        histo = Histogram.computeHistogramByte(data, IndexValidator.TRUE, 5, histo, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(4, binValues[0]);
        assertEquals(6, binValues[1]);
        assertEquals(1, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(12, histo.getBinCountsSum());
    }

    public void testComputeHistogramUByte() {
        byte[] data = new byte[]{
                1, 2, -3 /*=253*/, -3/*=253*/, 40, 5, -6/*=250*/, 7, 8, 9, -10/*=246*/, 11, -120/*136*/, 120, 121, 122
        };

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     null
        //        Range:              null

        Histogram histo = Histogram.computeHistogramUByte(data, IndexValidator.TRUE, 5, null, null,
                                                          ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(8, histo.getMaxBinCount());
        assertEquals(1, histo.getMin(), 1e-10);
        int[] binValues = histo.getBinCounts();
        assertEquals(8, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(4, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(4, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramUByte(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(5, histo.getMaxBinCount());
        assertEquals(5, histo.getMin(), 1e-10);
        assertEquals(253, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(5, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               5 ... 250

        histo = Histogram.computeHistogramUByte(data, IndexValidator.TRUE, 5, new Range(5, 250), null,
                                                ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(5, histo.getMin(), 1e-10);
        assertEquals(250, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(6, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(4, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(2, binValues[4]);
        //=====================================
        assertEquals(12, histo.getBinCountsSum());
    }

    public void testComputeHistogramShort() {
        short[] data = new short[]{1, 2, -3, -3, 40, 5, -6, 7, 8, 9, -10, 11, -120, 120, 121, 122};

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     null
        //        Range:              null

        Histogram histo = Histogram.computeHistogramShort(data, IndexValidator.TRUE, 5, null, null,
                                                          ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(11, histo.getMaxBinCount());
        assertEquals(-120, histo.getMin(), 1e-10);
        assertEquals(122, histo.getMax(), 1e-10);
        int[] binValues = histo.getBinCounts();
        assertEquals(1, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(11, binValues[2]);
        assertEquals(1, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramShort(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(4, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(4, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               -10 ... 40

        histo = Histogram.computeHistogramShort(data, IndexValidator.TRUE, 5, histo, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(4, binValues[0]);
        assertEquals(6, binValues[1]);
        assertEquals(1, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(12, histo.getBinCountsSum());
    }

    public void testComputeHistogramUShort() {
        short[] data = new short[]{
                1,
                2,
                -3 /*=65533*/,
                -3/*=65533*/,
                40,
                5,
                -6/*=65530*/,
                7,
                8,
                9,
                -10/*=65526*/,
                11,
                -120/*65416*/,
                120,
                121,
                122
        };

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     null
        //        Range:              null

        Histogram histo = Histogram.computeHistogramUShort(data, IndexValidator.TRUE, 5, null, null,
                                                           ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(11, histo.getMaxBinCount());
        assertEquals(1, histo.getMin(), 1e-10);
        assertEquals(65533, histo.getMax(), 1e-10);
        int[] binValues = histo.getBinCounts();
        assertEquals(11, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(5, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramUShort(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(5, histo.getMaxBinCount());
        assertEquals(5, histo.getMin(), 1e-10);
        assertEquals(65533, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(5, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               5 ... 250

        histo = Histogram.computeHistogramUShort(data, IndexValidator.TRUE, 5, new Range(5, 250), null,
                                                 ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(5, histo.getMin(), 1e-10);
        assertEquals(250, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(6, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(0, binValues[4]);
        //=====================================
        assertEquals(9, histo.getBinCountsSum());
    }

    public void testComputeHistogramInt() {
        int[] data = new int[]{1, 2, -3, -3, 40, 5, -6, 7, 8, 9, -10, 11, -120, 120, 121, 122};

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     null
        //        Range:              null

        Histogram histo = Histogram.computeHistogramInt(data, IndexValidator.TRUE, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(11, histo.getMaxBinCount());
        assertEquals(-120, histo.getMin(), 1e-10);
        assertEquals(122, histo.getMax(), 1e-10);
        int[] binValues = histo.getBinCounts();
        assertEquals(1, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(11, binValues[2]);
        assertEquals(1, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramInt(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(4, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(4, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               -10 ... 40

        histo = Histogram.computeHistogramInt(data, IndexValidator.TRUE, 5, histo, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(4, binValues[0]);
        assertEquals(6, binValues[1]);
        assertEquals(1, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(12, histo.getBinCountsSum());
    }

    public void testComputeHistogramUInt() {
        int[] data = new int[]{
                1,
                2,
                -3 /*=4294967293*/,
                -3/*=4294967293*/,
                40,
                5,
                -6/*=4294967290*/,
                7,
                8,
                9,
                -10/*=4294967286*/,
                11,
                -120/*4294967176*/,
                120,
                121,
                122
        };

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     null
        //        Range:              null

        Histogram histo = Histogram.computeHistogramUInt(data, IndexValidator.TRUE, 5, null, null,
                                                         ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(11, histo.getMaxBinCount());
        assertEquals(1, histo.getMin(), 1e-10);
        assertEquals(4294967293L, histo.getMax(), 1e-10);
        int[] binValues = histo.getBinCounts();
        assertEquals(11, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(5, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramUInt(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(5, histo.getMaxBinCount());
        assertEquals(5, histo.getMin(), 1e-10);
        assertEquals(4294967293L, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(5, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               5 ... 250

        histo = Histogram.computeHistogramUInt(data, IndexValidator.TRUE, 5, new Range(5, 250), null,
                                               ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(5, histo.getMin(), 1e-10);
        assertEquals(250, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(6, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(0, binValues[4]);
        //=====================================
        assertEquals(9, histo.getBinCountsSum());
    }

    public void testComputeHistogramFloat() {
        float[] data = new float[]{1, 2, -3, -3, 40, 5, -6, 7, 8, 9, -10, 11, -120, 120, 121, 122};

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     null
        //        Range:              null

        Histogram histo = Histogram.computeHistogramFloat(data, IndexValidator.TRUE, 5, null, null,
                                                          ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(11, histo.getMaxBinCount());
        assertEquals(-120, histo.getMin(), 1e-10);
        assertEquals(122, histo.getMax(), 1e-10);
        int[] binValues = histo.getBinCounts();
        assertEquals(1, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(11, binValues[2]);
        assertEquals(1, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramFloat(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(4, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(4, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               -10 ... 40

        histo = Histogram.computeHistogramFloat(data, IndexValidator.TRUE, 5, histo, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(4, binValues[0]);
        assertEquals(6, binValues[1]);
        assertEquals(1, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(12, histo.getBinCountsSum());
    }

    public void testComputeHistogramDouble() {
        double[] data = new double[]{1, 2, -3, -3, 40, 5, -6, 7, 8, 9, -10, 11, -120, 120, 121, 122};

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     null
        //        Range:              null

        Histogram histo = Histogram.computeHistogramDouble(data, IndexValidator.TRUE, 5, null, null,
                                                           ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(11, histo.getMaxBinCount());
        assertEquals(-120, histo.getMin(), 1e-10);
        assertEquals(122, histo.getMax(), 1e-10);
        int[] binValues = histo.getBinCounts();
        assertEquals(1, binValues[0]);
        assertEquals(0, binValues[1]);
        assertEquals(11, binValues[2]);
        assertEquals(1, binValues[3]);
        assertEquals(3, binValues[4]);
        //=====================================
        assertEquals(16, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:     index > 2 && < 11
        //        Range:                 null

        IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index > 2 && index < 11;
            }
        };

        histo = Histogram.computeHistogramDouble(data, validator, 5, null, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(4, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(4, binValues[1]);
        assertEquals(0, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(8, histo.getBinCountsSum());

        ///////////////////////////////////////////////////////////////////////////////////////////
        //        IndexValidator:         null
        //        Range:               -10 ... 40

        histo = Histogram.computeHistogramDouble(data, IndexValidator.TRUE, 5, histo, null, ProgressMonitor.NULL);

        assertEquals(5, histo.getNumBins());
        assertEquals(6, histo.getMaxBinCount());
        assertEquals(-10, histo.getMin(), 1e-10);
        assertEquals(40, histo.getMax(), 1e-10);
        binValues = histo.getBinCounts();
        assertEquals(4, binValues[0]);
        assertEquals(6, binValues[1]);
        assertEquals(1, binValues[2]);
        assertEquals(0, binValues[3]);
        assertEquals(1, binValues[4]);
        //=====================================
        assertEquals(12, histo.getBinCountsSum());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    //   Compute Histogram Distribution Tests

    public void testComputeHistogramDistribution_Byte() {
        byte[] values = new byte[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramByte(values, IndexValidator.TRUE, 5, null, null,
                                                         ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    public void testComputeHistogramDistribution_UByte() {
        byte[] values = new byte[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramUByte(values, IndexValidator.TRUE, 5, null, null,
                                                          ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    public void testComputeHistogramDistribution_Short() {
        short[] values = new short[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramShort(values, IndexValidator.TRUE, 5, null, null,
                                                          ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    public void testComputeHistogramDistribution_UShort() {
        short[] values = new short[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramUShort(values, IndexValidator.TRUE, 5, null, null,
                                                           ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    public void testComputeHistogramDistribution_Int() {
        int[] values = new int[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramInt(values, IndexValidator.TRUE, 5, null, null,
                                                        ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    public void testComputeHistogramDistribution_UInt() {
        int[] values = new int[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramUInt(values, IndexValidator.TRUE, 5, null, null,
                                                         ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    public void testComputeHistogramDistribution_Float() {
        float[] values = new float[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramFloat(values, IndexValidator.TRUE, 5, null, null,
                                                          ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    public void testComputeHistogramDistribution_Double() {
        double[] values = new double[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramDouble(values, IndexValidator.TRUE, 5, null, null,
                                                           ProgressMonitor.NULL);
        int[] binValues = histo.getBinCounts();
        assertEquals(3, binValues[0]);
        assertEquals(3, binValues[1]);
        assertEquals(3, binValues[2]);
        assertEquals(3, binValues[3]);
        assertEquals(3, binValues[4]);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    //   Compute Histogram Distribution Tests

    public void testGetBinIndex() {
        double[] values = new double[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5};

        Histogram histo = Histogram.computeHistogramDouble(values, IndexValidator.TRUE, 5, null, null,
                                                           ProgressMonitor.NULL);
        assertEquals(-1, histo.getBinIndex(-1.0));
        assertEquals(-1, histo.getBinIndex(-100.0));
        assertEquals(1, histo.getBinIndex(+2.0));
        assertEquals(4, histo.getBinIndex(+5.0));
        assertEquals(-1, histo.getBinIndex(+6.0));
        assertEquals(-1, histo.getBinIndex(+100.0));
    }

}
