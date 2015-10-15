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
package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Guardian;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * The abstract <code>Field</code> class represents an element at the end of the containment hierarchy of ENVISAT
 * products. ENVISAT products are composed of datasets which are composed of one or more records which are composed of
 * one or more fields. Last not least a field is composed of one or more atomic data elements, the actual data arrays.
 * <p> A record performs its <code>readFrom</code> operation by delegating it sequentially to the abstract
 * <code>readFrom</code> operation of all of its fields.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.Record
 * @see org.esa.snap.dataio.envisat.FieldInfo
 * @see ProductData
 */
public class Field {

    /**
     * The info for this field.
     *
     * @supplierCardinality 1
     * @supplierRole -info
     */
    private final FieldInfo _info;
    private final ProductData _data;

    /**
     * Constructs a new field for the given field info.
     *
     * @param info the field info
     *
     * @throws java.lang.IllegalArgumentException
     *          if the field info is null
     */
    protected Field(FieldInfo info) {
        Guardian.assertNotNull("info", info);
        _info = info;
        _data = ProductData.createInstance(info.getDataType(), info.getNumDataElems());
        if (_data == null) {
            throw new IllegalArgumentException("field info has an unknown data type"); /*I18N*/
        }
    }

    /**
     * Returns this field's info.
     */
    public final FieldInfo getInfo() {
        return _info;
    }

    /**
     * Tests whether this field has an integer data type.
     *
     * @return true, if so
     */
    public final ProductData getData() {
        return _data;
    }

    /**
     * Returns this field's name.
     */
    public final String getName() {
        return getInfo().getName();
    }

    /**
     * Gets the field's internal data type which is always one of the multiple <code>TYPE_</code>XXX constants defined
     * in the <code>ProductData</code> interface.
     *
     * @return the data type
     *
     * @see ProductData
     */
    public final int getDataType() {
        return getInfo().getDataType();
    }

    /**
     * Returns the number of data elements contained in this field, sometimes called the field width.
     */
    public final int getNumElems() {
        return getInfo().getNumDataElems();
    }


    /**
     * Tests whether this field has an integer data type.
     *
     * @return true, if so
     */
    public final boolean isIntType() {
        return _data.isInt();
    }

    /**
     * Gets the field element with the given index as an <code>int</code>.
     *
     * @param index the field index, must be <code>&gt;=0</code> and <code>&lt;getNumDataElems()</code>
     *
     * @throws java.lang.IndexOutOfBoundsException
     *          if the index is out of bounds
     */
    public int getElemInt(int index) {
        return getData().getElemIntAt(index);
    }

    /**
     * Gets the field element with the given index as an <code>long</code>.
     *
     * @param index the field index, must be <code>&gt;=0</code> and <code>&lt;getNumDataElems()</code>
     *
     * @throws java.lang.IndexOutOfBoundsException
     *          if the index is out of bounds
     */
    public long getElemLong(int index) {
        return getData().getElemUIntAt(index);
    }

    /**
     * Gets the field element with the given index as an <code>float</code>.
     *
     * @param index the field index, must be <code>&gt;=0</code> and <code>&lt;getNumDataElems()</code>
     *
     * @throws java.lang.IndexOutOfBoundsException
     *          if the index is out of bounds
     */
    public float getElemFloat(int index) {
        return getData().getElemFloatAt(index);
    }

    /**
     * Gets the field element with the given index as a <code>double</code>.
     *
     * @param index the field index, must be <code>&gt;=0</code> and <code>&lt;getNumDataElems()</code>
     *
     * @throws java.lang.IndexOutOfBoundsException
     *          if the index is out of bounds
     */
    public double getElemDouble(int index) {
        return getData().getElemDoubleAt(index);
    }

    /**
     * Returns the internal data array. The actual type of the returned object is guaranteed to be one of <ol>
     * <li><code>byte[]</code> - for signed/unsigned 8-bit integer fields</li> <li><code>short[]</code> - for
     * signed/unsigned 16-bit integer fields</li> <li><code>int[]</code> - for signed/unsigned 32-bit integer
     * fields</li> <li><code>float[]</code> - for signed 32-bit floating point fields</li> <li><code>double[]</code> -
     * for signed 64-bit floating point fields</li> </ol>
     *
     * @return an array of one of the described types
     */
    public Object getElems() {
        return getData().getElems();
    }

    /**
     * Sets the internal data array. The actual type of the given data object should only be one of <ol>
     * <li><code>byte[]</code> - for signed/unsigned 8-bit integer fields</li> <li><code>short[]</code> - for
     * signed/unsigned 16-bit integer fields</li> <li><code>int[]</code> - for signed/unsigned 32-bit integer
     * fields</li> <li><code>float[]</code> - for signed 32-bit floating point fields</li> <li><code>double[]</code> -
     * for signed 64-bit floating point fields</li> </ol>
     *
     * @param data an array of one of the described types
     */
    public void setValue(Object data) {
        getData().setElems(data);
    }

    /**
     * Reads the data elements of this field from the given input stream. This method must be overridden in order to
     * provide an implementation suitable for the type of the underlying data array of this field.
     *
     * @param dataInputStream a seekable data input stream
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void readFrom(ImageInputStream dataInputStream) throws IOException {
        getData().readFrom(dataInputStream);
    }

    /**
     * Returns a textual representation of this field's value.
     *
     * @return a character string representing this field's value
     */
    public String getAsString() {
        return getData().getElemString();
    }

    /**
     * Sets this field's value given a textual representation of it. <p> <b>IMPORTANT NOTE:</b> This method is not yet
     * implemented.
     *
     * @param text a character string representing this field's new value
     */
    public void setValueAsText(String text) {
        // getElems().setFromString(text);
        throw new IllegalStateException("Field.setAsText() yet not implemented");
    }

    /**
     * Returns a string representation of this field which can be used for debugging purposes.
     */
    @Override
    public String toString() {
        int n = getInfo().getNumDataElems();
        StringBuffer sb = new StringBuffer(4 + 4 * n);
        sb.append("Field('");
        sb.append(getName());
        sb.append("')[");
        if (getElems() instanceof ProductData.ASCII) {
            sb.append('"');
            sb.append(getAsString());
            sb.append('"');
        } else {
            sb.append(getAsString());
        }
        sb.append("]");
        return sb.toString();
    }
}
