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
package com.bc.jexp.impl;

/**
 * An extension the the {@link java.lang.Math class}.
 */
public class ExtMath {

    private static final double INV_LN_10 = 1.0 / Math.log(10.0);

    /**
     * Returns the common logarithm, the logarithm with base 10 of a double value. Special cases:
     * <ld>
     * <li>If the argument is NaN or less than zero, then the result is NaN.</li>
     * <li>If the argument is positive infinity, then the result is positive infinity.</li>
     * <li>If the argument is positive zero or negative zero, then the result is negative infinity.</li>
     * </ld>
     *
     * @param x number greater than 0.0.
     *
     * @return the natural logarithm of a.
     *
     * @deprecated since BEAM 4.10,  Use Java Math class
     */
    @Deprecated
    public static double log10(final double x) {
        return Math.log10(x);
    }

    /**
     * Performs a fuzzy equal operation for the two given arguments.
     *
     * @param x1  the first value
     * @param x2  the second value
     * @param eps the maximum deviation
     *
     * @return true, if x1 and x2 are equal
     */
    public static boolean feq(final double x1, final double x2, final double eps) {
        return x1 == x2 || Math.abs(x1 - x2) <= eps;
    }

    /**
     * Performs a fuzzy not-equal operation for the two given arguments.
     *
     * @param x1  the first value
     * @param x2  the second value
     * @param eps the maximum deviation
     *
     * @return true, if x1 and x2 are not equal
     */
    public static boolean fneq(final double x1, final double x2, final double eps) {
        return !feq(x1, x2, eps);
    }

    /**
     * Computes the signum of a given integer. The method returns
     * <ld>
     * <li>-1, if a is negative</li>
     * <li>+1, if a is positive</li>
     * <li>0, if a is zero.</li>
     * </ld>
     *
     * @param a the number
     *
     * @return the signum of a
     */
    public static int sign(int a) {
        return a == 0 ? 0 : (a < 0 ? -1 : 1);
    }

    /**
     * Computes the signum of a given long integer. The method returns
     * <ld>
     * <li>-1, if a is negative</li>
     * <li>+1, if a is positive</li>
     * <li>0, if a is zero.</li>
     * </ld>
     *
     * @param a the number
     *
     * @return the signum of a
     */
    public static long sign(long a) {
        return a == 0L ? 0L : (a < 0L ? -1L : 1L);
    }

    /**
     * Computes the signum of a given number. The method returns
     * <ld>
     * <li>-1, if a is negative</li>
     * <li>+1, if a is positive</li>
     * <li>0, if a is zero</li>
     * <li>NaN, if a is NaN.</li>
     * </ld>
     *
     * @param a the number
     *
     * @return the signum of a
     */
    public static float sign(float a) {
        if (Float.isNaN(a)) {
            return Float.NaN;
        }
        return a == 0.0f ? 0.0f : (a < 0.0f ? -1.0f : 1.0f);
    }

    /**
     * Computes the signum of a given number. The method returns
     * <ld>
     * <li>-1, if a is negative</li>
     * <li>+1, if a is positive</li>
     * <li>0, if a is zero</li>
     * <li>NaN, if a is NaN.</li>
     * </ld>
     *
     * @param a the number
     *
     * @return the signum of a
     */
    public static double sign(double a) {
        if (Double.isNaN(a)) {
            return Double.NaN;
        }
        return a == 0.0 ? 0.0 : (a < 0.0 ? -1.0 : 1.0);
    }
}
