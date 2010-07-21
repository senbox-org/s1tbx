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

package org.esa.beam.util.math;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.util.Guardian;

/**
 * Instances of the <code>Statistics</code> class provide a set of standard statistical variables.
 *
 * @author Norman Fomferra
 */
public class Statistics {

    private long _numTotal;
    private long _num;
    private double _min;
    private double _max;
    private double _sum;
    private double _sqrSum;
    private double _mean;
    private double _var;
    private double _stdDev;

    public Statistics() {
    }

    /**
     * Gets the minimum value.
     *
     * @return the minimum value
     */
    public double getMin() {
        return _min;
    }

    /**
     * Sets the minimum value.
     *
     * @param min the minimum value
     */
    protected void setMin(double min) {
        _min = min;
    }

    /**
     * Gets the maximum value.
     *
     * @return the maximum value
     */
    public double getMax() {
        return _max;
    }

    /**
     * Sets the maximum value.
     *
     * @param max the maximum value
     */
    protected void setMax(double max) {
        _max = max;
    }

    /**
     * Gets the total number of all values.
     *
     * @return the total number
     */
    public long getNumTotal() {
        return _numTotal;
    }

    /**
     * Sets the total number of all values.
     *
     * @param numTotal the total number
     */
    protected void setNumTotal(long numTotal) {
        _numTotal = numTotal;
    }

    /**
     * Gets the number of all considered values.
     *
     * @return the number
     */
    public long getNum() {
        return _num;
    }

    /**
     * Sets the number of all considered values.
     *
     * @param num the number
     */
    protected void setNum(long num) {
        _num = num;
    }

    /**
     * Gets the ratio of the number of all considered values to all values.
     *
     * @return the ratio
     */
    public double getRatio() {
        return (double) getNum() / (double) getNumTotal();
    }

    /**
     * Gets the sum of all considered values.
     *
     * @return the sum
     */
    public double getSum() {
        return _sum;
    }

    /**
     * Sets the sum of all considered values.
     *
     * @param sum the sum
     */
    protected void setSum(double sum) {
        _sum = sum;
    }

    /**
     * Gets the sum of the squares of all considered values.
     *
     * @return the sum of the squares
     */
    public double getSqrSum() {
        return _sqrSum;
    }

    /**
     * Sets the sum of the squares of all considered values.
     *
     * @param sqrSum the sum of the squares
     */
    protected void setSqrSum(double sqrSum) {
        _sqrSum = sqrSum;
    }

    /**
     * Gets the mean value.
     *
     * @return the mean value
     */
    public double getMean() {
        return _mean;
    }

    /**
     * Sets the mean value.
     *
     * @param mean the mean value
     */
    protected void setMean(double mean) {
        _mean = mean;
    }

    /**
     * Gets the variance value.
     *
     * @return the variance value
     */
    public double getVar() {
        return _var;
    }

    /**
     * Sets the variance value.
     *
     * @param var the variance value
     */
    protected void setVar(double var) {
        _var = var;
    }

    /**
     * Gets the standard deviation value.
     *
     * @return the standard deviation value
     */
    public double getStdDev() {
        return _stdDev;
    }

    /**
     * Sets the standard deviation value.
     *
     * @param stdDev the standard deviation value
     */
    protected void setStdDev(double stdDev) {
        _stdDev = stdDev;
    }

    /**
     * Sets multiple base properties of this statistics container. The mean, variance and the standard deviation are
     * derived from the number of considered values (<code>num</code>), the sum (<code>sum</code>) and the square sum
     * (<code>sqrSum</code>) of all considered values.
     *
     * @param numTotal the total number of values
     * @param num      the  number of the considerd values
     * @param sum      the sum  of the considerd values
     * @param sqrSum   the square sum  of the  considerd values
     * @param min      the minimum of the considerd values
     * @param max      the maximum of the considerd values
     */
    public void set(long numTotal,
                    long num,
                    double sum,
                    double sqrSum,
                    double min,
                    double max) {
        setNum(num);
        setNumTotal(numTotal);
        setSum(sum);
        setSqrSum(sqrSum);
        setMin(min);
        setMax(max);

        double mean = Double.NaN;
        double var = Double.NaN;
        double stdDev = Double.NaN;
        if (num > 0) {
            mean = sum / num;
            var = 0.0;
            if (num > 1) {
                var = 1.0 / (num - 1) * (sqrSum - num * mean * mean);
            }
            stdDev = Math.sqrt(var);
        }

        setMean(mean);
        setVar(var);
        setStdDev(stdDev);
    }

