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

package org.esa.snap.core.util.math;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.util.Guardian;

/**
 * Instances of the <code>Range</code> class provide a minimum and a maximum value of type <code>double</code>.
 */
public class Range {

    private double min;
    private double max;

    /**
     * Constructs a new range object. Minimum and maximum are set to zero.
     */
    public Range() {
    }

    /**
     * Constructs a new range object with the given minimum and maximum.
     *
     * @param min the minimum value
     * @param max the maximum value
     */
    public Range(double min, double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Gets the mimimum value.
     *
     * @return the mimimum value
     */
    public double getMin() {
        return min;

    }

    /**
     * Sets the mimimum value.
     *
     * @param min the mimimum value
     */
    public void setMin(double min) {
        this.min = min;
    }

    /**
     * Gets the maximum value.
     *
     * @return the maximum value
     */
    public double getMax() {
        return max;
    }

    /**
     * Sets the maximum value.
     *
     * @param max the maximum value
     */
    public void setMax(double max) {
        this.max = max;
    }

    /**
     * Sets the mimimum and maximum value.
     *
     * @param min the mimimum value
     * @param max the maximum value
     */
    public void setMinMax(double min, double max) {
        setMin(min);
        setMax(max);
    }

    /**
     * Checks if this range is valid.
     *
     * @return <code>true</code> if minimum value is greater than the maximum value
     */
    public boolean isValid() {
        return Math.abs(max - min) < MathUtils.EPS;
    }

    // @todo se/nf - add documentation
    public void aggregate(final Object values, boolean unsigned,
                          final IndexValidator validator, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        final Range range = computeRangeGeneric(values, unsigned, validator, null, pm);
        setMax(Math.max(getMax(), range.getMax()));
        setMin(Math.min(getMin(), range.getMin()));
    }

    /**
     * Computes the value range for the values in the given <code>byte</code> array. The array elements are interpreted
     * as <i>signed</i> byte values. Values at a given index <code>i</code> for which <code>validator.validate(i)</code>
     * returns <code>false</code> are excluded from the computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given array
     */
    public static Range computeRangeByte(final byte[] values,
                                         final IndexValidator validator,
                                         Range range,
                                         ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.Byte(values), validator, range, pm);
    }

