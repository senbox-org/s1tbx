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
import org.esa.snap.core.util.Debug;

/**
 * The <code>FieldInfo</code> class contains the information about the structure of a particular record field.
 * <p> A <code>RecordInfo</code> instance contains a list of <code>FieldInfo</code> instances describing each field of
 * the record.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.snap.dataio.envisat.Field
 * @see org.esa.snap.dataio.envisat.Record
 * @see org.esa.snap.dataio.envisat.RecordInfo
 */
public class FieldInfo extends DataItemInfo {

    /**
     * The number of data elements contained in this field (field-width).
     */
    private final int _numDataElems;

    /**
     * Constructs a new field-info from the supplied parameters.
     *
     * @param fieldName    the field name, must not be null or empty
     * @param dataType     the internal data type. Must be one of the multiple <code>TYPE_</code>XXX constants defined
     *                     in the <code>ProductData</code> interface.
     * @param numDataElems the number of data elements contained in this field (field-width), must be <code>&gt;=
     *                     1</code>.
     * @param physicalUnit the field's unit (optional, can be null)
     * @param description  the field's description (optional, can be null)
     *
     * @see ProductData
     */
    FieldInfo(String fieldName,
              int dataType,
              int numDataElems,
              String physicalUnit,
              String description) {
        super(fieldName, dataType, physicalUnit, description);
        Debug.assertTrue(numDataElems >= 1,
                         "number of data elements must be greater zero"); /*I18N*/
        _numDataElems = numDataElems;
    }

    /**
     * Factory method which creates a new field instance according to the field structure defined by this field-info.
     * <p> The method simply calls <code>Field.create(this)</code> to create the instance.
     *
     * @return a new field instance
     */
    public Field createField() {
        return new Field(this);
    }


    /**
     * Gets the number of data elements contained in this field (the field-width).
     *
     * @return the field-width
     */
    public final int getNumDataElems() {
        return _numDataElems;
    }

    /**
     * Computes the size in bytes required to store the contents of a field described by this field-info and returns
     * it.
     *
     * @return the field size in bytes
     */
    public final int getSizeInBytes() {
        return getNumDataElems() * getDataTypeElemSize(getDataType());
    }

    /**
     * Returns a string representation of this field-info which can be used for debugging purposes.
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("FieldInfo[");
        sb.append("'");
        sb.append(getName());
        sb.append("',");
        sb.append(ProductData.getTypeString(getDataType()));
        sb.append(",");
        sb.append(getNumDataElems());
        sb.append(",'");
        sb.append(getPhysicalUnit());
        sb.append("','");
        sb.append(getDescription());
        sb.append("']");
        return sb.toString();
    }
}
