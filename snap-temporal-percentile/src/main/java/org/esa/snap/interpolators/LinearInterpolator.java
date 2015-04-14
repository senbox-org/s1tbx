/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: THIS FILE HAS BEEN MODIFIED BY BC TO SUIT PARTICULAR NEEDS.
 */
package org.esa.snap.interpolators;

import java.text.MessageFormat;

/**
 * Implements a linear function for interpolation of real univariate functions.
 */
public class LinearInterpolator implements Interpolator {

    private final int minNumPoints = 2;

    @Override
    public int getMinNumPoints() {
        return minNumPoints;
    }

    /**
     * Computes a linear interpolating function for the data set.
     *
     * @param x the arguments for the interpolation points
     * @param y the values for the interpolation points
     * @return a function which interpolates the data set
     * @throws IllegalArgumentException if {@code x} and {@code y} have different sizes.
     * @throws IllegalArgumentException if {@code x} is not sorted in strict increasing order.
     * @throws IllegalArgumentException if the size of {@code x} is smaller than 2.
     */
    public InterpolatingFunction interpolate(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(MessageFormat.format(
                                "Dimension mismatch {0} != {1}.", x.length, y.length));
        }
        if (x.length < minNumPoints) {
            throw new IllegalArgumentException(MessageFormat.format(
                                "{0} points are required, got only {1}.", minNumPoints, x.length));
        }
        // Number of intervals.  The number of data points is n + 1.
        int n = x.length - 1;

        for (int i = 0; i < n; i++) {
            if (x[i] >= x[i + 1]) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "Points {0} and {1} are not strictly increasing ({2} >= {3}).",
                        i, i + 1, x[i], x[i + 1]));
            }
        }

        // Slope of the lines between the datapoints.
        final double[] m = new double[n];
        for (int i = 0; i < n; i++) {
            m[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
        }

        PolynomialFunction[] polynomials = new PolynomialFunction[n];
        final double[] coefficients = new double[2];
        for (int i = 0; i < n; i++) {
            coefficients[0] = y[i];
            coefficients[1] = m[i];
            polynomials[i] = new PolynomialFunction(coefficients);
        }

        return new InterpolatingFunction(x, polynomials);
    }
}
