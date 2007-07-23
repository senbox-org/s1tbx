package org.esa.beam.dataio.chris.internal;

/**
 * Utility class.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class Sort {

    /**
     * Returns the nth smallest element of an array.
     *
     * @param a the array. Note that this array is modified so that all elements
     *          with array index less than n are less than a[n] and all elements
     *          with array index greater than n are greater than a[n].
     * @param n the ordinal number of the array element to be found.
     *
     * @return the nth smallest array element.
     *
     * @throws NullPointerException     if the array is {@code null}.
     * @throws IllegalArgumentException if the array is empty.
     * @throws IllegalArgumentException if {@code n <0}.
     * @throws IllegalArgumentException if {@code n >= a.length}.
     */
    public static double nthElement(double a[], int n) {
        return nthElement(a, n, 0, a.length - 1);
    }

    /**
     * Returns the nth smallest element of an array.
     *
     * @param a     the array. Note that this array is modified so that all elements
     *              with array index less than n are less than a[n] and all elements
     *              with array index greater than n are greater than a[n].
     * @param n     the ordinal number of the array element to be found.
     * @param first the index of the first array element to be considered.
     * @param last  the index of the last array element to be considered.
     *
     * @return the nth smalles array element.
     *
     * @throws NullPointerException     if the array is {@code null}.
     * @throws IllegalArgumentException if the array is empty.
     * @throws IllegalArgumentException if {@code n <0}.
     * @throws IllegalArgumentException if {@code n >= a.length}.
     * @throws IllegalArgumentException if {@code first < 0}.
     * @throws IllegalArgumentException if {@code last >= a.length}.
     * @throws IllegalArgumentException if {@code last < first}.
     * @throws IllegalArgumentException if {@code n < first}.
     * @throws IllegalArgumentException if {@code n > last}.
     */
    public static double nthElement(double a[], int n, int first, int last) {
        if (a.length == 0) {
            throw new IllegalArgumentException("a.length == 0");
        }
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        }
        if (n >= a.length) {
            throw new IllegalArgumentException("n >= a.length");
        }
        if (first < 0) {
            throw new IllegalArgumentException("first < 0");
        }
        if (last >= a.length) {
            throw new IllegalArgumentException("last >= a.length");
        }
        if (last < first) {
            throw new IllegalArgumentException("last < first");
        }
        if (n < first) {
            throw new IllegalArgumentException("n < first");
        }
        if (n > last) {
            throw new IllegalArgumentException("n > last");
        }

        for (int k = first, l = last; ;) {
            if (l > k + 1) {
                final int m = (k + l) >> 1;
                swap(a, m, k + 1);

                if (a[k] > a[l]) {
                    swap(a, k, l);
                }
                if (a[k + 1] > a[l]) {
                    swap(a, k + 1, l);
                }
                if (a[k] > a[k + 1]) {
                    swap(a, k, k + 1);
                }

                int i = k + 1;
                int j = l;

                for (; ;) {
                    do {
                        ++i;
                    } while (a[i] < a[k + 1]);
                    do {
                        --j;
                    } while (a[j] > a[k + 1]);
                    if (j < i) {
                        break;
                    }

                    swap(a, i, j);
                }
                swap(a, k + 1, j);

                if (j >= n) {
                    l = j - 1;
                }
                if (j <= n) {
                    k = i;
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
