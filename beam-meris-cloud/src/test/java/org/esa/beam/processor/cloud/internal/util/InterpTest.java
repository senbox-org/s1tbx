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
package org.esa.beam.processor.cloud.internal.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class InterpTest extends TestCase {

    public InterpTest(final String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(InterpTest.class);
    }

    public void testInterpCoordInAscendingMode() {

        final double[] xi = new double[]{10, 20, 30, 40};
        double x;
        int status;
        final FractIndex fractIndex = new FractIndex();

        x = 9; // below lower limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(2, status);
        assertEquals(0, fractIndex.index);
        assertEquals(0.0, fractIndex.fraction, 1e-10);

        x = 10.0; // = lower limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(0, fractIndex.index);
        assertEquals(0.0, fractIndex.fraction, 1e-10);

        x = 18.0; // between xi[0] and xi[1]
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(0, fractIndex.index);
        assertEquals(0.8, fractIndex.fraction, 1e-10);

        x = 33.4; // between xi[2] and xi[3]
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(2, fractIndex.index);
        assertEquals(0.34, fractIndex.fraction, 1e-10);

        x = 40.0; // = upper limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(3, fractIndex.index);
        assertEquals(0.0, fractIndex.fraction, 1e-10);

        x = 41.0; // above of upper limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(1, status);
        assertEquals(3, fractIndex.index);
        assertEquals(0.0, fractIndex.fraction, 1e-10);
    }

    public void testInterpCoordInDecendingMode() {

        final double[] xi = new double[]{40, 30, 20, 10};
        double x;
        int status;
        final FractIndex fractIndex = new FractIndex();

        x = 9; // below lower limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(4, status);
        assertEquals(3, fractIndex.index);
        assertEquals(0.0, fractIndex.fraction, 1e-10);

        x = 10.0; // = lower limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(3, fractIndex.index);
        assertEquals(0.0, fractIndex.fraction, 1e-10);

        x = 18.0; // between xi[0] and xi[1]
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(2, fractIndex.index);
        assertEquals(0.2, fractIndex.fraction, 1e-10);

        x = 33.4; // between xi[2] and xi[3]
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(0, fractIndex.index);
        assertEquals(0.66, fractIndex.fraction, 1e-10);

        x = 40.0; // = upper limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(0, status);
        assertEquals(0, fractIndex.index);
        assertEquals(0, fractIndex.fraction, 1e-10);

        x = 41.0; // above of upper limit
        status = Interp.interpCoord(x, xi, fractIndex);
        assertEquals(3, status);
        assertEquals(0, fractIndex.index);
        assertEquals(0, fractIndex.fraction, 1e-10);
    }

    public void testInterpolate1DFloat() {
        final int rank = 1;
        final int[] sizes = new int[rank];
        sizes[0] = 6;
        final float[] array = new float[sizes[0]];
        array[0] = 0.1f;
        array[1] = 0.2f;
        array[2] = 0.3f;
        array[3] = 0.4f;
        array[4] = 0.6f;
        array[5] = 0.8f;
        testInterpolate1D(array, sizes);
    }

    public void testInterpolate1DDouble() {
        final int rank = 1;
        final int[] sizes = new int[rank];
        sizes[0] = 6;
        final double[] array = new double[sizes[0]];
        array[0] = 0.1;
        array[1] = 0.2;
        array[2] = 0.3;
        array[3] = 0.4;
        array[4] = 0.6;
        array[5] = 0.8;
        testInterpolate1D(array, sizes);
    }

    public void testInterpolate2DFloat() {
        final int rank = 2;
        final int[] sizes = new int[rank];
        sizes[0] = 2;
        sizes[1] = 3;
        final float[][] array;
        array = new float[sizes[0]][sizes[1]];
        array[0][0] = 0.1f;
        array[0][1] = 0.2f;
        array[0][2] = 0.3f;
        array[1][0] = 0.4f;
        array[1][1] = 0.6f;
        array[1][2] = 0.8f;
        testInterpolate2D(array, sizes);
    }

    public void testInterpolate2DDouble() {
        final int rank = 2;
        final int[] sizes = new int[rank];
        sizes[0] = 2;
        sizes[1] = 3;
        final double[][] array;
        array = new double[sizes[0]][sizes[1]];
        array[0][0] = 0.1;
        array[0][1] = 0.2;
        array[0][2] = 0.3;
        array[1][0] = 0.4;
        array[1][1] = 0.6;
        array[1][2] = 0.8;
        testInterpolate2D(array, sizes);
    }

    private void testInterpolate1D(final Object a, final int[] ni) {
        final FractIndex[] fi = FractIndex.createArray(1);
        double y;

        /////////////////////////////////////
        // fraction = 0

        fi[0].index = 0;
        fi[0].fraction = 0;
        y = Interp.interpolate(a, fi);
        assertEquals(0.1, y, 1e-6);

        fi[0].index = 3;
        fi[0].fraction = 0;
        y = Interp.interpolate(a, fi);
        assertEquals(0.4, y, 1e-6);

        fi[0].index = 5;
        fi[0].fraction = 0;
        y = Interp.interpolate(a, fi);
        assertEquals(0.8, y, 1e-6);


        ///////////////////////////////////////////
        // test that no extrapolation is performed

        fi[0].index = 5;
        fi[0].fraction = 0.5;
        y = Interp.interpolate(a, fi);
        assertEquals(0.8, y, 1e-6);  // I (nf) would have expected 0.9 = (0.8 + 0.5 * (0.8 - 0.6))

        ///////////////////////////////////////////
        // test illegal fractions

        fi[0].index = 0;
        fi[0].fraction = -0.5;
        try {
            y = Interp.interpolate(a, fi);
            fail("IllegalArgumentException expected since fraction < 0");
        } catch (IllegalArgumentException e) {
        }

        fi[0].index = 0;
        fi[0].fraction = 1;
        try {
            y = Interp.interpolate(a, fi);
            fail("IllegalArgumentException expected since fraction >= 1");
        } catch (IllegalArgumentException e) {
        }
    }

    private void testInterpolate2D(final Object a, final int[] ni) {
        final FractIndex[] fi = FractIndex.createArray(2);
        double y;

        fi[0].index = 0;
        fi[0].fraction = 0.5;
        fi[1].index = 0;
        fi[1].fraction = 0.5;
        y = Interp.interpolate(a, fi);
        assertEquals(0.325, y, 1e-6);

        fi[0].index = 0;
        fi[0].fraction = 0.5;
        fi[1].index = 1;
        fi[1].fraction = 0.5;
        y = Interp.interpolate(a, fi);
        assertEquals(0.475, y, 1e-6);

        fi[0].index = 1;
        fi[0].fraction = 0.0;
        fi[1].index = 0;
        fi[1].fraction = 0.5;
        y = Interp.interpolate(a, fi);
        assertEquals(0.5, y, 1e-6);

        fi[0].index = 1;
        fi[0].fraction = 0.0;
        fi[1].index = 0;
        fi[1].fraction = 0.6;
        y = Interp.interpolate(a, fi);
        assertEquals(0.52, y, 1e-6);

        fi[0].index = 1;
        fi[0].fraction = 0.0;
        fi[1].index = 1;
        fi[1].fraction = 0.5;
        y = Interp.interpolate(a, fi);
        assertEquals(0.7, y, 1e-6);

        fi[0].index = 1;
        fi[0].fraction = 0.0;
        fi[1].index = 1;
        fi[1].fraction = 0.4;
        y = Interp.interpolate(a, fi);
        assertEquals(0.68, y, 1e-6);
    }
}

