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

package org.esa.snap.dataio.geotiff.internal;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Guardian;

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

    public static short getBytesForType(final TiffShort type) {
        switch (type.getValue()) {
        case BYTE_TYPE:
        case SBYTE_TYPE:
        case ASCII_TYPE:
        case UNDEFINED_TYPE:
            return 1;
        case SHORT_TYPE:
        case SSHORT_TYPE:
            return 2;
        case LONG_TYPE:
        case SLONG_TYPE:
        case FLOAT_TYPE:
            return 4;
        case RATIONAL_TYPE:
        case SRATIONAL_TYPE:
        case DOUBLE_TYPE:
            return 8;
        default:
            throw new IllegalArgumentException("illegal tiff data type");
        }
    }

    public static TiffShort getType(final TiffValue[] values) {
        Guardian.assertNotNull("values", values);
        Guardian.assertGreaterThan("values.length", values.length, 0);
        final TiffValue value = values[0];
        Guardian.assertNotNull("value", value);
        if (value instanceof TiffShort) {
            ensureElementsEqualValueType(values, TiffShort.class);
            return SHORT;
        }
        if (value instanceof TiffLong) {
            ensureElementsEqualValueType(values, TiffLong.class);
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
        throw new IllegalArgumentException("the given type [" + values.getClass() + "] is not supported");
    }

    private static void ensureElementsEqualValueType(final TiffValue[] values, final Class compareClass) {
        for (int i = 0; i < values.length; i++) {
            if (!compareClass.isInstance(values[i])) {
                throw new IllegalArgumentException("all elements of the given values array must be instances " +
                                                   "of the same type [" + compareClass.getName() + "]");
            }
        }
    }

    public static TiffShort getTiffTypeFrom(final Band band) {
        final int dataType = band.getGeophysicalDataType();
        switch (dataType) {
        case ProductData.TYPE_UINT8:
            return TiffType.BYTE;
        case ProductData.TYPE_UINT16:
            return TiffType.SHORT;
        case ProductData.TYPE_UINT32:
            return TiffType.LONG;
        case ProductData.TYPE_INT8:
            return TiffType.SBYTE;
        case ProductData.TYPE_INT16:
            return TiffType.SSHORT;
        case ProductData.TYPE_INT32:
            return TiffType.SLONG;
        case ProductData.TYPE_FLOAT32:
            return TiffType.FLOAT;
        case ProductData.TYPE_FLOAT64:
            return TiffType.DOUBLE;
        default:
            throw new IllegalArgumentException("the given band has an unsupported geophysical data type");
        }
    }
}
