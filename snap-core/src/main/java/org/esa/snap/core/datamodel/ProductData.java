/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ObjectUtils;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The abstract {@code ProductData} class represents a generic data buffer used to hold the actual data values
 * stored in remote sensing data products.
 * <p> A single {@code ProductData} instance can have one or more elements of a primitive type. The primitive types
 * are:
 * <ul>
 * <li> {@link ProductData.Byte signed 8-bit integer} </li>
 * <li> {@link ProductData.UByte unsigned 16-bit integer} </li>
 * <li> {@link ProductData.Short signed 32-bit integer} </li>
 * <li> {@link ProductData.UShort unsigned 16-bit integer} </li>
 * <li> {@link ProductData.Int signed 32-bit integer} </li>
 * <li> {@link ProductData.UInt unsigned 32-bit integer} </li>
 * <li> {@link ProductData.Float 32-bit floating point} </li>
 * <li> {@link ProductData.Double 64-bit floating point} </li>
 * <li> {@link ProductData.ASCII a character string (8-bit ASCII encoding)} </li>
 * <li> {@link ProductData.UTC a MJD-2000 encoded data/time value} </li>
 * </ul>
 * <p>The number of elements is an inmutable property of a {@code ProductData} instance.
 * <p>In order to access the data in a {@code ProductData} instance, multiple setters and getters are provided
 * which use generic <i>transfer data types</i> in order to make the data transfer in and out of a
 * {@code ProductData} instance easy for programmers.<br> For scalar (one-element) values the prototypes are
 * <pre>
 *    void setElem<b>Type</b>(<b>Type</b> elem);
 *    <b>Type</b> getElem<b>Type</b>();
 * </pre>
 * For vector (multiple-element) values the prototypes are
 * <pre>
 *    void setElem<b>Type</b>At(int index, <b>Type</b> elem);
 *    <b>Type</b> getElem<b>Type</b>At(int index);
 * </pre>
 * Where the transfer data type <code><b>Type</b></code> is one of {@code int}, {@code long},
 * {@code float}, {@code double} and {@code String}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class ProductData implements Cloneable {

    /**
     * The ID for an undefined data type.
     */
    public static final int TYPE_UNDEFINED = 0;

    /**
     * The ID for a signed 8-bit integer data type.
     */
    public static final int TYPE_INT8 = 10;

    /**
     * The ID for a signed 16-bit integer data type.
     */
    public static final int TYPE_INT16 = 11;

    /**
     * The ID for a signed 32-bit integer data type.
     */
    public static final int TYPE_INT32 = 12;

    /**
     * The ID for a signed 64-bit integer data type.
     */
    public static final int TYPE_INT64 = 13;

    /**
     * The ID for an unsigned 8-bit integer data type.
     */
    public static final int TYPE_UINT8 = 20;

    /**
     * The ID for an unsigned 16-bit integer data type.
     */
    public static final int TYPE_UINT16 = 21;

    /**
     * The ID for an unsigned 32-bit integer data type.
     */
    public static final int TYPE_UINT32 = 22;

    /**
     * The ID for a signed 32-bit floating point data type.
     */
    public static final int TYPE_FLOAT32 = 30;

    /**
     * The ID for a signed 64-bit floating point data type.
     */
    public static final int TYPE_FLOAT64 = 31;

    /**
     * The ID for a ASCII string represented by an array of bytes ({@code byte[]}).
     */
    public static final int TYPE_ASCII = 41;

    /**
     * The ID for a UTC date/time value represented as Modified Julian Day (MJD) (an {@code int[3]}: int[0] = days,
     * int[1] = seconds, int[2] = micro-seconds).
     */
    public static final int TYPE_UTC = 51;

    /**
     * The type ID of this value.
     */
    private final int _type;

    /**
     * The string representation of {@code TYPE_INT8}
     */
    public static final String TYPESTRING_INT8 = "int8";
    /**
     * The string representation of {@code TYPE_INT16}
     */
    public static final String TYPESTRING_INT16 = "int16";
    /**
     * The string representation of {@code TYPE_INT32}
     */
    public static final String TYPESTRING_INT32 = "int32";
    /**
     * The string representation of {@code TYPE_INT64}
     */
    public static final String TYPESTRING_INT64 = "int64";
    /**
     * The string representation of {@code TYPE_UINT8}
     */
    public static final String TYPESTRING_UINT8 = "uint8";
    /**
     * The string representation of {@code TYPE_UINT16}
     */
    public static final String TYPESTRING_UINT16 = "uint16";
    /**
     * The string representation of {@code TYPE_UINT32}
     */
    public static final String TYPESTRING_UINT32 = "uint32";
    /**
     * The string representation of {@code TYPE_FLOAT32}
     */
    public static final String TYPESTRING_FLOAT32 = "float32";
    /**
     * The string representation of {@code TYPE_FLOAT64}
     */
    public static final String TYPESTRING_FLOAT64 = "float64";
    /**
     * The string representation of {@code TYPE_ASCII}
     */
    public static final String TYPESTRING_ASCII = "ascii";

    /**
     * The string representation of {@code TYPE_UTC}
     */
    public static final String TYPESTRING_UTC = "utc";

    /**
     * Constructs a new value of the given type.
     *
     * @param type the value's type
     */
    protected ProductData(int type) {
        _type = type;
    }

    /**
     * Factory method which creates a value instance of the given type and with exactly one element.
     *
     * @param type the value's type
     *
     * @return a new value instance, {@code null} if the given type is not known
     */
    public static ProductData createInstance(int type) {
        return createInstance(type, 1);
    }

    /**
     * Factory method which creates a value instance of the given type and with the specified number of elements.
     *
     * @param type     the value's type
     * @param numElems the number of elements, must be greater than zero if type is not {@link ProductData#TYPE_UTC}
     *
     * @return a new value instance, {@code null} if the given type is not known
     *
     * @throws IllegalArgumentException if one of the arguments is invalid
     */
    public static ProductData createInstance(int type, int numElems) {
        if (numElems < 1 && type != TYPE_UTC) {
            throw new IllegalArgumentException("numElems is less than one");
        }
        switch (type) {
            case TYPE_INT8:
                return new ProductData.Byte(numElems);
            case TYPE_INT16:
                return new ProductData.Short(numElems);
            case TYPE_INT32:
                return new ProductData.Int(numElems);
            case TYPE_INT64:
                return new ProductData.Long(numElems);
            case TYPE_UINT8:
                return new ProductData.UByte(numElems);
            case TYPE_UINT16:
                return new ProductData.UShort(numElems);
            case TYPE_UINT32:
                return new ProductData.UInt(numElems);
            case TYPE_FLOAT32:
                return new ProductData.Float(numElems);
            case TYPE_FLOAT64:
                return new ProductData.Double(numElems);
            case TYPE_ASCII:
                return new ProductData.ASCII(numElems);
            case TYPE_UTC:
                return new ProductData.UTC();
            default:
                return null;
        }
    }

    /**
     * Factory method which creates a value instance of the given type and with the specified number of elements.
     *
     * @param type the value's type
     * @param data if {@code type} is {@code TYPE_ASCII} the {@code String}, otherwise the primitive array type corresponding to {@code type}
     *
     * @return a new value instance, {@code null} if the given type is not known
     *
     * @throws IllegalArgumentException if one of the arguments is invalid
     */
    public static ProductData createInstance(int type, Object data) {
        switch (type) {
            case TYPE_INT8:
                return new ProductData.Byte((byte[]) data);
            case TYPE_INT16:
                return new ProductData.Short((short[]) data);
            case TYPE_INT32:
                return new ProductData.Int((int[]) data);
            case TYPE_INT64:
                return new ProductData.Long((long[]) data);
            case TYPE_UINT8:
                return new ProductData.UByte((byte[]) data);
            case TYPE_UINT16:
                return new ProductData.UShort((short[]) data);
            case TYPE_UINT32:
                return new ProductData.UInt((int[]) data);
            case TYPE_FLOAT32:
                return new ProductData.Float((float[]) data);
            case TYPE_FLOAT64:
                return new ProductData.Double((double[]) data);
            case TYPE_ASCII:
                return new ProductData.ASCII(data.toString());
            case TYPE_UTC:
                return new ProductData.UTC((int[]) data);
            default:
                return null;
        }
    }

    public static ProductData createInstance(byte[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.Byte(elems);
    }

    public static ProductData createUnsignedInstance(byte[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.UByte(elems);
    }

    public static ProductData createInstance(short[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.Short(elems);
    }

    public static ProductData createUnsignedInstance(short[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.UShort(elems);
    }

    public static ProductData createInstance(int[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.Int(elems);
    }

    public static ProductData createUnsignedInstance(int[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.UInt(elems);
    }

    public static ProductData createInstance(long[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.Long(elems);
    }

    public static ProductData createInstance(String strData) {
        Guardian.assertNotNull("strData", strData);
        return new ProductData.ASCII(strData);
    }

    public static ProductData createInstance(float[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.Float(elems);
    }

    public static ProductData createInstance(double[] elems) {
        Guardian.assertNotNull("elems", elems);
        return new ProductData.Double(elems);
    }

    /**
     * Returns this value's type ID.
     */
    public int getType() {
        return _type;
    }

    /**
     * Gets the element size of an element of the given type in bytes.
     *
     * @param type the element type
     *
     * @return the size of a single element in bytes.
     *
     * @throws IllegalArgumentException if the type is not supported.
     */
    public static int getElemSize(int type) {
        switch (type) {
            case TYPE_INT8:
            case TYPE_UINT8:
            case TYPE_ASCII:
                return 1;
            case TYPE_INT16:
            case TYPE_UINT16:
                return 2;
            case TYPE_INT32:
            case TYPE_UINT32:
            case TYPE_FLOAT32:
            case TYPE_UTC:
                return 4;
            case TYPE_INT64:
            case TYPE_FLOAT64:
                return 8;
            default:
                throw new IllegalArgumentException("type is not supported");
        }
    }

    /**
     * Gets the element size of an element of this product data in bytes.
     *
     * @return the size of a single element in bytes
     */
    public int getElemSize() {
        return getElemSize(getType());
    }

    /**
     * Returns a textual representation of the given data type.
     *
     * @return a data type string, {@code null} if the type is unknown
     */
    public static String getTypeString(int type) {
        switch (type) {
            case TYPE_INT8:
                return TYPESTRING_INT8;
            case TYPE_INT16:
                return TYPESTRING_INT16;
            case TYPE_INT32:
                return TYPESTRING_INT32;
            case TYPE_INT64:
                return TYPESTRING_INT64;
            case TYPE_UINT8:
                return TYPESTRING_UINT8;
            case TYPE_UINT16:
                return TYPESTRING_UINT16;
            case TYPE_UINT32:
                return TYPESTRING_UINT32;
            case TYPE_FLOAT32:
                return TYPESTRING_FLOAT32;
            case TYPE_FLOAT64:
                return TYPESTRING_FLOAT64;
            case TYPE_ASCII:
                return TYPESTRING_ASCII;
            case TYPE_UTC:
                return TYPESTRING_UTC;
            default:
                return null;
        }
    }

    /**
     * Returns a integer representation of the given data type string.
     *
     * @return a data type integer, {@code null} if the type is unknown
     */
    public static int getType(String type) {
        switch (type) {
            case TYPESTRING_INT8:
                return TYPE_INT8;
            case TYPESTRING_INT16:
                return TYPE_INT16;
            case TYPESTRING_INT32:
                return TYPE_INT32;
            case TYPESTRING_INT64:
                return TYPE_INT64;
            case TYPESTRING_UINT8:
                return TYPE_UINT8;
            case TYPESTRING_UINT16:
                return TYPE_UINT16;
            case TYPESTRING_UINT32:
                return TYPE_UINT32;
            case TYPESTRING_FLOAT32:
                return TYPE_FLOAT32;
            case TYPESTRING_FLOAT64:
                return TYPE_FLOAT64;
            case TYPESTRING_ASCII:
                return TYPE_ASCII;
            case TYPESTRING_UTC:
                return TYPE_UTC;
            default:
                return TYPE_UNDEFINED;
        }
    }

//    /**
//     * Returns a textual representation of the data type of this product data.
//     * @return the data type string, never <code>null</code>
//     */
//    public String getTypeString() {
//        return getTypeString(getType());
//    }

    /**
     * Returns this value's data type String.
     */
    public String getTypeString() {
        return getTypeString(getType());
    }

    /**
     * Tests whether this value has an integer.
     *
     * @return true, if so
     */
    public boolean isInt() {
        return isIntType(_type);
    }

    /**
     * Tests whether the given value type is a signed or unsigned integer type.
     *
     * @return true, if so
     */
    public static boolean isIntType(int type) {
        return type >= 10 && type < 30;
    }

    /**
     * Tests whether the actual instance is an signed data type.
     *
     * @return true, if so
     */
    public boolean isSigned() {
        return !isUnsigned();
    }

    /**
     * Tests whether the actual instance is an unsigned data type.
     *
     * @return true, if so
     */
    public boolean isUnsigned() {
        return isUIntType(_type);
    }

    /**
     * Tests whether the given value type is an unsigned integer type.
     *
     * @return true, if so
     */
    public static boolean isUIntType(int type) {
        return type >= 20 && type < 30;
    }

    /**
     * Tests whether the given value type is a floating point type.
     *
     * @return true, if so
     */
    public static boolean isFloatingPointType(int type) {
        return type >= 30 && type < 40;
    }

    /**
     * Tests if this value is a scalar.
     *
     * @return true, if so
     */
    public boolean isScalar() {
        return getNumElems() == 1;
    }

    /**
     * Returns the number of data elements this value has.
     */
    public abstract int getNumElems();

    /**
     * Returns the value as an {@code int}.
     * <p>The method assumes that this value is a scalar and therefore simply returns {@code getElemIntAt(0)}.
     *
     * @see #getElemIntAt(int index)
     */
    public int getElemInt() {
        return getElemIntAt(0);
    }

    /**
     * Returns the value as an unsigned {@code int} given as a {@code long}.
     * <p>The method assumes that this value is a scalar and therefore simply returns {@code getElemUIntAt(0)}.
     *
     * @see #getElemUIntAt(int index)
     */
    public long getElemUInt() {
        return getElemUIntAt(0);
    }

    /**
     * Returns the value as a {@code long}.
     * <p>The method assumes that this value is a scalar and therefore simply returns {@code getElemLongAt(0)}.
     *
     * @see #getElemLongAt(int index)
     */
    public long getElemLong() {
        return getElemLongAt(0);
    }

    /**
     * Returns the value as an {@code float}.
     * <p>The method assumes that this value is a scalar and therefore simply returns {@code getElemFloatAt(0)}.
     *
     * @see #getElemFloatAt(int index)
     */
    public float getElemFloat() {
        return getElemFloatAt(0);
    }

    /**
     * Returns the value as an {@code double}.
     * <p>The method assumes that this value is a scalar and therefore simply returns {@code getElemDoubleAt(0)}.
     *
     * @see #getElemDoubleAt(int index)
     */
    public double getElemDouble() {
        return getElemDoubleAt(0);
    }

    /**
     * Returns the value as a {@code String}. The text returned is the comma-separated list of elements contained
     * in this value.
     *
     * @return a text representing this fields value, never {@code null}
     */
    public String getElemString() {
        if (isScalar()) {
            return getElemStringAt(0);
        } else {
            StringBuilder sb = new StringBuilder(4 + 4 * getNumElems());
            for (int i = 0; i < getNumElems(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(getElemStringAt(i));
            }
            return sb.toString();
        }
    }

    /**
     * Returns the value as an {@code boolean}. <p>The method assumes that this value is a scalar and therefore
     * simply returns {@code getElemBooleanAt(0)}.
     *
     * @see #getElemBooleanAt(int index)
     */
    public boolean getElemBoolean() {
        return getElemBooleanAt(0);
    }

    /**
     * Gets the value element with the given index as an {@code int}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract int getElemIntAt(int index);

    /**
     * Gets the value element with the given index as a {@code long}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract long getElemUIntAt(int index);

    /**
     * Gets the value element with the given index as an {@code long}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract long getElemLongAt(int index);

    /**
     * Gets the value element with the given index as a {@code float}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract float getElemFloatAt(int index);

    /**
     * Gets the value element with the given index as a {@code double}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract double getElemDoubleAt(int index);

    /**
     * Gets the value element with the given index as a {@code String}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract String getElemStringAt(int index);

    /**
     * Gets the value element with the given index as a {@code boolean}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public boolean getElemBooleanAt(int index) {
        return (getElemIntAt(index) != 0);
    }

    /**
     * Sets the value as an {@code int}. <p>The method assumes that this value is a scalar and therefore simply
     * calls {@code setElemIntAt(0, value)}.
     *
     * @param value the value to be set
     *
     * @see #setElemIntAt(int index, int value)
     */
    public void setElemInt(int value) {
        setElemIntAt(0, value);
    }

    /**
     * Sets the value as an unsigned {@code int} given as a {@code long}. <p>The method assumes that this
     * value is a scalar and therefore simply calls {@code setElemUIntAt(0, value)}.
     *
     * @param value the value to be set
     *
     * @see #setElemUIntAt(int index, long value)
     */
    public void setElemUInt(long value) {
        setElemUIntAt(0, value);
    }

    /**
     * Sets the value as a {@code long}. <p>The method assumes that this value is a scalar and therefore simply
     * calls {@code setElemLongInt(0, value)}.
     *
     * @param value the value to be set
     *
     * @see #setElemLongAt(int index, long value)
     */
    public void setElemLong(long value) {
        setElemLongAt(0, value);
    }

    /**
     * Sets the value as a {@code float}. <p>The method assumes that this value is a scalar and therefore simply
     * calls {@code setElemFloatAt(0, value)}.
     *
     * @param value the value to be set
     *
     * @see #setElemFloatAt(int index, float value)
     */
    public void setElemFloat(float value) {
        setElemFloatAt(0, value);
    }

    /**
     * Sets the value as a {@code double}. <p>The method assumes that this value is a scalar and therefore simply
     * calls {@code setElemDoubleAt(0)}.
     *
     * @param value the value to be set
     *
     * @see #setElemDoubleAt(int index, double value)
     */
    public void setElemDouble(double value) {
        setElemDoubleAt(0, value);
    }

    /**
     * Sets the value as a {@code String}. <p>The method assumes that this value is a scalar and therefore simply
     * calls {@code setElemStringAt(0)}.
     *
     * @param value the value to be set
     *
     * @see #setElemStringAt
     */
    public void setElemString(String value) {
        setElemStringAt(0, value);
    }

    /**
     * Sets the value as a {@code boolean}. <p>The method assumes that this value is a scalar and therefore simply
     * calls {@code setElemDoubleAt(0)}.
     *
     * @param value the value to be set
     *
     * @see #setElemBooleanAt(int index, boolean value)
     */
    public void setElemBoolean(boolean value) {
        setElemBooleanAt(0, value);
    }

    /**
     * Sets the value at the specified index as an {@code int}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     * @param value the value to be set
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void setElemIntAt(int index, int value);

    /**
     * Sets the value at the specified index as an unsigned {@code int} given as a {@code long}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     * @param value the value to be set
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void setElemUIntAt(int index, long value);

    /**
     * Sets the value at the specified index as a {@code long}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     * @param value the value to be set
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void setElemLongAt(int index, long value);

    /**
     * Sets the value at the specified index as a {@code float}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     * @param value the value to be set
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void setElemFloatAt(int index, float value);

    /**
     * Sets the value at the specified index as a {@code double}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     * @param value the value to be set
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void setElemDoubleAt(int index, double value);

    /**
     * Sets the value at the specified index as a {@code String}.
     * <p><i>THE METHOD IS CURRENTLY NOT IMPLEMENTED.</i>
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     * @param value the value to be set
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public /*abstract*/ void setElemStringAt(int index, String value) {
        throw new IllegalStateException("not implemented");
    }

    /**
     * Sets the value at the specified index as a {@code boolean}.
     *
     * @param index the value index, must be {@code &gt;=0} and {@code &lt;getNumDataElems()}
     * @param value the value to be set
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public void setElemBooleanAt(int index, boolean value) {
        setElemIntAt(index, value ? 1 : 0);
    }

    /**
     * Returns the internal value. The actual type of the returned object should only be one of <ol>
     * <li>{@code byte[]} - for signed/unsigned 8-bit integer fields</li> <li>{@code short[]} - for
     * signed/unsigned 16-bit integer fields</li> <li>{@code int[]} - for signed/unsigned 32-bit integer
     * fields</li> <li>{@code long[]} - for signed/unsigned 64-bit integer
     * fields</li> <li>{@code float[]} - for signed 32-bit floating point fields</li> <li>{@code double[]} -
     * for signed 64-bit floating point fields</li> </ol>
     *
     * @return an array of one of the described types
     */
    public abstract Object getElems();

    /**
     * Sets the internal value. The actual type of the given data object should only be one of <ol>
     * <li>{@code byte[]} - for signed/unsigned 8-bit integer fields</li> <li>{@code short[]} - for
     * signed/unsigned 16-bit integer fields</li> <li>{@code int[]} - for signed/unsigned 32-bit integer
     * fields</li> <li>{@code long[]} - for signed/unsigned 64-bit integer
     * fields</li> <li>{@code float[]} - for signed 32-bit floating point fields</li> <li>{@code double[]} -
     * for signed 64-bit floating point fields</li> <li>{@code String[]} - for all field types</li> </ol>
     *
     * @param data an array of one of the described types
     */
    public abstract void setElems(Object data);

    /**
     * Reads all elements of this {@code ProductData} instance from to the given input stream.
     * <p> The method subsequentially reads the elements at {@code 0} to {@code getNumElems()-1} of this
     * {@code ProductData} instance from the given input stream.<br> Reading starts at the current seek position
     * within the input stream.
     *
     * @param input a seekable data input stream
     *
     * @throws IOException if an I/O error occurs
     */
    public void readFrom(ImageInputStream input) throws IOException {
        readFrom(0, getNumElems(), input);
    }

    /**
     * Reads a single element of this {@code ProductData} instance from to the given output stream.
     * <p> The method reads the element at {@code pos} of this {@code ProductData} instance from the given
     * output stream.<br> Reading starts at the current seek position within the output stream.
     *
     * @param pos   the destination position (zero-based)
     * @param input a seekable data input stream
     *
     * @throws IOException if an I/O error occurs
     */
    public void readFrom(int pos, ImageInputStream input) throws IOException {
        readFrom(pos, 1, input);
    }

    /**
     * Reads elements of this {@code ProductData} instance from the given output stream.
     * <p> The method subsequentially reads the elements at {@code startPos} to {@code startPos+numElems-1} of
     * this {@code ProductData} instance from the given input stream.<br> Reading starts at the current seek
     * position of the input stream.
     *
     * @param startPos the destination start position (zero-based)
     * @param numElems the number of elements to read
     * @param input    a seekable data input stream
     *
     * @throws IOException if an I/O error occurs
     */
    public abstract void readFrom(int startPos, int numElems, ImageInputStream input) throws IOException;

    /**
     * Reads elements into this {@code ProductData} instance from the given input stream.
     * <p> The method subsequentially reads the elements at {@code startPos} to {@code startPos+numElems-1} of
     * this {@code ProductData} instance from the given input stream.<br> Reading starts at {@code inputPos}
     * within the output stream. The method multiplies this position with the value returned by
     * {@code getElemSize()} in order to find the correct stream offset in bytes.
     *
     * @param startPos the destination start position (zero-based)
     * @param numElems the number of elements to read
     * @param input    a seekable data input stream
     * @param inputPos the (zero-based) position in the data output stream where reading starts
     *
     * @throws IOException if an I/O error occurs
     */
    public void readFrom(int startPos, int numElems, ImageInputStream input, long inputPos) throws IOException {
        input.seek(getElemSize() * inputPos);
        readFrom(startPos, numElems, input);
    }


    /**
     * Writes all elements of this {@code ProductData} instance to to the given output stream.
     * <p> The method subsequentially writes the elements at {@code 0} to {@code getNumElems()-1} of this
     * {@code ProductData} instance to the given output stream.<br> Writing starts at the current seek position
     * within the output stream.
     *
     * @param output a seekable data output stream
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeTo(ImageOutputStream output) throws IOException {
        writeTo(0, getNumElems(), output);
    }

    /**
     * Writes a single element of this {@code ProductData} instance to to the given output stream.
     * <p> The method writes the element at {@code pos} of this {@code ProductData} instance to the given
     * output stream.<br> Writing starts at the current seek position within the output stream.
     *
     * @param pos    the source position (zero-based)
     * @param output a seekable data output stream
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeTo(int pos, ImageOutputStream output) throws IOException {
        writeTo(pos, 1, output);
    }

    /**
     * Writes elements of this {@code ProductData} instance to to the given output stream.
     * <p> The method subsequentially writes the elements at {@code startPos} to {@code startPos+numElems-1}
     * of this {@code ProductData} instance to the given output stream.<br> Writing starts at the current seek
     * position within the output stream.
     *
     * @param startPos the source start position (zero-based)
     * @param numElems the number of elements to be written
     * @param output   a seekable data output stream
     *
     * @throws IOException if an I/O error occurs
     */
    public abstract void writeTo(int startPos, int numElems, ImageOutputStream output) throws IOException;

    /**
     * Writes elements of this {@code ProductData} instance to to the given output stream.
     * <p> The method subsequentially writes the elements at {@code startPos} to {@code startPos+numElems-1}
     * of this {@code ProductData} instance to the given output stream.<br> Writing starts at
     * {@code outputPos} within the output stream. The method multiplies this position with the value returned by
     * {@code getElemSize()} in order to find the correct stream offset in bytes.
     *
     * @param startPos  the source start position (zero-based)
     * @param numElems  the number of elements to be written
     * @param output    a seekable data output stream
     * @param outputPos the position in the data output stream where writing starts
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeTo(int startPos, int numElems, ImageOutputStream output, long outputPos) throws IOException {
        output.seek(getElemSize() * outputPos);
        writeTo(startPos, numElems, output);
    }

    /**
     * Returns a string representation of this value which can be used for debugging purposes.
     */
    @Override
    public String toString() {
        return getElemString();
    }

    /**
     * Returns {@link Object#hashCode()}.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns {@link Object#equals(Object)}.
     * Use {@link #equalElems} in order to perform an element-wise comparision.
     */
    @Override
    public final boolean equals(Object other) {
        return super.equals(other);
    }

    /**
     * Tests whether this ProductData is equal to another one.
     * Performs an element-wise comparision if the other object is a {@link ProductData} instance of the same data type.
     * Otherwise the method behaves like {@link Object#equals(Object)}.
     *
     * @param other the other one
     */
    public boolean equalElems(ProductData other) {
        return other == this || ObjectUtils.equalObjects(getElems(), other.getElems());
    }

    /**
     * Retuns a "deep" copy of this product data.
     *
     * @return a copy of this product data
     */
    protected abstract ProductData createDeepClone();

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to {@code dispose()} are undefined.
     */
    public abstract void dispose();

    /**
     * The {@code Byte} class is a {@code ProductData} specialisation for signed 8-bit integer fields.
     * <p> Internally, data is stored in an array of the type {@code byte[]}.
     */
    public static class Byte extends ProductData {

        /**
         * The internal data array holding this value's data elements.
         */
        protected byte[] _array;

        /**
         * Constructs a new signed {@code byte} value.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public Byte(int numElems) {
            this(numElems, false);
        }

        /**
         * Constructs a new signed {@code byte} value.
         *
         * @param array the elements
         */
        public Byte(byte[] array) {
            this(array, false);
        }

        /**
         * Constructs a new signed {@code byte} value.
         *
         * @param numElems the number of elements, must not be less than one
         * @param type must be one of TYPE_UINT8, TYPE_INT8 or TYPE_ASCII
         */
        protected Byte(int numElems, int type) {
            super(type);
            _array = new byte[numElems];
        }

        /**
         * Constructs a new signed {@code byte} value.
         *
         * @param numElems the number of elements, must not be less than one
         * @param unsigned if {@code true} an unsigned value type is constructed
         */
        protected Byte(int numElems, boolean unsigned) {
            this(new byte[numElems], unsigned);
        }


        /**
         * Constructs a new signed {@code byte} value.
         *
         * @param array    the elements
         * @param unsigned if {@code true} an unsigned value type is constructed
         */
        protected Byte(byte[] array, boolean unsigned) {
            this(array, unsigned ? TYPE_UINT8 : TYPE_INT8);
        }

        /**
         * Constructs a new signed {@code byte} value.
         *
         * @param array the elements
         * @param type  must be one of TYPE_UINT8, TYPE_INT8 or TYPE_ASCII
         */
        protected Byte(byte[] array, int type) {
            super(type);
            _array = array;
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final Byte data = new Byte(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Returns the number of data elements this value has.
         */
        @Override
        public int getNumElems() {
            return _array.length;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemLongAt(int)}.
         */
        @Override
        public long getElemLongAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(getElemIntAt(index));
        }

        /**
         * Please refer to {@link ProductData#setElemIntAt(int, int)}.
         */
        @Override
        public void setElemIntAt(int index, int value) {
            _array[index] = (byte) value;
        }

        /**
         * Please refer to {@link ProductData#setElemUIntAt(int, long)}.
         */
        @Override
        public void setElemUIntAt(int index, long value) {
            _array[index] = (byte) value;
        }

        /**
         * Please refer to {@link ProductData#setElemLongAt(int, long)}.
         */
        @Override
        public void setElemLongAt(int index, long value) {
            _array[index] = (byte) value;
        }

        /**
         * Please refer to {@link ProductData#setElemFloatAt(int, float)}.
         */
        @Override
        public void setElemFloatAt(int index, float value) {
            _array[index] = (byte) Math.round(value);
        }

        /**
         * Please refer to {@link ProductData#setElemDoubleAt(int, double)}.
         */
        @Override
        public void setElemDoubleAt(int index, double value) {
            _array[index] = (byte) Math.round(value);
        }

        /**
         * Returns the internal data array holding this value's data elements.
         *
         * @return the internal data array, never {@code null}
         */
        public final byte[] getArray() {
            return _array;
        }

        /**
         * Gets the actual value value(s). The value returned can safely been casted to an array object of the type
         * {@code byte[]}.
         *
         * @return this value's value, always a {@code byte[]}, never {@code null}
         */
        @Override
        public Object getElems() {
            return _array;
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code byte[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    _array[i] = java.lang.Byte.parseByte(strings[i]);
                }
                return;
            }
            if (!(data instanceof byte[]) || ((byte[]) data).length != getNumElems()) {
                throw new IllegalArgumentException("data is not a byte[" + getNumElems() + "]");
            }
            System.arraycopy(data, 0, _array, 0, getNumElems());
        }

        /**
         * Please refer to {@link ProductData#readFrom(int, int, ImageInputStream)}.
         */
        @Override
        public void readFrom(int startPos, int numElems, ImageInputStream source) throws IOException {
            source.readFully(_array, startPos, numElems);
        }

        /**
         * Please refer to {@link ProductData#writeTo(int, int, ImageOutputStream)}.
         */
        @Override
        public void writeTo(int sourceStartPos, int numSourceElems, ImageOutputStream destination) throws IOException {
            destination.write(_array, sourceStartPos, numSourceElems);
        }

        @Override
        public Object clone() {
            Byte c = new Byte(getNumElems(), isUnsigned());
            c.setElems(getElems());
            return c;
        }

        /**
         * Releases all of the resources used by this object instance and all of its owned children. Its primary use is
         * to allow the garbage collector to perform a vanilla job.
         * <p>This method should be called only if it is for sure that this object instance will never be used again.
         * The results of referencing an instance of this class after a call to {@code dispose()} are undefined.
         * <p>Overrides of this method should always call {@code super.dispose();} after disposing this instance.
         */
        @Override
        public void dispose() {
            _array = null;
        }
    }

    /**
     * The {@code UByte} class is a {@code ProductData} specialisation for unsigned 8-bit integer fields.
     * <p> Internally, data is stored in an array of the type {@code byte[]}.
     * <p> In order to preserve the accuracy for the unsigned byte value range the {@code getElemIntAt} method
     * should be used to retrieve the data stored in this value instead of accessing the data array directly.
     * <p>
     * Another method is to mask each of the array elements in order to get the unsigned type in the following way:
     * <pre>
     *     byte[] data = (byte[]) {@link #getElems() value.getElems()};
     *     for (int i = 0; i &lt; data.length; i++) {
     *         int value = data[i] &amp; 0xff;
     *         ...
     *     }
     * </pre>
     */
    public static class UByte extends Byte {

        /**
         * Constructs a new unsigned {@code byte} value.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public UByte(int numElems) {
            super(numElems, true);
        }

        /**
         * Constructs a new unsigned {@code byte} value.
         *
         * @param array the elements
         */
        public UByte(byte[] array) {
            super(array, true);
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final UByte data = new UByte(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return _array[index] & 0xff;
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return getElemIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return getElemIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return getElemIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(getElemIntAt(index));
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code byte[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    final short shortValue = java.lang.Short.parseShort(strings[i]);
                    if (shortValue > java.lang.Byte.MAX_VALUE * 2 + 1
                        || shortValue < 0) {
                        throw new NumberFormatException(
                                "Value out of range. The value:'" + strings[i] + "' is not an unsigned byte value.");
                    }
                    _array[i] = (byte) shortValue;
                }
                return;
            }
            super.setElems(data);
        }
    }

    /**
     * The {@code Short} class is a {@code ProductData} specialisation for signed 16-bit integer fields.
     * <p> Internally, data is stored in an array of the type {@code short[]}.
     */
    public static class Short extends ProductData {

        /**
         * The internal data array holding this value's data elements.
         */
        protected short[] _array;


        /**
         * Constructs a new signed {@code short} value.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public Short(int numElems) {
            this(numElems, false);
        }

        /**
         * Constructs a new signed {@code short} value.
         *
         * @param numElems the number of elements, must not be less than one
         * @param unsigned if {@code true} an unsigned value type is constructed
         */
        protected Short(int numElems, boolean unsigned) {
            this(new short[numElems], unsigned);
        }

        /**
         * Constructs a new signed {@code short} value.
         *
         * @param array the elements
         */
        public Short(short[] array) {
            this(array, false);
        }

        /**
         * Constructs a new signed {@code short} value.
         *
         * @param array    the elements
         * @param unsigned if {@code true} an unsigned value type is constructed
         */
        protected Short(short[] array, boolean unsigned) {
            super(unsigned ? TYPE_UINT16 : TYPE_INT16);
            _array = array;
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final Short data = new Short(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Returns the number of data elements this value has.
         */
        @Override
        public int getNumElems() {
            return _array.length;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemLongAt(int)}.
         */
        @Override
        public long getElemLongAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(getElemIntAt(index));
        }

        /**
         * Please refer to {@link ProductData#setElemIntAt(int, int)}.
         */
        @Override
        public void setElemIntAt(int index, int value) {
            _array[index] = (short) value;
        }

        /**
         * Please refer to {@link ProductData#setElemUIntAt(int, long)}.
         */
        @Override
        public void setElemUIntAt(int index, long value) {
            _array[index] = (short) value;
        }

        /**
         * Please refer to {@link ProductData#setElemLongAt(int, long)}.
         */
        @Override
        public void setElemLongAt(int index, long value) {
            _array[index] = (short) value;
        }

        /**
         * Please refer to {@link ProductData#setElemFloatAt(int, float)}.
         */
        @Override
        public void setElemFloatAt(int index, float value) {
            _array[index] = (short) Math.round(value);
        }

        /**
         * Please refer to {@link ProductData#setElemDoubleAt(int, double)}.
         */
        @Override
        public void setElemDoubleAt(int index, double value) {
            _array[index] = (short) Math.round(value);
        }

        /**
         * Returns the internal data array holding this value's data elements.
         *
         * @return the internal data array, never {@code null}
         */
        public final short[] getArray() {
            return _array;
        }

        /**
         * Gets the actual value value(s). The value returned can safely been casted to an array object of the type
         * {@code short[]}.
         *
         * @return this value's value, always a {@code short[]}, never {@code null}
         */
        @Override
        public Object getElems() {
            return _array;
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code short[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    _array[i] = java.lang.Short.parseShort(strings[i]);
                }
                return;
            }
            if (!(data instanceof short[]) || ((short[]) data).length != getNumElems()) {
                throw new IllegalArgumentException("data is not a short[" + getNumElems() + "]");
            }
            System.arraycopy(data, 0, _array, 0, getNumElems());
        }

        /**
         * Please refer to {@link ProductData#readFrom(int, int, ImageInputStream)}.
         */
        @Override
        public void readFrom(int startPos, int numElems, ImageInputStream source) throws IOException {
            source.readFully(_array, startPos, numElems);
        }

        /**
         * Please refer to {@link ProductData#writeTo(int, int, ImageOutputStream)}.
         */
        @Override
        public void writeTo(int sourceStartPos, int numSourceElems, ImageOutputStream destination) throws IOException {
            destination.writeShorts(_array, sourceStartPos, numSourceElems);
        }

        /**
         * Releases all of the resources used by this object instance and all of its owned children. Its primary use is
         * to allow the garbage collector to perform a vanilla job.
         * <p>This method should be called only if it is for sure that this object instance will never be used again.
         * The results of referencing an instance of this class after a call to {@code dispose()} are undefined.
         * <p>Overrides of this method should always call {@code super.dispose();} after disposing this instance.
         */
        @Override
        public void dispose() {
            _array = null;
        }

    }

    /**
     * The {@code UShort} class is a {@code ProductData} specialisation for unsigned 16-bit integer fields.
     * <p> Internally, data is stored in an array of the type {@code short[]}.
     * <p> In order to preserve the accuracy for the unsigned byte value range the {@code getElemIntAt} method
     * should be used to retrieve the data stored in this value instead of accessing the data array directly.
     * <p>
     * Another method is to mask each of the array elements in order to get the unsigned type in the following way:
     * <pre>
     *     short[] data = (short[]) value.getRaster();
     *     for (int i = 0; i &lt; data.length; i++) {
     *         int value = data[i] &amp; 0xffff;
     *         ...
     *     }
     * </pre>
     */
    public static class UShort extends Short {

        /**
         * Constructs a new unsigned {@code short} value.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public UShort(int numElems) {
            super(numElems, true);
        }

        /**
         * Constructs a new unsigned {@code short} value.
         *
         * @param array the elements
         */
        public UShort(short[] array) {
            super(array, true);
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final UShort data = new UShort(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return _array[index] & 0xffff;
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return getElemIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return getElemIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return getElemIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(getElemIntAt(index));
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code short[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    final int intValue = Integer.parseInt(strings[i]);
                    if (intValue > java.lang.Short.MAX_VALUE * 2 + 1
                        || intValue < 0) {
                        throw new NumberFormatException(
                                "Value out of range. The value:'" + strings[i] + "' is not an unsigned short value.");
                    }
                    _array[i] = (short) intValue;
                }
                return;
            }
            super.setElems(data);
        }
    }

    /**
     * The {@code Int} class is a {@code ProductData} specialisation for signed 32-bit integer fields.
     * <p> Internally, data is stored in an array of the type {@code int[]}.
     */
    public static class Int extends ProductData {

        /**
         * The internal data array holding this value's data elements.
         */
        protected int[] _array;

        /**
         * Constructs a new signed {@code int} value.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public Int(int numElems) {
            this(numElems, false);
        }

        /**
         * Constructs a new {@code int} instance.
         *
         * @param numElems the number of elements, must not be less than one
         * @param unsigned if {@code true} an unsigned value type is constructed
         */
        protected Int(int numElems, boolean unsigned) {
            this(new int[numElems], unsigned);
        }

        /**
         * Constructs a new signed {@code int} value.
         *
         * @param array the elements
         */
        public Int(int[] array) {
            this(array, false);
        }

        /**
         * Constructs a new signed {@code int} value.
         *
         * @param array    the elements
         * @param unsigned if {@code true} an unsigned value type is constructed
         */
        protected Int(int[] array, boolean unsigned) {
            super(unsigned ? TYPE_UINT32 : TYPE_INT32);
            _array = array;
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final Int data = new Int(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }


        /**
         * Returns the number of data elements this value has.
         */
        @Override
        public int getNumElems() {
            return _array.length;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemLongAt(int)}.
         */
        @Override
        public long getElemLongAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(getElemIntAt(index));
        }

        /**
         * Please refer to {@link ProductData#setElemIntAt(int, int)}.
         */
        @Override
        public void setElemIntAt(int index, int value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemUIntAt(int, long)}.
         */
        @Override
        public void setElemUIntAt(int index, long value) {
            _array[index] = (int) value;
        }

        /**
         * Please refer to {@link ProductData#setElemLongAt(int, long)}.
         */
        @Override
        public void setElemLongAt(int index, long value) {
            _array[index] = (int) value;
        }

        /**
         * Please refer to {@link ProductData#setElemFloatAt(int, float)}.
         */
        @Override
        public void setElemFloatAt(int index, float value) {
            _array[index] = Math.round(value);
        }

        /**
         * Please refer to {@link ProductData#setElemDoubleAt(int, double)}.
         */
        @Override
        public void setElemDoubleAt(int index, double value) {
            _array[index] = (int) Math.round(value);
        }

        /**
         * Returns the internal data array holding this value's data elements.
         *
         * @return the internal data array, never {@code null}
         */
        public final int[] getArray() {
            return _array;
        }

        /**
         * Gets the actual value value(s). The value returned can safely been casted to an array object of the type
         * {@code int[]}.
         *
         * @return this value's value, always a {@code int[]}, never {@code null}
         */
        @Override
        public Object getElems() {
            return _array;
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code int[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    _array[i] = Integer.parseInt(strings[i]);
                }
                return;
            }
            if (!(data instanceof int[]) || ((int[]) data).length != getNumElems()) {
                throw new IllegalArgumentException("data is not an int[" + getNumElems() + "]");
            }
            System.arraycopy(data, 0, _array, 0, getNumElems());
        }

        /**
         * Please refer to {@link ProductData#readFrom(int, int, ImageInputStream)}.
         */
        @Override
        public void readFrom(int startPos, int numElems, ImageInputStream source) throws IOException {
            source.readFully(_array, startPos, numElems);
        }

        /**
         * Please refer to {@link ProductData#writeTo(int, int, ImageOutputStream)}.
         */
        @Override
        public void writeTo(int sourceStartPos, int numSourceElems, ImageOutputStream destination) throws IOException {
            destination.writeInts(_array, sourceStartPos, numSourceElems);
        }

        /**
         * Releases all of the resources used by this object instance and all of its owned children. Its primary use is
         * to allow the garbage collector to perform a vanilla job.
         * <p>This method should be called only if it is for sure that this object instance will never be used again.
         * The results of referencing an instance of this class after a call to {@code dispose()} are undefined.
         * <p>Overrides of this method should always call {@code super.dispose();} after disposing this instance.
         */
        @Override
        public void dispose() {
            _array = null;
        }
    }

    /**
     * The {@code Long} class is a {@code ProductData} specialisation for signed 64-bit integer fields.
     * <p> Internally, data is stored in an array of the type {@code long[]}.
     */
    public static class Long extends ProductData {

        /**
         * The internal data array holding this value's data elements.
         */
        protected long[] _array;

        /**
         * Constructs a new {@code long} instance.
         *
         * @param numElems the number of elements, must not be less than one
         */
        protected Long(int numElems) {
            this(new long[numElems]);
        }

        /**
         * Constructs a new {@code Long} instance.
         *
         * @param array the elements
         */
        protected Long(long[] array) {
            super(TYPE_INT64);
            _array = array;
        }

        /**
         * Returns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final Long data = new Long(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }


        /**
         * Returns the number of data elements this value has.
         */
        @Override
        public int getNumElems() {
            return _array.length;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return (int)_array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemLongAt(int)}.
         */
        @Override
        public long getElemLongAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(getElemLongAt(index));
        }

        /**
         * Please refer to {@link ProductData#setElemIntAt(int, int)}.
         */
        @Override
        public void setElemIntAt(int index, int value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemUIntAt(int, long)}.
         */
        @Override
        public void setElemUIntAt(int index, long value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemLongAt(int, long)}.
         */
        @Override
        public void setElemLongAt(int index, long value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemFloatAt(int, float)}.
         */
        @Override
        public void setElemFloatAt(int index, float value) {
            _array[index] = Math.round(value);
        }

        /**
         * Please refer to {@link ProductData#setElemDoubleAt(int, double)}.
         */
        @Override
        public void setElemDoubleAt(int index, double value) {
            _array[index] = Math.round(value);
        }

        /**
         * Returns the internal data array holding this value's data elements.
         *
         * @return the internal data array, never {@code null}
         */
        public final long[] getArray() {
            return _array;
        }

        /**
         * Gets the actual value value(s). The value returned can safely been casted to an array object of the type
         * {@code int[]}.
         *
         * @return this value's value, always a {@code int[]}, never {@code null}
         */
        @Override
        public Object getElems() {
            return _array;
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code int[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    _array[i] = java.lang.Long.parseLong(strings[i]);
                }
                return;
            }
            if (!(data instanceof long[]) || ((long[]) data).length != getNumElems()) {
                throw new IllegalArgumentException("data is not an long[" + getNumElems() + "]");
            }
            System.arraycopy(data, 0, _array, 0, getNumElems());
        }

        /**
         * Please refer to {@link ProductData#readFrom(int, int, ImageInputStream)}.
         */
        @Override
        public void readFrom(int startPos, int numElems, ImageInputStream source) throws IOException {
            source.readFully(_array, startPos, numElems);
        }

        /**
         * Please refer to {@link ProductData#writeTo(int, int, ImageOutputStream)}.
         */
        @Override
        public void writeTo(int sourceStartPos, int numSourceElems, ImageOutputStream destination) throws IOException {
            destination.writeLongs(_array, sourceStartPos, numSourceElems);
        }

        /**
         * Releases all of the resources used by this object instance and all of its owned children. Its primary use is
         * to allow the garbage collector to perform a vanilla job.
         * <p>This method should be called only if it is for sure that this object instance will never be used again.
         * The results of referencing an instance of this class after a call to {@code dispose()} are undefined.
         * <p>Overrides of this method should always call {@code super.dispose();} after disposing this instance.
         */
        @Override
        public void dispose() {
            _array = null;
        }
    }

    /**
     * The {@code UInt} class is a {@code ProductData} specialisation for unsigned 32-bit integer fields.
     * <p> Internally, data is stored in an array of the type {@code int[]}.
     * <p> In order to preserve the accuracy for the unsigned {@code int} value range the
     * {@code getElemUIntAt} method should be used to retrieve the data stored in this value instead of accessing
     * the data array directly.
     * <p>
     * Another method is to mask each of the array elements in order to get the unsigned type in the following way:
     * <pre>
     *     int[] data = (int[]) value.getRaster();
     *     for (int i = 0; i %lt; data.length; i++) {
     *         long value = data[i] &amp; 0xffffffffL;
     *         ...
     *     }
     * </pre>
     */
    public static class UInt extends Int {

        /**
         * Constructs a new unsigned {@code int} value.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public UInt(int numElems) {
            super(numElems, true);
        }

        /**
         * Constructs a new unsigned {@code int} value.
         *
         * @param array the elements
         */
        public UInt(int[] array) {
            super(array, true);
        }

        /**
         * Constructs a new unsigned {@code int} value.
         *
         * @param elems the elements
         */
        protected UInt(long[] elems) {
            this(elems.length);
            for (int i = 0; i < elems.length; i++) {
                _array[i] = (int) elems[i];
            }
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final UInt data = new UInt(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         * <p><i>IMPORTANT NOTE: This method returns the data element unchanged as it is sinternally stored (a 32-bit
         * signed integer) and thus can also return negative values. Use {@code getElemUIntAt} which returns
         * unsigned {@code long} values only.</i>
         */
        @Override
        public int getElemIntAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return _array[index] & 0xffffffffL;
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return getElemUIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return getElemUIntAt(index);
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(getElemUIntAt(index));
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code int[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    final long longValue = java.lang.Long.parseLong(strings[i]);
                    if (longValue > (long) Integer.MAX_VALUE * 2 + 1
                        || longValue < 0) {
                        throw new NumberFormatException(
                                "Value out of range. The value:'" + strings[i] + "' is not an unsigned int value.");
                    }
                    _array[i] = (int) longValue;
                }
                return;
            }
            super.setElems(data);
        }
    }

    /**
     * The {@code ProductData.Float} class is a {@code ProductData} specialisation for 32-bit floating point
     * fields.
     * <p> Internally, data is stored in an array of the type {@code float[]}.
     */
    public static class Float extends ProductData {

        /**
         * The internal data array holding this value's data elements.
         */
        protected float[] _array;

        /**
         * Constructs a new {@code Float} instance for the given array reference.
         *
         * @param array the array reference
         */
        public Float(float[] array) {
            super(TYPE_FLOAT32);
            _array = array;
        }

        /**
         * Constructs a new {@code Float} instance with the given number of elements.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public Float(int numElems) {
            super(TYPE_FLOAT32);
            _array = new float[numElems];
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final Float data = new Float(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Returns the number of data elements this value has.
         */
        @Override
        public int getNumElems() {
            return _array.length;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return Math.round(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return Math.round(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#getElemLongAt(int)}.
         */
        @Override
        public long getElemLongAt(int index) {
            return Math.round(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#setElemIntAt(int, int)}.
         */
        @Override
        public void setElemIntAt(int index, int value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemUIntAt(int, long)}.
         */
        @Override
        public void setElemUIntAt(int index, long value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemLongAt(int, long)}.
         */
        @Override
        public void setElemLongAt(int index, long value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemFloatAt(int, float)}.
         */
        @Override
        public void setElemFloatAt(int index, float value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemDoubleAt(int, double)}.
         */
        @Override
        public void setElemDoubleAt(int index, double value) {
            _array[index] = (float) value;
        }

        /**
         * Returns the internal data array holding this value's data elements.
         *
         * @return the internal data array, never {@code null}
         */
        public final float[] getArray() {
            return _array;
        }

        /**
         * Gets the actual value value(s). The value returned can safely been casted to an array object of the type
         * {@code float[]}.
         *
         * @return this value's value, always a {@code float[]}, never {@code null}
         */
        @Override
        public Object getElems() {
            return _array;
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code float[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    _array[i] = java.lang.Float.parseFloat(strings[i]);
                }
                return;
            }
            if (!(data instanceof float[]) || ((float[]) data).length != getNumElems()) {
                throw new IllegalArgumentException("data is not a float[" + getNumElems() + "]");
            }
            System.arraycopy(data, 0, _array, 0, getNumElems());
        }

        /**
         * Please refer to {@link ProductData#readFrom(int, int, ImageInputStream)}.
         */
        @Override
        public void readFrom(int startPos, int numElems, ImageInputStream source) throws IOException {
            source.readFully(_array, startPos, numElems);
        }

        /**
         * Please refer to {@link ProductData#writeTo(int, int, ImageOutputStream)}.
         */
        @Override
        public void writeTo(int sourceStartPos, int numSourceElems, ImageOutputStream destination) throws IOException {
            destination.writeFloats(_array, sourceStartPos, numSourceElems);
        }

        /**
         * Tests whether this ProductData is equal to another one.
         * Performs an element-wise comparision if the other object is a {@link ProductData} instance of the same data type.
         * Otherwise the method behaves like {@link Object#equals(Object)}.
         *
         * @param other the other one
         */
        @Override
        public boolean equalElems(ProductData other) {
            if (other == this) {
                return true;
            } else if (other instanceof ProductData.Float) {
                return Arrays.equals(_array, ((ProductData.Float) other).getArray());
            }
            return false;
        }

        /**
         * Releases all of the resources used by this object instance and all of its owned children. Its primary use is
         * to allow the garbage collector to perform a vanilla job.
         * <p>This method should be called only if it is for sure that this object instance will never be used again.
         * The results of referencing an instance of this class after a call to {@code dispose()} are undefined.
         * <p>Overrides of this method should always call {@code super.dispose();} after disposing this instance.
         */
        @Override
        public void dispose() {
            _array = null;
        }
    }

    /**
     * The {@code ProductData.Float} class is a {@code ProductData} specialisation for 64-bit floating point
     * fields.
     * <p> Internally, data is stored in an array of the type {@code double[]}.
     */
    public static class Double extends ProductData {

        /**
         * The internal data array holding this value's data elements.
         */
        protected double[] _array;

        /**
         * Constructs a new {@code Double} instance for the given array reference.
         *
         * @param array the array reference
         */
        public Double(double[] array) {
            super(TYPE_FLOAT64);
            _array = array;
        }

        /**
         * Constructs a new {@code Double} instance with the given number of elements.
         *
         * @param numElems the number of elements, must not be less than one
         */
        public Double(int numElems) {
            super(TYPE_FLOAT64);
            _array = new double[numElems];
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final Double data = new Double(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }


        /**
         * Returns the number of data elements this value has.
         */
        @Override
        public int getNumElems() {
            return _array.length;
        }

        /**
         * Please refer to {@link ProductData#getElemIntAt(int)}.
         */
        @Override
        public int getElemIntAt(int index) {
            return (int) Math.round(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#getElemUIntAt(int)}.
         */
        @Override
        public long getElemUIntAt(int index) {
            return Math.round(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#getElemLongAt(int)}.
         */
        @Override
        public long getElemLongAt(int index) {
            return Math.round(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#getElemFloatAt(int)}.
         */
        @Override
        public float getElemFloatAt(int index) {
            return (float) _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemDoubleAt(int)}.
         */
        @Override
        public double getElemDoubleAt(int index) {
            return _array[index];
        }

        /**
         * Please refer to {@link ProductData#getElemStringAt(int)}.
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf(_array[index]);
        }

        /**
         * Please refer to {@link ProductData#setElemIntAt(int, int)}.
         */
        @Override
        public void setElemIntAt(int index, int value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemUIntAt(int, long)}.
         */
        @Override
        public void setElemUIntAt(int index, long value) {
            _array[index] = value;
        }

        @Override
        public void setElemLongAt(int index, long value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemFloatAt(int, float)}.
         */
        @Override
        public void setElemFloatAt(int index, float value) {
            _array[index] = value;
        }

        /**
         * Please refer to {@link ProductData#setElemDoubleAt(int, double)}.
         */
        @Override
        public void setElemDoubleAt(int index, double value) {
            _array[index] = value;
        }

        /**
         * Returns the internal data array holding this value's data elements.
         *
         * @return the internal data array, never {@code null}
         */
        public final double[] getArray() {
            return _array;
        }

        /**
         * Gets the actual value value(s). The value returned can safely been casted to an array object of the type
         * {@code double[]}.
         *
         * @return this value's value, always a {@code double[]}, never {@code null}
         */
        @Override
        public Object getElems() {
            return _array;
        }

        /**
         * Sets the data of this value. The data must be an array of the type {@code float[]} or
         * {@code String[]} and have a length that is equal to the value returned by the
         * {@code getNumDataElems} method.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does not have the required array length.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String[] && ((String[]) data).length == getNumElems()) {
                final String[] strings = (String[]) data;
                for (int i = 0; i < getNumElems(); i++) {
                    _array[i] = java.lang.Double.parseDouble(strings[i]);
                }
                return;
            }
            if (!(data instanceof double[]) || ((double[]) data).length != getNumElems()) {
                throw new IllegalArgumentException("data is not a double[" + getNumElems() + "]");
            }
            System.arraycopy(data, 0, _array, 0, getNumElems());
        }

        /**
         * Please refer to {@link ProductData#readFrom(int, int, ImageInputStream)}.
         */
        @Override
        public void readFrom(int startPos, int numElems, ImageInputStream source) throws IOException {
            source.readFully(_array, startPos, numElems);
        }

        /**
         * Please refer to {@link ProductData#writeTo(int, int, ImageOutputStream)}.
         */
        @Override
        public void writeTo(int sourceStartPos, int numSourceElems, ImageOutputStream destination) throws IOException {
            destination.writeDoubles(_array, sourceStartPos, numSourceElems);
        }

        /**
         * Releases all of the resources used by this object instance and all of its owned children. Its primary use is
         * to allow the garbage collector to perform a vanilla job.
         * <p>This method should be called only if it is for sure that this object instance will never be used again.
         * The results of referencing an instance of this class after a call to {@code dispose()} are undefined.
         * <p>Overrides of this method should always call {@code super.dispose();} after disposing this instance.
         */
        @Override
        public void dispose() {
            _array = null;
        }
    }

    /**
     * The {@code ProductData.ASCII} class is a {@code ProductData.Byte} specialisation representing textual
     * values.
     * <p> Internally, data is stored in an array of the type {@code byte[]}.
     */
    public static class ASCII extends Byte {

        /**
         * Constructs a new {@code ASCII} value.
         *
         * @param length the ASCII string length
         */
        public ASCII(int length) {
            super(length, TYPE_ASCII);
        }

        /**
         * Constructs a new {@code ASCII} value.
         *
         * @param data the ASCII string data
         */
        public ASCII(String data) {
            super(data.getBytes(), TYPE_ASCII);
        }

        /**
         * Returns a textual representation of this value's value. The text returned is a string created from the bytes
         * array elements in this value interpreted as ASCII values.
         *
         * @return a text representing this product data, never {@code null}
         */
        @Override
        public String getElemString() {
            return new String(_array);
        }

        /**
         * Sets the data of this value. The data must be a string, an byte or an char array.
         * Each has to have at least a length of one.
         *
         * @param data the data array
         *
         * @throws IllegalArgumentException if data is {@code null} or it is not an array of the required type or
         *                                  does the array length is less than one.
         */
        @Override
        public void setElems(Object data) {
            Guardian.assertNotNull("data", data);
            if (data instanceof String && ((String) data).length() > 0) {
                _array = ((String) data).getBytes();
            } else if (data instanceof char[] && ((char[]) data).length > 0) {
                _array = String.valueOf((char[]) data).getBytes();
            } else if (data instanceof byte[] && ((byte[]) data).length > 0) {
                _array = (byte[]) data;
            } else {
                throw new IllegalArgumentException("data is not an instance of String, char[] or byte[]" +
                                                   "or the length is less than one");
            }
        }

        /**
         * Returns a textual representation of this product data. The text returned is a string cretaed from the bytes
         * array elements in this value interpreted as ASCII values.
         *
         * @return a text representing this product data, never {@code null}
         */
        @Override
        public String getElemStringAt(int index) {
            return String.valueOf((char) _array[index]);
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final ASCII data = new ASCII(_array.length);
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Returns this value's data type String.
         */
        @Override
        public String getTypeString() {
            return getTypeString(TYPE_ASCII);
        }

    }

    /**
     * The {@code ProductData.UTC} class is a {@code ProductData.UInt} specialisation for UTC date/time
     * values.
     * <p> Internally, data is stored in an {@code int[3]} array which represents a Modified Julian Day 2000
     * ({@link ProductData.UTC#getMJD() MJD}) as a {@link
     * ProductData.UTC#getDaysFraction() days}, a {@link
     * ProductData.UTC#getSecondsFraction() seconds} and a {@link
     * ProductData.UTC#getMicroSecondsFraction() micro-seconds} fraction.
     *
     * @see ProductData.UTC#getMJD()
     * @see ProductData.UTC#getDaysFraction()
     * @see ProductData.UTC#getSecondsFraction()
     * @see ProductData.UTC#getMicroSecondsFraction()
     */
    public static class UTC extends UInt {

        /**
         * The default UTC time zone used by this class.
         */
        public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

        /**
         * The default pattern used to format date strings.
         */
        public static final String DATE_FORMAT_PATTERN = "dd-MMM-yyyy HH:mm:ss";

        private static final double SECONDS_PER_DAY = 86400.0;
        private static final double SECONDS_TO_DAYS = 1.0 / SECONDS_PER_DAY;
        private static final double MICROS_PER_SECOND = 1000000.0;
        private static final double MICROS_TO_SECONDS = 1.0 / MICROS_PER_SECOND;

        /**
         * Constructs a new {@code UTC} value.
         */
        public UTC() {
            super(3);
        }

        /**
         * Constructs a MJD2000 date instance.
         *
         * @param elems an array containg at least the three elements {@code {days, seconds, microSeconds}}
         */
        public UTC(int[] elems) {
            this(elems[0], elems[1], elems[2]);
        }

        /**
         * Constructs a MJD2000 date instance.
         *
         * @param days         the number of days since 2000-01-01 00:00
         * @param seconds      the seconds fraction of the number of days
         * @param microSeconds the microseconds fraction of the number of days
         */
        public UTC(int days, int seconds, int microSeconds) {
            super(3);
            setElemIntAt(0, days);
            setElemIntAt(1, seconds);
            setElemIntAt(2, microSeconds);
        }

        /**
         * Constructs a MJD2000 date instance.
         *
         * @param mjd the Modified Julian Day 2000 (MJD2000) as double value
         *
         * @see #getMJD()
         */
        public UTC(double mjd) {
            super(3);

            double microSeconds = (mjd * SECONDS_PER_DAY * MICROS_PER_SECOND) % MICROS_PER_SECOND;
            double seconds = (mjd * SECONDS_PER_DAY) % SECONDS_PER_DAY;
            final double days = (int) mjd;

            if (microSeconds < 0) {         // handle date prior to year 2000
                microSeconds += MICROS_PER_SECOND;
                seconds -= 1;
            }
            setElemIntAt(0, (int) days);
            setElemIntAt(1, (int) seconds);
            setElemIntAt(2, (int) microSeconds);
        }

        /**
         * Creates a new UTC instance based on the given time and microseconds fraction.
         *
         * @param date   the UTC time
         * @param micros the microseconds fraction
         *
         * @return a new UTC instance
         */
        public static UTC create(final Date date, long micros) {
            final Calendar calendar = createCalendar();
            final long offset = calendar.getTimeInMillis();
            calendar.setTime(date);
            final int millsPerSecond = 1000;
            final int millisPerDay = 24 * 60 * 60 * millsPerSecond;
            calendar.add(Calendar.DATE, -(int) (offset / millisPerDay));
            calendar.add(Calendar.MILLISECOND, -(int) (offset % millisPerDay));
            final long mjd2000Millis = calendar.getTimeInMillis();
            final long days = mjd2000Millis / millisPerDay;
            final long seconds = (mjd2000Millis - days * millisPerDay) / millsPerSecond;
            return new UTC((int) days, (int) seconds, (int) micros);
        }

        /**
         * Gets the MJD 2000 calendar on which this UTC date/time is based. The date is initially set the 1st January
         * 2000, 0:00.
         *
         * @return the MJD 2000 calendar
         *
         * @see #getAsCalendar()
         */
        public static Calendar createCalendar() {
            final Calendar calendar = GregorianCalendar.getInstance(UTC_TIME_ZONE, Locale.ENGLISH);
            calendar.clear();
            calendar.set(2000, 0, 1);
            return calendar;
        }

        /**
         * Creates the MJD 2000 date format used to parse and format. The method returns
         * {@link #createDateFormat(String)} with {@link #DATE_FORMAT_PATTERN}.
         *
         * @return a MJD 2000 date/time format
         */
        public static DateFormat createDateFormat() {
            return createDateFormat(DATE_FORMAT_PATTERN);
        }

        /**
         * Creates a date format using the given pattern. The date format returned, will use the
         * english locale ('en') and a calendar returned by the {@link #createCalendar()} method.
         *
         * @param pattern the data format pattern
         *
         * @return a date format
         *
         * @see java.text.SimpleDateFormat
         */
        public static DateFormat createDateFormat(String pattern) {
            final SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
            dateFormat.setCalendar(createCalendar());
            return dateFormat;
        }

        /**
         * Parses a UTC value given as text in MJD 2000 format.
         * The method returns {@link #parse(String, String)} using {@link #DATE_FORMAT_PATTERN} as pattern.
         *
         * @param text a UTC value given as text
         *
         * @return the UTC value represented by the given text
         *
         * @throws ParseException thrown if the text could not be parsed
         * @see #createCalendar
         * @see #createDateFormat
         */
        public static UTC parse(String text) throws ParseException {
            return parse(text, DATE_FORMAT_PATTERN);
        }

        /**
         * Parses a UTC value given as text. The method also considers an optional
         * mircoseconds fraction at the end of the text string. The mircoseconds fraction
         * is a dot '.' followed by a maximum of 6 digits.
         *
         * @param text    a UTC value given as text
         * @param pattern the date/time pattern
         *
         * @return the UTC value represented by the given text
         *
         * @throws ParseException thrown if the text could not be parsed
         * @see #createCalendar
         * @see #createDateFormat
         */
        public static UTC parse(String text, String pattern) throws ParseException {
            return parse(text, createDateFormat(pattern));
        }

        /**
         * Parses a UTC value given as text. The method also considers an optional
         * mircoseconds fraction at the end of the text string. The mircoseconds fraction
         * is a dot '.' followed by a maximum of 6 digits.
         *
         * @param text    a UTC value given as text
         * @param dateFormat the date/time pattern
         *
         * @return the UTC value represented by the given text
         *
         * @throws ParseException thrown if the text could not be parsed
         * @see #createCalendar
         * @see #createDateFormat
         */
        public static UTC parse(String text, DateFormat dateFormat) throws ParseException {
            Guardian.assertNotNullOrEmpty("text", text);
            Guardian.assertNotNull("dateFormat", dateFormat);

            final int dotPos = text.lastIndexOf(".");
            String noFractionString = text;
            long micros = 0;
            if (dotPos > 0) {
                noFractionString = text.substring(0, dotPos);
                final String fractionString = text.substring(dotPos + 1, text.length());
                if (fractionString.length() > 6) { // max. 6 digits!
                    throw new ParseException("Unparseable date:" + text, dotPos);
                }
                try {
                    micros = Integer.parseInt(fractionString);
                } catch (NumberFormatException e) {
                    throw new ParseException("Unparseable date:" + text, dotPos);
                }
                for (int i = fractionString.length(); i < 6; i++) {
                    micros *= 10;
                }
            }

            final Date date = dateFormat.parse(noFractionString);
            return create(date, micros);
        }

        /**
         * Formats this UTC date/time value as a string using the format {@link #DATE_FORMAT_PATTERN} and the default
         * MJD 2000 calendar.
         *
         * @return a formated UTC date/time string
         *
         * @see #createCalendar
         * @see #createDateFormat
         */
        public String format() {
            final Calendar calendar = createCalendar();
            calendar.add(Calendar.DATE, getDaysFraction());
            calendar.add(Calendar.SECOND, (int) getSecondsFraction());
            final DateFormat dateFormat = createDateFormat();
            final Date time = calendar.getTime();
            final String dateString = dateFormat.format(time);
            final String microsString = String.valueOf(getMicroSecondsFraction());
            StringBuilder sb = new StringBuilder(dateString.toUpperCase());
            sb.append('.');
            for (int i = microsString.length(); i < 6; i++) {
                sb.append('0');
            }
            sb.append(microsString);
            return sb.toString();
        }

        /**
         * Returns this UTC date/time value as a string using the format {@link #DATE_FORMAT_PATTERN}. Simply calls
         * {@link #format()}.
         */
        @Override
        public String getElemString() {
            return format();
        }

        /**
         * Retuns a "deep" copy of this product data.
         *
         * @return a copy of this product data
         */
        @Override
        protected ProductData createDeepClone() {
            final UTC data = new UTC();
            System.arraycopy(_array, 0, data._array, 0, _array.length);
            return data;
        }

        /**
         * Returns this value's data type String.
         */
        @Override
        public String getTypeString() {
            return getTypeString(TYPE_UTC);
        }

        /**
         * Gets the MJD 2000 calendar on which this UTC date/time is based.
         * The date of the calendar is set to this UTC value.
         *
         * @return the MJD 2000 calendar
         *
         * @see #createCalendar()
         * @see #getAsDate()
         */
        public Calendar getAsCalendar() {
            final Calendar calendar = createCalendar();
            calendar.add(Calendar.DATE, getDaysFraction());
            calendar.add(Calendar.SECOND, (int) getSecondsFraction());
            calendar.add(Calendar.MILLISECOND, (int) Math.round(getMicroSecondsFraction() / 1000.0));
            return calendar;
        }

        /**
         * Returns this UTC date/time value as a Date. The method interpretes this UTC value as a MJD 2000 date
         * (Modified Julian Day where the  first day is the 01.01.2000).
         *
         * @see #getAsCalendar()
         */
        public Date getAsDate() {
            return getAsCalendar().getTime();
        }

        /**
         * Returns the Modified Julian Day 2000 (MJD2000) represented by this UTC value as double value.
         *
         * @return this UTC value computed as days
         */
        public double getMJD() {
            return getDaysFraction()
                   + SECONDS_TO_DAYS * (getSecondsFraction()
                                        + MICROS_TO_SECONDS * getMicroSecondsFraction());
        }

        /**
         * Returns the days fraction of the Modified Julian Day (MJD) as a signed integer (the 1st element of the
         * internal data array).
         *
         * @return This UTC's days fraction.
         *
         * @see #getMJD()
         */
        public int getDaysFraction() {
            return this.getElemIntAt(0);
        }

        /**
         * Returns the seconds fraction of the Modified Julian Day (MJD) as a signed integer (the 2nd element of the
         * internal data array).
         *
         * @return This UTC's seconds fraction.
         *
         * @see #getMJD()
         */
        public long getSecondsFraction() {
            return this.getElemIntAt(1);
        }

        /**
         * Returns the micro-seconds fraction of the Modified Julian Day (MJD) as a signed integer (the 3rd element
         * of the internal data array).
         *
         * @return This UTC's micro-seconds fraction.
         *
         * @see #getMJD()
         */
        public long getMicroSecondsFraction() {
            return this.getElemIntAt(2);
        }
    }
}
