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

package org.esa.beam.dataio.bigtiff.internal;

import org.esa.beam.util.Guardian;

/**
 * TIFF types for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
class TiffType {

    /**
     * 8-bit unsigned integer,
     */
    public static final byte BYTE_TYPE = 1;
    public static final TiffShort BYTE = new TiffShort(BYTE_TYPE);

    /**
     * a byte sequence that contains a 7-bit ASCII code;
     * the last byte must be NUL (binary zero)
     */
    public static final byte ASCII_TYPE = 2;
    public static final TiffShort ASCII = new TiffShort(ASCII_TYPE);

    /**
     * 16-bit (2-byte) unsigned integer.
     */
    public static final byte SHORT_TYPE = 3;
    public static final TiffShort SHORT = new TiffShort(SHORT_TYPE);

    /**
     * 32-bit (4-byte) unsigned integer.
     */
    public static final byte LONG_TYPE = 4;
    public static final TiffShort LONG = new TiffShort(LONG_TYPE);

    /**
     * Two LONGs: the first represents the numerator of a fraction; the second,
     * the denominator.
     */
    public static final byte RATIONAL_TYPE = 5;
    public static final TiffShort RATIONAL = new TiffShort(RATIONAL_TYPE);

    /**
     * An 8-bit signed (twos-complement) integer.
     */
    public static final byte SBYTE_TYPE = 6;
    public static final TiffShort SBYTE = new TiffShort(SBYTE_TYPE);

    /**
     * An 8-bit byte that may contain anything, depending on the definition of
     * the field.
     */
    public static final byte UNDEFINED_TYPE = 7;
    public static final TiffShort UNDEFINED = new TiffShort(UNDEFINED_TYPE);

    /**
     * A 16-bit (2-byte) signed (twos-complement) integer.
     */
    public static final byte SSHORT_TYPE = 8;
    public static final TiffShort SSHORT = new TiffShort(SSHORT_TYPE);

    /**
     * A 32-bit (4-byte) signed (twos-complement) integer.
     */
    public static final byte SLONG_TYPE = 9;
    public static final TiffShort SLONG = new TiffShort(SLONG_TYPE);

    /**
     * Two SLONG's: the first represents the numerator of a fraction, the second
     * the denominator.
     */
    public static final byte SRATIONAL_TYPE = 10;
    public static final TiffShort SRATIONAL = new TiffShort(SRATIONAL_TYPE);

    /**
     * Single precision (4-byte) IEEE format.
     */
    public static final byte FLOAT_TYPE = 11;
    public static final TiffShort FLOAT = new TiffShort(FLOAT_TYPE);

    /**
     * Double precision (8-byte) IEEE format.
     */
    public static final byte DOUBLE_TYPE = 12;
    public static final TiffShort DOUBLE = new TiffShort(DOUBLE_TYPE);

    /**
     * unsigned integer (8-byte)
     */
    public static final byte LONG_8_TYPE = 16;
    public static final TiffShort LONG_8 = new TiffShort(LONG_8_TYPE);

    /**
     * signed integer (8-byte)
     */
    public static final byte SLONG_8_TYPE = 17;
    public static final TiffShort SLONG_8 = new TiffShort(SLONG_8_TYPE);

    public static TiffShort getType(final TiffValue[] values) {
        Guardian.assertNotNull("values", values);
        Guardian.assertGreaterThan("values.length", values.length, 0);
        final TiffValue value = values[0];
        Guardian.assertNotNull("value", value);
        if (value instanceof TiffShort) {
            ensureElementsEqualValueType(values, TiffShort.class);
            return SHORT;
        }
        if (value instanceof TiffUInt) {
            ensureElementsEqualValueType(values, TiffUInt.class);
            return LONG;
        }
        if (value instanceof TiffRational) {
            ensureElementsEqualValueType(values, TiffRational.class);
            return RATIONAL;
        }
        if (value instanceof GeoTiffAscii) {
            ensureElementsEqualValueType(values, GeoTiffAscii.class);
            return ASCII;
        }
        if (value instanceof TiffAscii) {
            ensureElementsEqualValueType(values, TiffAscii.class);
            return ASCII;
        }
        if (value instanceof TiffDouble) {
            ensureElementsEqualValueType(values, TiffDouble.class);
            return DOUBLE;
        }
        if (value instanceof TiffLong) {
            ensureElementsEqualValueType(values, TiffLong.class);
            return SLONG_8;
        }
        throw new IllegalArgumentException("the given type [" + values.getClass() + "] is not supported");
    }

    private static void ensureElementsEqualValueType(final TiffValue[] values, final Class compareClass) {
        for (TiffValue value : values) {
            if (!compareClass.isInstance(value)) {
                throw new IllegalArgumentException("all elements of the given values array must be instances " +
                        "of the same type [" + compareClass.getName() + "]");
            }
        }
    }
}