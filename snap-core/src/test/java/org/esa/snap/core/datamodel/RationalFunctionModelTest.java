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

package org.esa.snap.core.datamodel;

import junit.framework.TestCase;

import java.util.Random;

/**
 * Tests for class {@link RationalFunctionModel}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class RationalFunctionModelTest extends TestCase {

    public void testConstuctor() {
        // todo - complete
    }

    public void testGetValue() {
        final F f1 = new F1();
        assertApproximation(f1, 1, 0, 0, 4);
        assertApproximation(f1, 2, 0, 0, 4);
        assertApproximation(f1, 3, 0, 0, 4);
        assertApproximation(f1, 4, 0, 0, 4);
        assertApproximation(f1, 4, 2, 2, 4);

        final F f2 = new F2();
        assertApproximation(f2, 4, 4, 0, 29);

        final F f3 = new Noisyfier(f2);
        assertApproximation(f3, 4, 4, 0, 29);

        final F f4 = new RandomLat();
        assertApproximation(f4, 3, 0, 0, 10);

        final F f5 = new RandomLon();
        assertApproximation(f5, 3, 0, 0, 10);
    }

    public void testLatLonApproximation() { // from MERIS scene of Italian lakes
        double[] x = new double[]{43.5,
                37.5,
                523.5,
                530.5,
                1075.5,
                1074.5,
                832.5,
                229.5,
                524.5};
        double[] y = new double[]{22.5,
                284.5,
                289.5,
                18.5,
                17.5,
                284.5,
                157.5,
                157.5,
                155.5};
        double[] lat = new double[]{49.27275,
                46.573524,
                45.553078,
                48.319298,
                46.770004,
                44.09146,
                46.09978,
                47.545685,
                46.92791};
        double[] lon = new double[]{6.051173,
                5.255776,
                11.624002,
                12.795015,
                19.971725,
                18.575577,
                16.156733,
                8.208557,
                12.158185};

        final RationalFunctionModel latModel = new RationalFunctionModel(3, 0, x, y, lat);
        final RationalFunctionModel lonModel = new RationalFunctionModel(3, 0, x, y, lon);

        for (int i = 0; i < lon.length; i++) {
            assertEquals(lat[i], latModel.getValue(x[i], y[i]), 1.0E-6);
            assertEquals(lon[i], lonModel.getValue(x[i], y[i]), 1.0E-6);
        }
    }

    private static void assertApproximation(F f, int degreeP, int degreeQ, int iterationCount, int sampleCount) {
        final double[] x = new double[sampleCount];
        final double[] y = new double[sampleCount];
        final double[] g = new double[sampleCount];

        final Random random = new Random(17);
        for (int i1 = 0; i1 < sampleCount; i1++) {
            x[i1] = random.nextDouble();
            y[i1] = random.nextDouble();
            g[i1] = f.getValue(x[i1], y[i1]);
        }

        final RationalFunctionModel rfm = new RationalFunctionModel(degreeP, degreeQ, x, y, g, iterationCount);

        for (int i = 0; i < sampleCount; i++) {
            assertEquals(g[i], rfm.getValue(x[i], y[i]), 1.0E-10);
        }
    }

    private static class F1 implements F {

        public double getValue(double x, double y) {
            return x + y;
        }
    }

    private static class F2 implements F {

        public double getValue(double x, double y) {
            final double xx = x * x;
            final double xy = x * y;
            final double yy = y * y;

            return (1.0 + x + y + xx + xy + xx * xy) / (1.0 + xx + yy * yy);
        }
    }

    private static class Noisyfier implements F {

        private final Random random;
        private final F f;

        public Noisyfier(F f) {
            this(f, new Random(27182));
        }

        public Noisyfier(F f, Random random) {
            this.f = f;
            this.random = random;
        }

        public double getValue(double x, double y) {
            return f.getValue(x, y) + random.nextDouble() - 0.5;
        }
    }

    private static class RandomLat implements F {

        private final Random random;

        public RandomLat() {
            this(new Random(27182));
        }

        public RandomLat(Random random) {
            this.random = random;
        }

        public double getValue(double x, double y) {
            return 180.0 * random.nextDouble() - 90.0;
        }
    }

    private static class RandomLon implements F {

        private final Random random;

        public RandomLon() {
            this(new Random(27182));
        }

        public RandomLon(Random random) {
            this.random = random;
        }

        public double getValue(double x, double y) {
            return 360.0 * random.nextDouble() - 180.0;
        }
    }

    private interface F {

        public double getValue(double x, double y);
    }
}
