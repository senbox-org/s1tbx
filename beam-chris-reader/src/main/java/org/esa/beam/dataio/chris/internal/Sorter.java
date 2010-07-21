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

package org.esa.beam.dataio.chris.internal;

/**
 * Utility class for sorting numbers.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class Sorter {

    /**
     * Returns the nth smallest element of an array.
     *
     * @param a the array.
     * @param n the ordinal number of the array element to be found.
     *
     * @return the nth smalles array element.  Note that on return the
     *         input array is arranged so that all elements with array
     *         index {@code i < n} are less than the returned element
     *         while all elements with index {@code i > n} are greater.
     *
     * @throws NullPointerException     if the input array is {@code null}.
     * @throws IllegalArgumentException if the input array is empty,
     *                                  if {@code n < 0}, or
     *                                  if {@code n >= a.length}.
     */
    public static double nthElement(double a[], int n) {
        if (a.length == 0) {
            throw new IllegalArgumentException("a.length == 0");
        }
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        }
        if (n >= a.length) {
            throw new IllegalArgumentException("n >= a.length");
        }

        return nthElement(a, n, 0, a.length - 1);
    }

    private static double nthElement(double a[], int n, int first, int last) {
        partioning:
        for (int k = first, l = last; ;) {
            if (l > k + 1) {
                swap(a, (k + l) >> 1, k + 1);

                if (a[k] > a[l]) {
                    swap(a, k, l);
                }
                if (a[k + 1] > a[l]) {
                    swap(a, k + 1, l);
                }
                if (a[k] > a[k + 1]) {
                    swap(a, k, k + 1);
                }

                for (int i = k + 1, j = l; ;) {
                    do {
                        ++i;
                    } while (a[i] < a[k + 1]);
                    do {
                        --j;
                    } while (a[j] > a[k + 1]);

                    if (j < i) {
                        swap(a, k + 1, j);
                        if (j >= n) {
                            l = j - 1;
                        }
                        if (j <= n) {
                            k = i;
                        }
                        continue partioning;
                    } else {
                        swap(a, i, j);
                    }
                }
            } else {
                if (l == k + 1 && a[l] < a[k]) {
                    swap(a, k, l);
                }
                return a[n];
            }
        }
    }

    private static void swap(double[] a, int i, int j) {
        final double t = a[i];

        a[i] = a[j];
        a[j] = t;
    }

}