    /**
     * Computes the value range for the values in the given <code>byte</code> array. The array elements are interpreted
     * as <i>unsigned</i> byte values. Values at a given index <code>i</code> for which
     * <code>validator.validate(i)</code> returns <code>false</code> are excluded from the computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given array
     */
    public static Range computeRangeUByte(final byte[] values,
                                          final IndexValidator validator,
                                          Range range,
                                          ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.UByte(values), validator, range, pm);

    }

    /**
     * Computes the value range for the values in the given <code>short</code> array. The array elements are interpreted
     * as <i>signed</i> short values. Values at a given index <code>i</code> for which
     * <code>validator.validate(i)</code> returns <code>false</code> are excluded from the computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given array
     */
    public static Range computeRangeShort(final short[] values,
                                          final IndexValidator validator,
                                          Range range,
                                          ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.Short(values), validator, range, pm);
    }

    /**
     * Computes the value range for the values in the given <code>short</code> array. The array elements are interpreted
     * as <i>unsigned</i> short values. Values at a given index <code>i</code> for which
     * <code>validator.validate(i)</code> returns <code>false</code> are excluded from the computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     *
     * @return the value range for the given array
     */
    public static Range computeRangeUShort(final short[] values,
                                           final IndexValidator validator,
                                           Range range, ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.UShort(values), validator, range, pm);
    }

    /**
     * Computes the value range for the values in the given <code>int</code> array. The array elements are interpreted
     * as <i>signed</i> int values. Values at a given index <code>i</code> for which <code>validator.validate(i)</code>
     * returns <code>false</code> are excluded from the computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given array
     */
    public static Range computeRangeInt(final int[] values,
                                        final IndexValidator validator,
                                        Range range,
                                        ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.Int(values), validator, range, pm);
    }

    /**
     * Computes the value range for the values in the given <code>int</code> array. The array elements are interpreted
     * as <i>unsigned</i> int values. Values at a given index <code>i</code> for which
     * <code>validator.validate(i)</code> returns <code>false</code> are excluded from the computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given array
     */
    public static Range computeRangeUInt(final int[] values,
                                         final IndexValidator validator,
                                         Range range,
                                         ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.UInt(values), validator, range, pm);
    }

    /**
     * Computes the value range for the values in the given <code>float</code> array. Values at a given index
     * <code>i</code> for which <code>validator.validate(i)</code> returns <code>false</code> are excluded from the
     * computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given array
     */
    public static Range computeRangeFloat(final float[] values,
                                          final IndexValidator validator,
                                          Range range,
                                          ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.Float(values), validator, range, pm);
    }

    /**
     * Computes the value range for the values in the given <code>double</code> array. Values at a given index
     * <code>i</code> for which <code>validator.validate(i)</code> returns <code>false</code> are excluded from the
     * computation.
     *
     * @param values    the array whose value range to compute
     * @param validator used to validate the array indexes, must not be <code>null</code>. Use {@link
     *                  IndexValidator#TRUE} instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given array
     */
    public static Range computeRangeDouble(final double[] values,
                                           final IndexValidator validator,
                                           Range range,
                                           ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        return computeRangeDouble(new DoubleList.Double(values), validator, range, pm);
    }

    /**
     * Computes the value range for the values in the given <code>Range.DoubleList</code>. Values at a given index
     * <code>i</code> for which <code>validator.validate(i)</code> returns <code>false</code> are excluded from the
     * computation.
     *
     * @param values    the <code>Range.DoubleList</code> whose value range to compute
     * @param validator used to validate the indexes, must not be <code>null</code>. Use {@link IndexValidator#TRUE}
     *                  instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given values
     */
    public static Range computeRangeDouble(final DoubleList values,
                                           final IndexValidator validator,
                                           Range range,
                                           ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        final int n = values.getSize();
        double min = +Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double value;
        pm.beginTask("Computing range ...", n);
        try {
            for (int i = 0; i < n; i++) {
                if (validator.validateIndex(i)) {
                    value = values.getDouble(i);
                    if (!Double.isNaN(value) && !Double.isInfinite(value)) {
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
        if (range == null) {
            range = new Range(min, max);
        } else {
            range.setMinMax(min, max);
        }
        return range;
    }


    /**
     * Computes the value range for the values in the given <code>Object</code>. Values at a given index <code>i</code>
     * for which <code>validator.validate(i)</code> returns <code>false</code> are excluded from the computation.<br>
     * <br> Supportet types for the values object: <blockquote> <code>byte[], short[], int[], float[], double[], {@link
     * DoubleList}</code></blockquote>
     *
     * @param values    the <code>Object</code> whose value range to compute
     * @param unsigned  if true interprete all the values as unsignet type.
     * @param validator used to validate the indexes, must not be <code>null</code>. Use {@link IndexValidator#TRUE}
     *                  instead.
     * @param range     if not <code>null</code>, used as return value, otherwise a new instance is created
     * @param pm        a monitor to inform the user about progress
     *
     * @return the value range for the given values
     *
     * @throws IllegalArgumentException if the given object is not an istance of the supported types.
     */
    public static Range computeRangeGeneric(final Object values,
                                            boolean unsigned,
                                            final IndexValidator validator,
                                            Range range,
                                            ProgressMonitor pm) {
        Guardian.assertNotNull("validator", validator);
        Range result;
        if (values instanceof byte[]) {
            if (unsigned) {
                result = computeRangeUByte((byte[]) values, validator, range, pm);
            } else {
                result = computeRangeByte((byte[]) values, validator, range, pm);
            }
        } else if (values instanceof short[]) {
            if (unsigned) {
                result = computeRangeUShort((short[]) values, validator, range, pm);
            } else {
                result = computeRangeShort((short[]) values, validator, range, pm);
            }
        } else if (values instanceof int[]) {
            if (unsigned) {
                result = computeRangeUInt((int[]) values, validator, range, pm);
            } else {
                result = computeRangeInt((int[]) values, validator, range, pm);
            }
        } else if (values instanceof float[]) {
            result = computeRangeFloat((float[]) values, validator, range, pm);
        } else if (values instanceof double[]) {
            result = computeRangeDouble((double[]) values, validator, range, pm);
        } else if (values instanceof DoubleList) {
            result = computeRangeDouble((DoubleList) values, validator, range, pm);
        } else if (values == null) {
            throw new IllegalArgumentException("values is null");
        } else {
            throw new IllegalArgumentException("values has an illegal type: " + values.getClass());
        }
        return result;
    }

    @Override
    public String toString() {
        return getMin() + "," + getMax();
    }
}
