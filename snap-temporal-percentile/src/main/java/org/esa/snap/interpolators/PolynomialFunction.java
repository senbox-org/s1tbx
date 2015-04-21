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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Immutable representation of a real polynomial function with real coefficients.
 * <p>
 * <a href="http://mathworld.wolfram.com/HornersMethod.html">Horner's Method</a>
 * is used to evaluate the function.
 */
class PolynomialFunction implements Serializable {

    /**
     * Serialization identifier
     */
    private static final long serialVersionUID = -7726511984200295583L;
    /**
     * The coefficients of the polynomial, ordered by degree -- i.e.,
     * coefficients[0] is the constant term and coefficients[n] is the
     * coefficient of x^n where n is the degree of the polynomial.
     */
    private final double[] coefficients;

    /**
     * Construct a polynomial with the given coefficients.  The first element
     * of the coefficients array is the constant term.  Higher degree
     * coefficients follow in sequence.  The degree of the resulting polynomial
     * is the index of the last non-null element of the array, or 0 if all elements
     * are null.
     * <p>
     * The constructor makes a copy of the input array and assigns the copy to
     * the coefficients property.
     *
     * @param c Polynomial coefficients.
     * @throws NullPointerException if {@code c} is {@code null}.
     * @throws IllegalArgumentException if {@code c} is empty.
     */
    public PolynomialFunction(double[] c) {
        super();

        int n = c.length;
        if (n == 0) {
            throw new IllegalArgumentException("empty polynomials coefficients array");
        }
        while ((n > 1) && (c[n - 1] == 0)) {
            --n;
        }
        this.coefficients = new double[n];
        System.arraycopy(c, 0, this.coefficients, 0, n);
    }

    /**
     * Compute the value of the function for the given argument.
     * <p>
     *  The value returned is <br/>
     *  <code>coefficients[n] * x^n + ... + coefficients[1] * x  + coefficients[0]</code>
     *
     *
     * @param x the argument for which the function value should be computed.
     *
     * @return the value of the polynomial at the given point.
     */
    public double value(double x) {
       return evaluate(coefficients, x);
    }

    /**
     * Uses Horner's Method to evaluate the polynomial with the given coefficients at
     * the argument.
     *
     * @param coefficients the coefficients of the polynomial to evaluate.
     * @param argument the input value.
     *
     * @return the value of the polynomial.
     *
     * @throws IllegalArgumentException if {@code coefficients} is empty.
     * @throws NullPointerException if {@code coefficients} is {@code null}.
     */
    protected static double evaluate(double[] coefficients, double argument) {
        int n = coefficients.length;
        if (n == 0) {
            throw new IllegalArgumentException("empty polynomials coefficients array");
        }
        double result = coefficients[n - 1];
        for (int j = n - 2; j >= 0; j--) {
            result = argument * result + coefficients[j];
        }
        return result;
    }

    /**
     * Returns a string representation of the polynomial.
     *
     * <p>The representation is user oriented. Terms are displayed lowest
     * degrees first. The multiplications signs, coefficients equals to
     * one and null terms are not displayed (except if the polynomial is 0,
     * in which case the 0 constant term is displayed). Addition of terms
     * with negative coefficients are replaced by subtraction of terms
     * with positive coefficients except for the first displayed term
     * (i.e. we display <code>-3</code> for a constant negative polynomial,
     * but <code>1 - 3 x + x^2</code> if the negative coefficient is not
     * the first one displayed).
     *
     * @return a string representation of the polynomial.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (coefficients[0] == 0.0) {
            if (coefficients.length == 1) {
                return "0";
            }
        } else {
            s.append(Double.toString(coefficients[0]));
        }

        for (int i = 1; i < coefficients.length; ++i) {
            if (coefficients[i] != 0) {
                if (s.length() > 0) {
                    if (coefficients[i] < 0) {
                        s.append(" - ");
                    } else {
                        s.append(" + ");
                    }
                } else {
                    if (coefficients[i] < 0) {
                        s.append("-");
                    }
                }

                double absAi = Math.abs(coefficients[i]);
                if ((absAi - 1) != 0) {
                    s.append(Double.toString(absAi));
                    s.append(' ');
                }

                s.append("x");
                if (i > 1) {
                    s.append('^');
                    s.append(Integer.toString(i));
                }
            }
        }

        return s.toString();
    }

    /**
* {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(coefficients);
        return result;
    }

    /**
* {@inheritDoc}
*/
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PolynomialFunction)) {
            return false;
        }
        PolynomialFunction other = (PolynomialFunction) obj;
        if (!Arrays.equals(coefficients, other.coefficients)) {
            return false;
        }
        return true;
    }

}
