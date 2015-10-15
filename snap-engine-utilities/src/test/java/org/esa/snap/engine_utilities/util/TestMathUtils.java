/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.util;

import org.esa.snap.engine_utilities.util.Maths;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * MathUtils Tester.
 *
 * @author lveci
 */
public class TestMathUtils {

    @Test
    public void testHanning() {
        int windowLength = 5;
        double w0 = Maths.hanning(-2.0, windowLength);
        double w1 = Maths.hanning(-1.0, windowLength);
        double w2 = Maths.hanning(0.0, windowLength);
        double w3 = Maths.hanning(1.0, windowLength);
        double w4 = Maths.hanning(2.0, windowLength);
        assertTrue(Double.compare(w0, 0.2500000000000001) == 0);
        assertTrue(Double.compare(w1, 0.75) == 0);
        assertTrue(Double.compare(w2, 1.0) == 0);
        assertTrue(Double.compare(w3, 0.75) == 0);
        assertTrue(Double.compare(w4, 0.2500000000000001) == 0);
    }

    @Test
    public void testInterpolationSinc() {

        double y0 = (-2.0 - 0.3) * (-2.0 - 0.3);
        double y1 = (-1.0 - 0.3) * (-1.0 - 0.3);
        double y2 = (0.0 - 0.3) * (0.0 - 0.3);
        double y3 = (1.0 - 0.3) * (1.0 - 0.3);
        double y4 = (2.0 - 0.3) * (2.0 - 0.3);
        double mu = 0.3;
        double y = Maths.interpolationSinc(y0, y1, y2, y3, y4, mu);

        double yExpected = -0.06751353045007909;
        assertTrue(Double.compare(y, yExpected) == 0);
    }
}