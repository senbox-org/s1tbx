/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.util;

import junit.framework.TestCase;
import org.apache.commons.math.util.FastMath;


/**
 * MathUtils Tester.
 *
 * @author lveci
 */
public class TestMathUtils extends TestCase {

    final int numItr = 80000000;

    public TestMathUtils(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHanning() {
        int windowLength = 5;
        double w0 = MathUtils.hanning(-2.0, windowLength);
        double w1 = MathUtils.hanning(-1.0, windowLength);
        double w2 = MathUtils.hanning( 0.0, windowLength);
        double w3 = MathUtils.hanning( 1.0, windowLength);
        double w4 = MathUtils.hanning( 2.0, windowLength);
        assertTrue(Double.compare(w0, 0.2500000000000001) == 0);
        assertTrue(Double.compare(w1, 0.75) == 0);
        assertTrue(Double.compare(w2, 1.0) == 0);
        assertTrue(Double.compare(w3, 0.75) == 0);
        assertTrue(Double.compare(w4, 0.2500000000000001) == 0);
    }

    public void testInterpolationSinc() {

        double y0 = (-2.0 - 0.3)*(-2.0 - 0.3);
        double y1 = (-1.0 - 0.3)*(-1.0 - 0.3);
        double y2 = (0.0 - 0.3)*(0.0 - 0.3);
        double y3 = (1.0 - 0.3)*(1.0 - 0.3);
        double y4 = (2.0 - 0.3)*(2.0 - 0.3);
        double mu = 0.3;
        double y = MathUtils.interpolationSinc(y0, y1, y2, y3, y4, mu);

        double yExpected = -0.06751353045007912;
        assertTrue(Double.compare(y, yExpected) == 0);
    }

     public void testMathCos() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.cos(i);
        }
    }

    public void testFastMathCos() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.cos(i);
        }
    }

    public void testStrictMathCos() {
        for(double i=0; i < numItr; ++i) {
            double val = StrictMath.cos(i);
        }
    }

    public void testMathSin() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.sin(i);
        }
    }

    public void testFastMathSin() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.sin(i);
        }
    }

    public void testMathTan() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.tan(i);
        }
    }

    public void testFastMathTan() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.tan(i);
        }
    }

    public void testMathATan2() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.atan2(i, i);
        }
    }

    public void testFastMathATan2() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.atan2(i, i);
        }
    }

    public void testMathMin() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.min(i, 500);
        }
    }

    public void testFastMathMin() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.min(i, 500);
        }
    }

    public void testMathCeil() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.ceil(i);
        }
    }

    public void testFastMathCeil() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.ceil(i);
        }
    }

    public void testMathFloor() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.floor(i);
        }
    }

    public void testFastMathFloor() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.floor(i);
        }
    }

    public void testMathAbs() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.abs(i);
        }
    }

    public void testFastMathAbs() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.abs(i);
        }
    }

    public void testStrictMathAbs() {
        for(double i=0; i < numItr; ++i) {
            double val = StrictMath.abs(i);
        }
    }

    public void testMathRound() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.round(i);
        }
    }

    public void testFastMathRound() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.round(i);
        }
    }

    public void testMathPow() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.pow(i, i);
        }
    }

    public void testFastMathPow() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.pow(i, i);
        }
    }

    public void testMathLog() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.log(i);
        }
    }

    public void testFastMathLog() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.log(i);
        }
    }

    public void testMathLog10() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.log10(i);
        }
    }

    public void testFastMathLog10() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.log10(i);
        }
    }

    public void testMathExp() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.exp(i);
        }
    }

    public void testFastMathExp() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.exp(i);
        }
    }

    public void testMathSqrt() {
        for(double i=0; i < numItr; ++i) {
            double val = Math.sqrt(i);
        }
    }

    public void testFastMathSqrt() {
        for(double i=0; i < numItr; ++i) {
            double val = FastMath.sqrt(i);
        }
    }
}