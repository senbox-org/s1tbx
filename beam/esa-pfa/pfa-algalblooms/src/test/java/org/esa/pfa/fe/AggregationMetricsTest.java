package org.esa.pfa.fe;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AggregationMetricsTest {

    @Test
    public void testCompute3x3_1() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(3, 3, new byte[]{
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
        });

        assertEquals(0, am.n00);

        assertEquals(0, am.n01);

        assertEquals(0, am.n10);

        assertEquals(40, am.n11);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertEquals(1.0, am.clumpiness, 0.0);
    }

    @Test
    public void testCompute3x3_2() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(3, 3, new byte[]{
                0, 0, 0,
                0, 0, 0,
                0, 0, 0,
        });

        assertEquals(40, am.n00);

        assertEquals(0, am.n01);

        assertEquals(0, am.n10);

        assertEquals(0, am.n11);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertEquals(-1.0, am.clumpiness, 0.0);
    }

    @Test
    public void testCompute4x4_1() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(4, 4, new byte[]{
                1, 1, 1, 1,
                1, 1, 1, 1,
                1, 1, 0, 0,
                1, 1, 0, 0,
        });

        assertEquals(3 + 3 + 3 + 3, am.n00);

        assertEquals(5 + 2 + 2 + 0, am.n01);

        assertEquals(1 + 2 + 2 + 2 + 2, am.n10);

        assertEquals(3 + 5 + 5 + 3 + 5 + 7 + 6 + 3 + 5 + 6 + 3 + 3, am.n11);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertTrue(am.clumpiness >= -1.0);

        assertTrue(am.clumpiness <= 1.0);

        System.out.println("am.clumpiness = " + am.clumpiness);
    }

    @Test
    public void testCompute4x4_2() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(4, 4, new byte[]{
                1, 1, 1, 1,
                1, 1, 1, 1,
                0, 0, 0, 0,
                0, 0, 0, 0,
        });

        assertEquals(3 + 5 + 5 + 3 + 3 + 5 + 5 + 3, am.n00);

        assertEquals(2 + 3 + 3 + 2, am.n01);

        assertEquals(2 + 3 + 3 + 2, am.n10);

        assertTrue(am.p01 == am.p10);

        assertEquals(3 + 5 + 5 + 3 + 3 + 5 + 5 + 3, am.n11);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertTrue(am.clumpiness >= -1.0);

        assertTrue(am.clumpiness <= 1.0);

        System.out.println("am.clumpiness = " + am.clumpiness);
    }

    @Test
    public void testCompute4x4_3() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(4, 4, new byte[]{
                1, 0, 1, 0,
                0, 1, 0, 1,
                1, 0, 1, 0,
                0, 1, 0, 1,
        });

        assertEquals(2 + 1 + 2 + 4 + 4 + 2 + 1 + 2, am.n00);

        assertEquals(3 + 2 + 3 + 4 + 4 + 3 + 2 + 3, am.n01);

        assertEquals(2 + 3 + 4 + 3 + 3 + 4 + 3 + 2, am.n10);

        assertEquals(1 + 2 + 4 + 2 + 2 + 4 + 2 + 1, am.n11);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertTrue(am.clumpiness >= -1.0);

        assertTrue(am.clumpiness <= 1.0);

        System.out.println("am.clumpiness = " + am.clumpiness);
    }

    @Test
    public void testCompute5x5() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(5, 5, new byte[]{
                1, 0, 1, 1, 1,
                1, 1, 0, 1, 1,
                1, 0, 1, 0, 1,
                0, 1, 0, 1, 0,
                0, 1, 0, 1, 0,
        });

        assertEquals(1 + 3 + 3 + 3 + 2 + 3 + 2 + 1 + 1 + 1, am.n00);

        assertEquals(4 + 5 + 5 + 5 + 3 + 5 + 3 + 2 + 4 + 2, am.n01);

        assertEquals(1 + 2 + 1 + 0 + 2 + 3 + 2 + 1 + 2 + 4 + 2 + 5 + 5 + 4 + 4, am.n10);

        assertEquals(2 + 3 + 4 + 3 + 3 + 5 + 6 + 4 + 3 + 4 + 3 + 3 + 3 + 1 + 1, am.n11);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertTrue(am.clumpiness >= -1.0);

        assertTrue(am.clumpiness <= 1.0);

        System.out.println("am.clumpiness = " + am.clumpiness);
    }

    @Test
    public void testCompute5x5_2() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(5, 5, new byte[]{
                0, 1, 1, 1, 0,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                0, 1, 1, 1, 0,
        });

        assertEquals(0, am.n00);

        assertEquals(12, am.n01);

        assertEquals(12, am.n10);

        assertEquals(120, am.n11);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertTrue(am.clumpiness >= -1.0);

        assertTrue(am.clumpiness <= 1.0);

        System.out.println("am.clumpiness = " + am.clumpiness);
    }

    @Test
    public void testCompute5x5_3() throws Exception {
        AggregationMetrics am = AggregationMetrics.compute(5, 5, new byte[]{
                1, 1, 0, 0, 0,
                1, 1, 1, 1, 0,
                0, 1, 1, 1, 0,
                0, 1, 1, 1, 1,
                0, 0, 0, 1, 1,
        });

        assertEquals(1 + 3 + 2 + 3 + 1 + 1 + 3 + 2 + 3 + 1, am.n00);

        assertEquals(4 + 2 + 1 + 2 + 4 + 4 + 2 + 1 + 2 + 4, am.n01);

        assertEquals(26, am.n10, 1e-5);

        assertEquals(72, am.n11, 1e-5);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertTrue(am.clumpiness >= -1.0);

        assertTrue(am.clumpiness <= 1.0);

        System.out.println("am.clumpiness = " + am.clumpiness);
    }

    @Test
    public void testCompute200x200_1() throws Exception {
        final byte[] data = new byte[200 * 200];
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 200; x++) {
                if (y < 100) {
                    data[x + y * 200] = 1;
                }
            }
        }
        AggregationMetrics am = AggregationMetrics.compute(200, 200, data);

        assertEquals(158204, am.n00);

        assertEquals(198 * 3 + 2 + 2, am.n01);

        assertEquals(198 * 3 + 2 + 2, am.n10, 1e-5);

        assertEquals(158204, am.n11, 1e-5);

        assertTrue(am.p01 == am.p10);

        assertEquals(1.0, am.p00 + am.p01 + am.p10 + am.p11, 0.0);

        assertTrue(am.contagion >= 0.0);

        assertTrue(am.contagion <= 1.0);

        assertTrue(am.clumpiness >= -1.0);

        assertTrue(am.clumpiness <= 1.0);

        System.out.println("am.clumpiness = " + am.clumpiness);
    }

    @Test
    public void testCompute200x200_2() throws Exception {
        final byte[] data = new byte[200 * 200];
        final Random random = new Random(3287);

        for (int i = 1; i < 1000; i++) {
            Arrays.fill(data, (byte) 1);

            for (int j = 0; j < i; j++) {
                final int k = random.nextInt(40000);
                data[k] = 0;
            }

            final AggregationMetrics am = AggregationMetrics.compute(200, 200, data);

            assertTrue(am.clumpiness >= -0.1);

            assertTrue(am.clumpiness <= 0.1);
        }
    }

    @Test
    public void testCompute200x200_3() throws Exception {
        final byte[] data = new byte[200 * 200];

        for (int i = 1; i < 1000; i++) {
            Arrays.fill(data, (byte) 1);

            for (int j = 0; j < i; j++) {
                data[j] = 0;
            }

            final AggregationMetrics am = AggregationMetrics.compute(200, 200, data);

            assertTrue(am.clumpiness >= -1.0);

            assertTrue(am.clumpiness <= 1.0);

            System.out.println("am.clumpiness = " + am.clumpiness);
        }
    }

}
