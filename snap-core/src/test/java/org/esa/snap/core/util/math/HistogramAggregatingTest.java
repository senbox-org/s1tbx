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

import static org.esa.snap.GlobalTestTools.*;

public class HistogramAggregatingTest extends TestCase {

    public void testByte() {
        final byte[][] values = new byte[][]{
                new byte[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new byte[]{-2, 0, 2, 4},
                new byte[]{0, 1, 2}
        };
        final boolean unsigned = false;


        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testByte_withValidator() {
        final byte[][] values = new byte[][]{
                new byte[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new byte[]{-2, 0, 2, 4},
                new byte[]{0, 1, 2}
        };
        final boolean unsigned = false;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testUByte() {
        final byte[][] values = new byte[][]{
                TestHelper.createUBytes(new short[]{124, 125, 126, 127, 128, 129, 130, 131}),
                TestHelper.createUBytes(new short[]{125, 127, 129, 131}),
                TestHelper.createUBytes(new short[]{127, 128, 129})
        };
        final boolean unsigned = true;

        final Histogram histogram = new Histogram(new int[6], 125, 130);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testUByte_withValidator() {
        final byte[][] values = new byte[][]{
                TestHelper.createUBytes(new short[]{124, 125, 126, 127, 128, 129, 130, 131}),
                TestHelper.createUBytes(new short[]{125, 127, 129, 131}),
                TestHelper.createUBytes(new short[]{127, 128, 129})
        };
        final boolean unsigned = true;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], 125, 130);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testShort() {
        final short[][] values = new short[][]{
                new short[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new short[]{-2, 0, 2, 4},
                new short[]{0, 1, 2}
        };
        final boolean unsigned = false;

        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testShort_withValidator() {
        final short[][] values = new short[][]{
                new short[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new short[]{-2, 0, 2, 4},
                new short[]{0, 1, 2}
        };
        final boolean unsigned = false;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testUShort() {
        final short[][] values = new short[][]{
                TestHelper.createUShorts(new int[]{32764, 32765, 32766, 32767, 32768, 32769, 32770, 32771}),
                TestHelper.createUShorts(new int[]{32765, 32767, 32769, 32771}),
                TestHelper.createUShorts(new int[]{32767, 32768, 32769})
        };
        final boolean unsigned = true;

        final Histogram histogram = new Histogram(new int[6], 32765, 32770);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testUShort_withValidator() {
        final short[][] values = new short[][]{
                TestHelper.createUShorts(new int[]{32764, 32765, 32766, 32767, 32768, 32769, 32770, 32771}),
                TestHelper.createUShorts(new int[]{32765, 32767, 32769, 32771}),
                TestHelper.createUShorts(new int[]{32767, 32768, 32769})
        };
        final boolean unsigned = true;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], 32765, 32770);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testInt() {
        final int[][] values = new int[][]{
                new int[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new int[]{-2, 0, 2, 4},
                new int[]{0, 1, 2}
        };
        final boolean unsigned = false;

        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testInt_withValidator() {
        final int[][] values = new int[][]{
                new int[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new int[]{-2, 0, 2, 4},
                new int[]{0, 1, 2}
        };
        final boolean unsigned = false;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testUInt() {
        final int[][] values = new int[][]{
                TestHelper.createUInts(new long[]{
                        2147483644L,
                        2147483645L,
                        2147483646L,
                        2147483647L,
                        2147483648L,
                        2147483649L,
                        2147483650L,
                        2147483651L
                }),
                TestHelper.createUInts(new long[]{
                        2147483645L, 2147483647L, 2147483649L, 2147483651L
                }),
                TestHelper.createUInts(new long[]{
                        2147483647L, 2147483648L, 2147483649L
                })
        };
        final boolean unsigned = true;

        final Histogram histogram = new Histogram(new int[6], 2147483645, 2147483650L);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testUInt_withValidator() {
        final int[][] values = new int[][]{
                TestHelper.createUInts(new long[]{
                        2147483644L,
                        2147483645L,
                        2147483646L,
                        2147483647L,
                        2147483648L,
                        2147483649L,
                        2147483650L,
                        2147483651L
                }),
                TestHelper.createUInts(new long[]{
                        2147483645L, 2147483647L, 2147483649L, 2147483651L
                }),
                TestHelper.createUInts(new long[]{
                        2147483647L, 2147483648L, 2147483649L
                })
        };
        final boolean unsigned = true;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], 2147483645, 2147483650L);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testFloat() {
        final float[][] values = new float[][]{
                new float[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new float[]{-2, 0, 2, 4},
                new float[]{0, 1, 2}
        };
        final boolean unsigned = false;

        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testFloat_withValidator() {
        final float[][] values = new float[][]{
                new float[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new float[]{-2, 0, 2, 4},
                new float[]{0, 1, 2}
        };
        final boolean unsigned = false;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testDouble() {
        final double[][] values = new double[][]{
                new double[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new double[]{-2, 0, 2, 4},
                new double[]{0, 1, 2}
        };
        final boolean unsigned = false;

        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testDouble_withValidator() {
        final double[][] values = new double[][]{
                new double[]{-3, -2, -1, 0, 1, 2, 3, 4},
                new double[]{-2, 0, 2, 4},
                new double[]{0, 1, 2}
        };
        final boolean unsigned = false;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testDoubleArray() {
        final DoubleList[] values = new DoubleList[]{
                TestHelper.createArray(new int[]{-3, -2, -1, 0, 1, 2, 3, 4}),
                TestHelper.createArray(new int[]{-2, 0, 2, 4}),
                TestHelper.createArray(new int[]{0, 1, 2})
        };
        final boolean unsigned = false;

        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, IndexValidator.TRUE, ProgressMonitor.NULL);
        }

        final int[] exp = new int[]{2, 1, 3, 2, 3, 1};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

    public void testDoubleArray_withValidator() {
        final DoubleList[] values = new DoubleList[]{
                TestHelper.createArray(new int[]{-3, -2, -1, 0, 1, 2, 3, 4}),
                TestHelper.createArray(new int[]{-2, 0, 2, 4}),
                TestHelper.createArray(new int[]{0, 1, 2})
        };
        final boolean unsigned = false;
        final IndexValidator validator = new IndexValidator() {
            public boolean validateIndex(int index) {
                return index % 3 != 0;
            }
        };


        final Histogram histogram = new Histogram(new int[6], -2, 3);
        for (int i = 0; i < values.length; i++) {
            histogram.aggregate(values[i], unsigned, validator, ProgressMonitor.NULL);
        }


        final int[] exp = new int[]{1, 1, 1, 2, 3, 0};
        assertEquals("", equal(exp, histogram.getBinCounts()));
    }

}
