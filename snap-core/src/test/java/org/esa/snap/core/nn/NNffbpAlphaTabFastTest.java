package org.esa.snap.core.nn;

/*
 * $Id: NNffbpAlphaTabFastTest.java,v 1.2 2006/07/24 14:40:25 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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


import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

public class NNffbpAlphaTabFastTest {


    @Test
    public void testCalcJacobi() {
        final NNffbpAlphaTabFast tab = loadTestNet();

        final double[] nnInput = new double[]{1.0, 3.4, 6.988, 4.4, 7.0, 16.21};

        final NNCalc nnCalc = tab.calcJacobi(nnInput);

        final double[] expNnOutput = new double[]{0.9999546066706964};
        assertEquals(expNnOutput[0], nnCalc.getNnOutput()[0], 1.0e-6);


        final double[][] expJacobiMatrix = new double[][]{
                {
                        -7.3325278006568306E-6, 3.2639459486171825E-6, 7.527711538784537E-6,
                        -8.186466522910375E-6, 9.557482758745628E-6, 1.2507178214659703E-5
                }
        };
        final double[][] jacobiMatrix = nnCalc.getJacobiMatrix();
        for (int i = 0; i < jacobiMatrix.length; i++) {
            for (int j = 0; j < jacobiMatrix[i].length; j++) {
                assertEquals(expJacobiMatrix[i][j], jacobiMatrix[i][j], 1.0e-6);
            }
        }

    }

    @SuppressWarnings({"CallToSystemGC"})
    // deactivated this test; it fails sometimes
    public void testCalcJacobiPerformance() {
        final NNffbpAlphaTabFast tab = loadTestNet();

        final double[] nnInput = new double[]{1.0, 3.4, 6.988, 4.4, 7.0, 16.21};
        System.gc(); // call gc in order to prevent garbage collection during performance test
        final long t1 = System.nanoTime();
        final int N = 100000;
        for (int i = 0; i < N; i++) {
            final double[] doubles = nnInput.clone();
            for (int j = 0; j < doubles.length; j++) {
                doubles[j] += 1.0e-5 * Math.random();
            }
            final NNCalc nnCalc = tab.calcJacobi(doubles);
            assertTrue(nnCalc.getNnOutput()[0] != 0.0);
        }
        final long t2 = System.nanoTime();
        final double duration = (t2 - t1) / 1.0e9;
        // using reference time in order to get rid of a constant time the duration is compared to
        System.gc(); // call gc in order to prevent garbage collection during performance test
        final double refTime = getReferenceTime(N);
        assertTrue(duration < refTime);
    }

    private double getReferenceTime(int n) {
        final long t0 = System.nanoTime();
        for (int i = 0; i < n * 100; i++) { // Jacobi is less than 100 times slower
            Math.acos(1.0e-5 * Math.random());
        }
        final long t1 = System.nanoTime();
        return (t1 - t0) / 1.0e9;
    }

    @Test
    public void testCalcPerformance() {
        final NNffbpAlphaTabFast tab = loadTestNet();

        final double[] nnInput = new double[]{1.0, 3.4, 6.988, 4.4, 7.0, 16.21};

        final long t1 = System.nanoTime();
        final int N = 100000;
        for (int i = 0; i < N; i++) {
            final double[] doubles = nnInput.clone();
            for (int j = 0; j < doubles.length; j++) {
                doubles[j] += 1.0e-5 * Math.random();
            }
            final double[] nnCalc = tab.calc(doubles);
            assertTrue(nnCalc[0] != 0.0);
        }
        final long t2 = System.nanoTime();
        final double seconds = (t2 - t1) / 1.0e9;
        System.out.println("testCalcPerformance: " + seconds + " seconds");
        assertTrue(seconds < 1.0);
    }

    private static NNffbpAlphaTabFast loadTestNet() {
        NNffbpAlphaTabFast tabFast = null;
        final InputStream stream = NNffbpAlphaTabFastTest.class.getResourceAsStream("nn_test.net");
        try {
            tabFast = new NNffbpAlphaTabFast(stream);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to load test net.");
        }
        return tabFast;
    }

}
