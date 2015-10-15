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

import java.awt.Dimension;
import java.awt.Rectangle;


/**
 * A utility class providing frequently used mathematical functions which are not found in the
 * <code>java.lang.Math</code> class.
 * <p> All functions have been implemented with extreme caution in order to provide a maximum performance.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class MathUtils {

    /**
     * The epsilon value for the <code>float</code> data type. The exact value of this constant is <code>1.0E-6</code>.
     */
    public static final float EPS_F = 1.0E-6F;

    /**
     * The epsilon value for the <code>double</code> data type. The exact value of this constant is
     * <code>1.0E-12</code>.
     */
    public static final double EPS = 1.0E-12;

    /**
     * Conversion factor for degrees to radians for the <code>double</code> data type.
     */
    public static final double DTOR = Math.PI / 180.0;

    /**
     * Conversion factor for radians to degrees for the <code>double</code> data type.
     */
    public static final double RTOD = 180.0 / Math.PI;

    /**
     * Conversion factor for degrees to radians for the <code>float</code> data type.
     */
    public static final float DTOR_F = (float) DTOR;

    /**
     * Conversion factor for radians to degrees for the <code>float</code> data type.
     */
    public static final float RTOD_F = (float) RTOD;

    /**
     * The natural logarithm of 10 as given by <code>Math.logging(10)</code>
     */
    public static final double LOG10 = Math.log(10.0);

    /**
     * Pi half
     */
    public static final double HALFPI = Math.PI * 0.5;

    /**
     * Compares two float values for equality within the fixed epsilon.
     *
     * @param x1 the first value
     * @param x2 the second value
     */
    public static boolean equalValues(final float x1,
                                      final float x2) {
        return Math.abs(x1 - x2) <= EPS_F;
    }

    /**
     * Compares two double values for equality within the fixed epsilon.
     *
     * @param x1 the first value
     * @param x2 the second value
     */
    public static boolean equalValues(final double x1,
                                      final double x2) {
        return Math.abs(x1 - x2) <= EPS;
    }

    /**
     * Compares two float values for equality within the given epsilon.
     *
     * @param x1  the first value
     * @param x2  the second value
     * @param eps the maximum allowed difference
     */
    public static boolean equalValues(final float x1,
                                      final float x2,
                                      final float eps) {
        return Math.abs(x1 - x2) <= eps;
    }

    /**
     * Compares two double values for equality within the given epsilon.
     *
     * @param x1  the first value
     * @param x2  the second value
     * @param eps the maximum allowed difference
     */
    public static boolean equalValues(final double x1,
                                      final double x2,
                                      final double eps) {
        return Math.abs(x1 - x2) <= eps;
    }

    /**
     * Performs a fast linear interpolation in two dimensions i and j.
     *
     * @param wi  weight in i-direction, a weight of 0.0 corresponds to i, 1.0 to i+1
     * @param wj  weight in j-direction, a weight of 0.0 corresponds to j, 1.0 to j+1
     * @param x00 first anchor point located at (i,j)
     * @param x10 second anchor point located at (i+1,j)
     * @param x01 third anchor point located at (i,j+1)
     * @param x11 forth anchor point located at (i+1,j+1)
     *
     * @return the interpolated value
     */
    public static float interpolate2D(final float wi,
                                      final float wj,
                                      final float x00,
                                      final float x10,
                                      final float x01,
                                      final float x11) {
        return x00 + wi * (x10 - x00) + wj * (x01 - x00) + wi * wj * (x11 + x00 - x01 - x10);
    }

    /**
     * Performs a fast linear interpolation in two dimensions i and j.
     *
     * @param wi  weight in i-direction, a weight of 0.0 corresponds to i, 1.0 to i+1
     * @param wj  weight in j-direction, a weight of 0.0 corresponds to j, 1.0 to j+1
     * @param x00 first anchor point located at (i,j)
     * @param x10 second anchor point located at (i+1,j)
     * @param x01 third anchor point located at (i,j+1)
     * @param x11 forth anchor point located at (i+1,j+1)
     *
     * @return the interpolated value
     */
    public static double interpolate2D(final double wi,
                                       final double wj,
                                       final double x00,
                                       final double x10,
                                       final double x01,
                                       final double x11) {
        return x00 + wi * (x10 - x00) + wj * (x01 - x00) + wi * wj * (x11 + x00 - x01 - x10);
    }

    /**
     * First calls <code>Math.floor</code> with <code>x</code> and then crops the resulting value to the range
     * <code>min</code> to <code>max</code>.
     */
    public static int floorAndCrop(final double x, final int min, final int max) {
        final int rx = floorInt(x);
        return crop(rx, min, max);
//        return (rx < min) ? min : (rx > max) ? max : rx;
    }

    /**
     * First calls <code>Math.round</code> with <code>x</code> and then crops the resulting value to the range
     * <code>min</code> to <code>max</code>.
     */
    public static int roundAndCrop(final float x, final int min, final int max) {
        final int rx = Math.round(x);
        return crop(rx, min, max);
//        return (rx < min) ? min : (rx > max) ? max : rx;
    }

    /**
     * First calls <code>Math.round</code> with <code>value</code> and then crops the resulting value to the range
     * <code>min</code> to <code>max</code>.
     * @param value the value to round and crop
     * @param min the minimum value of the crop range
     * @param max the maximum value of the crop range
     * 
     * @return the rounded and cropped value
     */
    public static long roundAndCrop(final double value, final long min, final long max) {
        final long rx = Math.round(value);
        return crop(rx, min, max);
    }

    /**
     * Returns <code>(int) Math.floor(value)</code>.
     *
     * @param value the <code>double</code> value to be converted
     *
     * @return the integer value corresponding to the floor of <code>value</code>
     */
    public static int floorInt(final double value) {
        return (int) Math.floor(value);
    }

    /**
     * Returns <code>(long) Math.floor(x)</code>.
     *
     * @param x the value to be converted
     *
     * @return the long integer value corresponding to the floor of <code>x</code>
     */
    public static long floorLong(final double x) {
        return (long) Math.floor(x);
    }

    /**
     * Returns <code>(int) Math.ceil(x)</code>.
     *
     * @param x the value to be converted
     *
     * @return the integer value corresponding to the ceil of <code>x</code>
     */
    public static int ceilInt(final double x) {
        return (int) Math.ceil(x);
    }

    /**
     * Returns <code>(long) Math.ceil(x)</code>.
     *
     * @param x the value to be converted
     *
     * @return the long value corresponding to the ceil of <code>x</code>
     */
    public static long ceilLong(final double x) {
        return (long) Math.ceil(x);
    }

    /**
     * Computes a rounding factor suitable for the given value range and number of significant digits.
     *
     * @param min       the minimum value of the range
     * @param max       the maximum value of the range
     * @param numDigits the number of significant digits, must be <code>=0</code>
     *
     * @return the rounded value, always a power to the base 10
     *
     * @see #round(float, float)
     */
    public static float computeRoundFactor(final float min,
                                           final float max,
                                           final int numDigits) {
        return (float) computeRoundFactor((double) min, (double) max, numDigits);
    }

    /**
     * Computes a rounding factor suitable for the given value range and number of significant digits after the decimal
     * point.
     *
     * @param min       the minimum value of the range
     * @param max       the maximum value of the range
     * @param numDigits the number of significant digits after the decimal point, must be <code>=0</code>
     *
     * @return the rounded value, always a power to the base 10
     *
     * @see #round(double, double)
     */
    public static double computeRoundFactor(final double min,
                                            final double max,
                                            int numDigits) {
        double exponent = log10(Math.abs(max - min));
        int numLeadingZeroDigits = -(int) Math.round(exponent);
        if (numLeadingZeroDigits > 0) {
            numDigits += numLeadingZeroDigits;
        }
        return Math.pow(10.0, numDigits);
    }

    /**
     * Computes a rounded value for the given rounding factor. The given value is pre-multiplied with the rounding
     * factor, then rounded to the closest <code>int</code> and then again divided by the the rounding factor.
     * <p>The rounding factor can be computed for a given value range and accuracy with the
     * <code>computeRoundFactor</code>  method.
     *
     * @param x           the value to be rounded
     * @param roundFactor the rounding factor specifying the accuracy, should always be a power to the base 10
     *
     * @return the rounded value
     *
     * @see #computeRoundFactor(float, float, int)
     */
    public static float round(final float x, final float roundFactor) {
        return (float) Math.round(x * roundFactor) / roundFactor;
    }

    /**
     * Computes a rounded value for the given rounding factor. The given value is pre-multiplied with the rounding
     * factor, then rounded to the closest <code>int</code> and then again divided by the the rounding factor.
     * <p>The rounding factor can be computed for a given value range and accuracy with the
     * <code>computeRoundFactor</code>  method.
     *
     * @param x           the value to be rounded
     * @param roundFactor the rounding factor specifying the accuracy, should always be a power to the base 10
     *
     * @return the rounded value
     *
     * @see #computeRoundFactor(double, double, int)
     */
    public static double round(final double x, final double roundFactor) {
        return (double) Math.round(x * roundFactor) / roundFactor;
    }

    /**
     * Returns the order of magnitude for the value <code>x</code>.
     *
     * @param x the input
     *
     * @return the order of magnitude: <code>Math.floor(log10(x))</code>
     */
    public static double getOrderOfMagnitude(double x) {
        return Math.floor(log10(x));
    }

    /**
     * Computes the common logarithm (to the base 10).
     *
     * @param x the input
     *
     * @return the common logarithm: <code>Math.logging(x)/LOG10</code>
     */
    public static double log10(double x) {
        return Math.log(x) / LOG10;
    }


    /**
     * Creates a quantized gamma (correction) curve for 256 samples in range from <code>0</code> to <code>1</code>. The
     * array returned for can be used as a lookup table for gamma transformations. In order to interpret the value
     * correctly use the following code snippet:
     * <pre>
     *    byte[] f = MathUtils.createGammaCurve(gamma, null);
     *    for (int i = 0; i &lt; 256; i++) {
     *         // transform i --&gt; j
     *         j = f[i] &amp; 0xff;
     *         // now use j instead of i
     *    }
     * </pre>
     *
     * @param gamma the gamma value, reasonable range is <code>1/10</code> to <code>10</code>, if <code>1</code> then
     *              each <code>f[i] &amp; 0xff</code> will be <code>i</code>.
     * @param f     the curve as an array of length 256. If not <code>null</code>, the method used this array as returen
     *              value after values have been written into it. If <code>null</code>,  the method creates a new array
     *              and returns it.
     *
     * @return a quantized gamma (correction) curve for 256 samples
     */
    public static byte[] createGammaCurve(double gamma, byte[] f) {
        if (gamma <= 0.0) {
            throw new IllegalArgumentException("gamma was <= zero");
        }
        if (f == null || f.length != 256) {
            f = new byte[256];
        }
        if (gamma == 1.0) {
            for (int i = 0; i < f.length; i++) {
                f[i] = (byte) i;
            }
        } else {
            double x, y;
            int j;
            for (int i = 0; i < f.length; i++) {
                x = (double) i / 255.0;
                y = Math.pow(x, gamma);
                j = (int) (y * 256.0);
                f[i] = (byte) (j < 0 ? 0 : j > 255 ? 255 : j);
            }
        }
        return f;
    }

    /**
     * Crops the value to the range <code>min</code> to <code>max</code>.
     *
     * @param val the value to crop
     * @param min the minimum crop limit
     * @param max the maximum crop limit
     */
    public static byte crop(byte val, byte min, byte max) {
        return val < min ? min : val > max ? max : val;
    }

    /**
     * Crops the value to the range <code>min</code> to <code>max</code>.
     *
     * @param val the value to crop
     * @param min the minimum crop limit
     * @param max the maximum crop limit
     */
    public static short crop(short val, short min, short max) {
        return val < min ? min : val > max ? max : val;
    }

    /**
     * Crops the value to the range <code>min</code> to <code>max</code>.
     *
     * @param val the value to crop
     * @param min the minimum crop limit
     * @param max the maximum crop limit
     */
    public static int crop(int val, int min, int max) {
        return val < min ? min : val > max ? max : val;
    }

    /**
     * Crops the value to the range <code>min</code> to <code>max</code>.
     *
     * @param val the value to crop
     * @param min the minimum crop limit
     * @param max the maximum crop limit
     */
    public static long crop(long val, long min, long max) {
        return val < min ? min : val > max ? max : val;
    }

    /**
     * Crops the value to the range <code>min</code> to <code>max</code>.
     *
     * @param val the value to crop
     * @param min the minimum crop limit
     * @param max the maximum crop limit
     */
    public static float crop(float val, float min, float max) {
        return val < min ? min : val > max ? max : val;
    }

    /**
     * Crops the value to the range <code>min</code> to <code>max</code>.
     *
     * @param val the value to crop
     * @param min the minimum crop limit
     * @param max the maximum crop limit
     */
    public static double crop(double val, double min, double max) {
        return val < min ? min : val > max ? max : val;
    }

    /**
     * Computes an integer dimension for a given integer area that best fits the
     * rectangle given by floating point width and height.
     *
     * @param n the integer area
     * @param a the rectangle's width
     * @param b the rectangle's height
     *
     * @return an integer dimension, never null
     */
    public static Dimension fitDimension(int n, double a, double b) {
        if (n == 0) {
            return new Dimension(0, 0);
        }
        double wd = Math.sqrt(n * a / b);
        double hd = n / wd;
        int w1 = (int) Math.floor(wd);
        int h1 = (int) Math.floor(hd);
        int w2, h2;
        if (w1 > 0) {
            w2 = w1 + 1;
        } else {
            w2 = w1 = 1;
        }
        if (h1 > 0) {
            h2 = h1 + 1;
        } else {
            h2 = h1 = 1;
        }
        double[] d = new double[4];
        d[0] = Math.abs(b * w1 - a * h1);
        d[1] = Math.abs(b * w1 - a * h2);
        d[2] = Math.abs(b * w2 - a * h1);
        d[3] = Math.abs(b * w2 - a * h2);
        int index = -1;
        double dMin = Double.MAX_VALUE;
        for (int i = 0; i < d.length; i++) {
            if (d[i] < dMin) {
                dMin = d[i];
                index = i;
            }
        }
        if (index == 0) {
            return new Dimension(w1, h1);
        } else if (index == 1) {
            return new Dimension(w1, h2);
        } else if (index == 2) {
            return new Dimension(w2, h1);
        } else {
            return new Dimension(w2, h2);
        }
    }

    /**
     * Subdivides a rectangle into tiles. The coordinates of each returned tile rectangle are guaranteed
     * to be within the given rectangle.
     *
     * @param width       the rectangle's width
     * @param height      the rectangle's height
     * @param numTilesX   the number of tiles in X direction
     * @param numTilesY   the number of tiles in Y direction
     * @param extraBorder an extra border size to extend each tile
     *
     * @return the tile coordinates as rectangles
     */
    public static Rectangle[] subdivideRectangle(int width, int height, int numTilesX, int numTilesY, int extraBorder) {
        Rectangle[] rectangles = new Rectangle[numTilesX * numTilesY];
        int k = 0;
        float w = (float) width / numTilesX;
        float h = (float) height / numTilesY;
        for (int j = 0; j < numTilesY; j++) {
            int y1 = (int) Math.floor((j + 0) * h);
            int y2 = (int) Math.floor((j + 1) * h) - 1;
            if (y2 < y1) {
                y2 = y1;
            }
            y1 -= extraBorder;
            y2 += extraBorder;
            if (y1 < 0) {
                y1 = 0;
            }
            if (y2 > height - 1) {
                y2 = height - 1;
            }
            for (int i = 0; i < numTilesX; i++) {
                int x1 = (int) Math.floor((i + 0) * w);
                int x2 = (int) Math.floor((i + 1) * w) - 1;
                if (x2 < x1) {
                    x2 = x1;
                }
                x1 -= extraBorder;
                x2 += extraBorder;
                if (x1 < 0) {
                    x1 = 0;
                }
                if (x2 > width - 1) {
                    x2 = width - 1;
                }

                rectangles[k] = new Rectangle(x1, y1, (x2 - x1) + 1, (y2 - y1) + 1);
                k++;
            }
        }
        return rectangles;
    }

    /**
     * @param sphereRadius the radius of the sphere
     * @param lambda1_deg  the lambda angle of point one, in degree
     * @param phi1_deg     the phi angle of point one, in degree
     * @param lambda2_deg  the lambda angle of point two, in degree
     * @param phi2_deg     the phi angle of point one, in degree
     *
     * @return the distance described by the two given points (lambda/phi) on the
     *         top of sphere described by the given radius
     */
    public final static double sphereDistanceDeg(final double sphereRadius,
                                                 final double lambda1_deg, final double phi1_deg,
                                                 final double lambda2_deg, final double phi2_deg) {
        final double conv = MathUtils.DTOR;
        return sphereDistance(sphereRadius,
                              conv * lambda1_deg, conv * phi1_deg,
                              conv * lambda2_deg, conv * phi2_deg);
    }

    /**
     * @param sphereRadius the radius of the sphere
     * @param lambda1_rad  the lambda angle of point one, in radians
     * @param phi1_rad     the phi angle of point one, in radians
     * @param lambda2_rad  the lambda angle of point two, in radians
     * @param phi2_rad     the phi angle of point one, in radians
     *
     * @return the distance described by the two given points (lambda/phi) on the
     *         top of sphere described by the given radius
     */
    public final static double sphereDistance(final double sphereRadius,
                                              final double lambda1_rad, final double phi1_rad,
                                              final double lambda2_rad, final double phi2_rad) {
        final double deltaLambda = lambda1_rad - lambda2_rad;
        final double cosDeltaLambda = Math.cos(deltaLambda);
        final double sinPhi = Math.sin(phi1_rad) * Math.sin(phi2_rad);
        final double cosPhi = Math.cos(phi1_rad) * Math.cos(phi2_rad);
        return sphereRadius * Math.acos(sinPhi + cosPhi * cosDeltaLambda);
    }
}