    /**
     * @deprecated in 4.0, use {@link #computeStatisticsGeneric(Object, boolean, IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsGeneric(final Object values, boolean unsigned,
                                                      final IndexValidator validator,
                                                      Statistics statistics) {
        return computeStatisticsGeneric(values, unsigned, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * Computes the statistics for the given values.
     *
     * @param values     an array of a primitive type (such as <code>float[]</code>) or an instance of {@link
     *                   DoubleList}. Must not be null.
     * @param unsigned   if true, integer type arrays are considered to contain unsigned values, e.g. a
     *                   <code>byte[]</code> element is considered to be in the range 0...255
     * @param validator  a validator used validate a the indexes <code>0...values.length - 1</code>, must not be
     *                   <code>null</code>. Use {@link IndexValidator#TRUE} instead.
     * @param statistics the instance to be modified and returned. If null, a new instance will be created, set and
     *                   returned.
     *
     * @return a new statistics instance if <code>statistics</code> is null, or the modified instance
     *         <code>statistics</code>
     */
    public static Statistics computeStatisticsGeneric(final Object values, boolean unsigned,
                                                      final IndexValidator validator,
                                                      Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        if (values instanceof byte[]) {
            if (unsigned) {
                statistics = computeStatisticsUByte((byte[]) values, validator, statistics, pm);
            } else {
                statistics = computeStatisticsByte((byte[]) values, validator, statistics, pm);
            }
        } else if (values instanceof short[]) {
            if (unsigned) {
                statistics = computeStatisticsUShort((short[]) values, validator, statistics, pm);
            } else {
                statistics = computeStatisticsShort((short[]) values, validator, statistics, pm);
            }
        } else if (values instanceof int[]) {
            if (unsigned) {
                statistics = computeStatisticsUInt((int[]) values, validator, statistics, pm);
            } else {
                statistics = computeStatisticsInt((int[]) values, validator, statistics, pm);
            }
        } else if (values instanceof long[]) {
            if (unsigned) {
                statistics = computeStatisticsULong((long[]) values, validator, statistics, pm);
            } else {
                statistics = computeStatisticsLong((long[]) values, validator, statistics, pm);
            }
        } else if (values instanceof float[]) {
            statistics = computeStatisticsFloat((float[]) values, validator, statistics, pm);
        } else if (values instanceof double[]) {
            statistics = computeStatisticsDouble((double[]) values, validator, statistics, pm);
        } else if (values instanceof DoubleList) {
            statistics = computeStatisticsDouble((DoubleList) values, validator, statistics, pm);
        } else if (values == null) {
            throw new IllegalArgumentException("values is null");
        } else {
            throw new IllegalArgumentException("values has an illegal type: " + values.getClass());
        }
        return statistics;
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsByte(byte[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsByte(final byte[] values, final IndexValidator validator,
                                                   Statistics statistics) {
        return computeStatisticsByte(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsByte(final byte[] values, final IndexValidator validator,
                                                   Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.Byte(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsUByte(byte[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsUByte(final byte[] values, final IndexValidator validator,
                                                    Statistics statistics) {
        return computeStatisticsUByte(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsUByte(final byte[] values, final IndexValidator validator,
                                                    Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.UByte(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsShort(short[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsShort(final short[] values, final IndexValidator validator,
                                                    Statistics statistics) {
        return computeStatisticsShort(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsShort(final short[] values, final IndexValidator validator,
                                                    Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.Short(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsUShort(short[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsUShort(final short[] values, final IndexValidator validator,
                                                     Statistics statistics) {
        return computeStatisticsUShort(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsUShort(final short[] values, final IndexValidator validator,
                                                     Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.UShort(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsInt(int[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsInt(final int[] values, final IndexValidator validator,
                                                  Statistics statistics) {
        return computeStatisticsInt(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsInt(final int[] values, final IndexValidator validator,
                                                  Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.Int(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, {@link #computeStatisticsUInt(int[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsUInt(final int[] values, final IndexValidator validator,
                                                   Statistics statistics) {
        return computeStatisticsUInt(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsUInt(final int[] values, final IndexValidator validator,
                                                   Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.UInt(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsLong(long[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsLong(final long[] values, final IndexValidator validator,
                                                   Statistics statistics) {
        return computeStatisticsLong(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsLong(final long[] values, final IndexValidator validator,
                                                   Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.Long(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsULong(long[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsULong(final long[] values, final IndexValidator validator,
                                                    Statistics statistics) {
        return computeStatisticsULong(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsULong(final long[] values, final IndexValidator validator,
                                                    Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.ULong(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsFloat(float[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsFloat(final float[] values, final IndexValidator validator,
                                                    Statistics statistics) {
        return computeStatisticsFloat(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsFloat(final float[] values, final IndexValidator validator,
                                                    Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.Float(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsDouble(double[], IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsDouble(final double[] values, final IndexValidator validator,
                                                     Statistics statistics) {
        return computeStatisticsDouble(new DoubleList.Double(values), validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsDouble(final double[] values, final IndexValidator validator,
                                                     Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeStatisticsDouble(new DoubleList.Double(values), validator, statistics, pm);
    }

    /**
     * @see #computeStatisticsGeneric
     * @deprecated in 4.0, use {@link #computeStatisticsDouble(DoubleList, IndexValidator, Statistics, com.bc.ceres.core.ProgressMonitor)} instead
     */
    public static Statistics computeStatisticsDouble(final DoubleList values, final IndexValidator validator,
                                                     Statistics statistics) {

        return computeStatisticsDouble(values, validator, statistics, ProgressMonitor.NULL);
    }

    /**
     * @see #computeStatisticsGeneric
     */
    public static Statistics computeStatisticsDouble(final DoubleList values, final IndexValidator validator,
                                                     Statistics statistics, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        long numTotal = values.getSize();
        long num = 0;
        double sum = 0.0;
        double sqrSum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double mean = Double.NaN;
        double var = Double.NaN;
        double stdDev = Double.NaN;

        double value;

        pm.beginTask("Computing statistic ...", (int) numTotal);
        try {
            for (int i = 0; i < numTotal; i++) {
                if (pm.isCanceled()) {
                    break;
                }
                if (validator == null || validator.validateIndex(i)) {
                    value = values.getDouble(i);
                    if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                        num++;
                        sum += value;
                        sqrSum += value * value;
                        if (value < min) {
                            min = value;
                        }
                        if (value > max) {
                            max = value;
                        }
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
        if (num > 0) {
            mean = sum / num;
            var = 0.0;
            if (num > 1) {
                var = 1.0 / (num - 1) * (sqrSum - num * mean * mean);
            }
            stdDev = Math.sqrt(var);
        }

        if (statistics == null) {
            statistics = new Statistics();
        }
        statistics.setNum(num);
        statistics.setNumTotal(numTotal);
        statistics.setSum(sum);
        statistics.setSqrSum(sqrSum);
        statistics.setMin(min);
        statistics.setMax(max);
        statistics.setMean(mean);
        statistics.setVar(var);
        statistics.setStdDev(stdDev);

        return statistics;

    }


    public static Statistics computeStatistics(final Statistics[] statisticsArray, Statistics statistics) {
        long numTotal = 0;
        long num = 0;
        double sum = 0.0;
        double sqrSum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < statisticsArray.length; i++) {
            Statistics s = statisticsArray[i];
            numTotal += s.getNumTotal();
            num += s.getNum();
            sum += s.getSum();
            sqrSum += s.getSqrSum();
            min = Math.min(min, s.getMin());
            max = Math.max(max, s.getMax());
        }

        if (statistics == null) {
            statistics = new Statistics();
        }
        statistics.set(numTotal, num, sum, sqrSum, min, max);
        return statistics;
    }

}
