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
package org.esa.snap.core.util.math;

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

/**
 * Math benchmark.
 *
 * FastMath faster for: sin, cos, tan, tanh, asin, acos, exp, pow, hypot  only
 * FastMath very slow for log, log10
 *
 * @author lveci
 */
public class FastMathTest {

    final int numItr = 1000; //80000000;

    @Test
    public void testMathCos() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.cos(i);
        }
    }

    @Test
    public void testMathCosFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.cos(i);
        }
    }

    @Test
    public void testMathCosStrict() {
        for (double i = 0; i < numItr; ++i) {
            double val = StrictMath.cos(i);
        }
    }

    @Test
    public void testMathACos() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.acos(i);
        }
    }

    @Test
    public void testMathACosFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.acos(i);
        }
    }

    @Test
    public void testMathSin() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.sin(i);
        }
    }

    @Test
    public void testMathSinFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.sin(i);
        }
    }

    @Test
    public void testMathASin() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.asin(i);
        }
    }

    @Test
    public void testMathASinFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.asin(i);
        }
    }

    @Test
    public void testMathTan() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.tan(i);
        }
    }

    @Test
    public void testMathTanFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.tan(i);
        }
    }

    @Test
    public void testMathATan() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.atan(i);
        }
    }

    @Test
    public void testMathATanFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.atan(i);
        }
    }

    @Test
    public void testMathATan2() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.atan2(i, i);
        }
    }

    @Test
    public void testMathATan2Fast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.atan2(i, i);
        }
    }

    @Test
    public void testMathTanH() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.tanh(i);
        }
    }

    @Test
    public void testMathTanHFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.tanh(i);
        }
    }

    @Test
    public void testMathMin() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.min(i, 500);
        }
    }

    @Test
    public void testMathMinFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.min(i, 500);
        }
    }

    @Test
    public void testMathMax() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.max(i, 500);
        }
    }

    @Test
    public void testMathMaxFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.max(i, 500);
        }
    }

    @Test
    public void testMathCeil() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.ceil(i);
        }
    }

    @Test
    public void testMathCeilFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.ceil(i);
        }
    }

    @Test
    public void testMathFloor() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.floor(i);
        }
    }

    @Test
    public void testMathFloorFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.floor(i);
        }
    }

    @Test
    public void testMathAbs() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.abs(i);
        }
    }

    @Test
    public void testMathAbsFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.abs(i);
        }
    }

    @Test
    public void testMathAbsStrict() {
        for (double i = 0; i < numItr; ++i) {
            double val = StrictMath.abs(i);
        }
    }

    @Test
    public void testMathRound() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.round(i);
        }
    }

    @Test
    public void testMathRoundFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.round(i);
        }
    }

    @Test
    public void testMathPow() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.pow(i, i);
        }
    }

    @Test
    public void testMathPowFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.pow(i, i);
        }
    }

    @Test
    public void testMathLog() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.log(i);
        }
    }

    @Test
    public void testMathLogFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.log(i);
        }
    }

    @Test
    public void testMathLog10() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.log10(i);
        }
    }

    @Test
    public void testMathLog10Fast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.log10(i);
        }
    }

    @Test
    public void testMathExp() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.exp(i);
        }
    }

    @Test
    public void testMathExpFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.exp(i);
        }
    }

    @Test
    public void testMathSqrt() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.sqrt(i);
        }
    }

    @Test
    public void testMathSqrtFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.sqrt(i);
        }
    }

    @Test
    public void testMathRint() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.rint(i);
        }
    }

    @Test
    public void testMathRintFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.rint(i);
        }
    }

    @Test
    public void testMathToDegrees() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.toDegrees(i);
        }
    }

    @Test
    public void testMathToDegreesFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.toDegrees(i);
        }
    }

    @Test
    public void testMathToRadians() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.toRadians(i);
        }
    }

    @Test
    public void testMathToRadiansFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.toRadians(i);
        }
    }

    @Test
    public void testMathHypot() {
        for (double i = 0; i < numItr; ++i) {
            double val = Math.hypot(i, i);
        }
    }

    @Test
    public void testMathHypotFast() {
        for (double i = 0; i < numItr; ++i) {
            double val = FastMath.hypot(i, i);
        }
    }
}
