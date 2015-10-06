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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FXYTest extends TestCase {

    public final static double EPS = 1.0e-10;

    public FXYTest(String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(FXYTest.class);
    }

    public void testF() {
        assertEquals(2039.28109056, FXY.X4Y4.f(2.1, 3.2), EPS);
        assertEquals(637.2753408, FXY.X4Y3.f(2.1, 3.2), EPS);
        assertEquals(971.0862336, FXY.X3Y4.f(2.1, 3.2), EPS);
        assertEquals(199.148544, FXY.X4Y2.f(2.1, 3.2), EPS);
        assertEquals(462.422016, FXY.X2Y4.f(2.1, 3.2), EPS);
        assertEquals(62.23392, FXY.X4Y.f(2.1, 3.2), EPS);
        assertEquals(220.20096, FXY.XY4.f(2.1, 3.2), EPS);
        assertEquals(19.4481, FXY.X4.f(2.1, 3.2), EPS);
        assertEquals(104.8576, FXY.Y4.f(2.1, 3.2), EPS);
        assertEquals(303.464448, FXY.X3Y3.f(2.1, 3.2), EPS);
        assertEquals(144.50688, FXY.X2Y3.f(2.1, 3.2), EPS);
        assertEquals(94.83264, FXY.X3Y2.f(2.1, 3.2), EPS);
        assertEquals(29.6352, FXY.X3Y.f(2.1, 3.2), EPS);
        assertEquals(45.1584, FXY.X2Y2.f(2.1, 3.2), EPS);
        assertEquals(68.8128, FXY.XY3.f(2.1, 3.2), EPS);
        assertEquals(9.261, FXY.X3.f(2.1, 3.2), EPS);
        assertEquals(14.112, FXY.X2Y.f(2.1, 3.2), EPS);
        assertEquals(21.504, FXY.XY2.f(2.1, 3.2), EPS);
        assertEquals(32.768, FXY.Y3.f(2.1, 3.2), EPS);
        assertEquals(4.41, FXY.X2.f(2.1, 3.2), EPS);
        assertEquals(6.72, FXY.XY.f(2.1, 3.2), EPS);
        assertEquals(10.24, FXY.Y2.f(2.1, 3.2), EPS);
        assertEquals(2.1, FXY.X.f(2.1, 3.2), EPS);
        assertEquals(3.2, FXY.Y.f(2.1, 3.2), EPS);
        assertEquals(1.0, FXY.ONE.f(2.1, 3.2), EPS);
    }

}
