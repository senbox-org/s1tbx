/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.gpf;

import junit.framework.TestCase;
import org.esa.nest.util.TestUtils;


/**
 * Unit test for PolOpUtils.
 */
public class TestPolOpUtils extends TestCase {

    public TestPolOpUtils(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Compute coherency matrix T4 from a given covariance matrix C4
     * @throws Exception general exception
     */
    public void testC4ToT4() throws Exception {

		final double[][] c4Re = {
                {0.8147, 0.6324, 0.9575, 0.9572},
                {0.6324, 0.0975, 0.9649, 0.4854},
                {0.9575, 0.9649, 0.1576, 0.8003},
                {0.9572, 0.4854, 0.8003, 0.1419} };

        final double[][] c4Im = {
                {0.0, 0.6557, 0.6787, 0.6555},
                {-0.6557, 0.0, 0.7577, 0.1712},
                {-0.6787, -0.7577, 0.0, 0.7060},
                {-0.6555, -0.1712, -0.7060, 0.0} };

        final double[][] expRe = {
                {1.4355, 0.3364, 1.4378, 0.25589999999999996},
                {0.3364, -0.47890000000000005, 0.1521, -0.2789},
                {1.4378, 0.1521, 1.09245, -0.7577},
                {0.25589999999999996, -0.2789, -0.7577, -0.83735} };

        final double[][] expIm = {
                {0.0, -0.6555, 0.22859999999999991, 0.32000000000000006},
                {0.6555,0.0, 1.1058, 0.005099999999999993},
                {-0.22859999999999991, -1.1058, 0.0, 0.030049999999999993},
                {-0.32000000000000006, -0.005099999999999993, -0.030049999999999993, 0.0} };

		final double[][] t4Re = new double[4][4];
		final double[][] t4Im = new double[4][4];

        PolOpUtils.c4ToT4(c4Re, c4Im, t4Re, t4Im);

        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                assertTrue(Double.compare(t4Re[i][j], expRe[i][j]) == 0);
                assertTrue(Double.compare(t4Im[i][j], expIm[i][j]) == 0);
            }
        }
    }
}
