/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.pixex.calvalus.ma;

/**
 * Basically this class holds a number of statistics derived from an arry of numbers.
 * The class satisfies the {@link Number} contract by providing its 'mean' value.
 *
 * @author MarcoZ
 * @author Norman
 */

// todo Copied from Calvalus: Move to BEAM core!?

public final class AggregatedNumber extends Number {

    /**
     * N is the number of "good" values that have been used to compute min, max, mean and stdDev.
     * <ol>
     * <li>not {@code NaN},</li>
     * <li>not masked out by a user-provided valid-mask.</li>
     * </ol>
     */
    public final int n;

    /**
     * NT is the total number of values that were not NaN.
     * In the BEAM data model: the pixels whose "validMask" is set.
     */
    public final int nT;

    /**
     * NF is the number of values x that have been filtered out since they do not satisfy the condition
     * ({@link #mean} - a * {@link #sigma}) &lt; x &lt;  ({@link #mean} + a * {@link #sigma}), where a is most likely
     * 1.5.
     */
    public final int nF;

    /**
     * The minimum value of all "good" values (see {@link #n}).
     */
    public final double min;

    /**
     * The maximum value of all "good" values (see {@link #n}).
     */
    public final double max;

    /**
     * The mean of the "good" values (see {@link #n}).
     */
    public final double mean;

    /**
     * The standard deviation of the "good" values (see {@link #n}).
     */
    public final double sigma;

    /**
     * The coefficient of variance is {@link #sigma} / {@link #mean}.
     */
    public final double cv;

    /**
     * The data that have been used to generate this object.
     */
    public final float[] data;

    public AggregatedNumber(int n, int nT, int nF,
                            double min,
                            double max,
                            double mean,
                            double sigma) {
        this(n, nT, nF, min, max, mean, sigma, null);
    }

    public AggregatedNumber(int n, int nT, int nF,
                            double min,
                            double max,
                            double mean,
                            double sigma,
                            float[] data) {
        this.nT = nT;
        this.min = min;
        this.max = max;
        this.n = n;
        this.nF = nF;
        this.mean = mean;
        this.sigma = sigma;
        this.cv = sigma / mean;
        this.data = data;
    }

    @Override
    public int intValue() {
        return (int) Math.round(mean);
    }

    @Override
    public long longValue() {
        return Math.round(mean);
    }

    @Override
    public float floatValue() {
        return (float) mean;
    }

    @Override
    public double doubleValue() {
        return mean;
    }

    @Override
    public String toString() {
        return String.valueOf(floatValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AggregatedNumber that = (AggregatedNumber) o;

        if (Double.compare(that.max, max) != 0) {
            return false;
        }
        if (Double.compare(that.mean, mean) != 0) {
            return false;
        }
        if (Double.compare(that.min, min) != 0) {
            return false;
        }
        if (n != that.n) {
            return false;
        }
        if (nF != that.nF) {
            return false;
        }
        if (nT != that.nT) {
            return false;
        }
        if (Double.compare(that.sigma, sigma) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = n;
        result = 31 * result + nT;
        result = 31 * result + nF;
        temp = min != +0.0d ? Double.doubleToLongBits(min) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = max != +0.0d ? Double.doubleToLongBits(max) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = mean != +0.0d ? Double.doubleToLongBits(mean) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = sigma != +0.0d ? Double.doubleToLongBits(sigma) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
