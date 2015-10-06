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
 * The <code>DataItemInfo</code> class represents a named item having a specific data type.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class DataItemInfo extends ItemInfo {

    /**
     * This items's data type. Always one of the multiple <code>ProductData.TYPE_</code>X
     * constants
     */
    private final int _dataType;

    /**
     * This items's unit (optional).
     */
    private final String _physicalUnit;

    /**
     * Constructs a new data item info from the supplied parameters.
     *
     * @param itemName     the item name, must not be null or empty
     * @param dataType     the internal data type. Must be one of the multiple <code>ProductData.TYPE_</code>X
     *                     constants
     * @param physicalUnit the item's physical unit (optional, can be null)
     * @param description  the item's description (optional, can be null)
     *
     * @see ProductData
     */
    protected DataItemInfo(String itemName,
                           int dataType,
                           String physicalUnit,
                           String description) {
        super(itemName, description);
        Debug.assertTrue(dataType != ProductData.TYPE_UNDEFINED,
                         "undefined field data type"); /*I18N*/
        _dataType = dataType;
        _physicalUnit = physicalUnit;
    }

    /**
     * Gets the field's internal data type which is always one of the multiple <code>TYPE_</code>X constants defined in
     * the <code>ProductData</code> interface.
     *
     * @return the data type
     *
     * @see ProductData
     */
    public final int getDataType() {
        return _dataType;
    }

    /**
     * Gets the physical units string, which can be <code>null</code>.
     *
     * @return the physical unit
     */
    public final String getPhysicalUnit() {
        return _physicalUnit;
    }


    /**
     * Utility method which returns the size in bytes required to store a single element of the type given by the
     * supplied data type ID. If the given type is unknown, the method returns zero.
     * <p>IMPORTANT NOTE: This method returns <code>12</code> (= 3 x 4 bytes) for the data type
     * <code>ProductData.TYPE_UTC</code>, since the DDDB interprets an UTC value as a single element, where as the
     * <code>ProductData.UTC</code> stores it as three <code>int</code>s.
     *
     * @param itemDataType the item's data type, must be one of the <code>ProductData.TYPE_</code>X
     *                     constants
     *
     * @return the data element size in bytes
     *
     * @see ProductData
     * @see ProductData.UTC
     */
    public static int getDataTypeElemSize(int itemDataType) {
        switch (itemDataType) {
        case ProductData.TYPE_INT8:
        case ProductData.TYPE_ASCII:
        case ProductData.TYPE_UINT8:
            return 1;
        case ProductData.TYPE_INT16:
        case ProductData.TYPE_UINT16:
            return 2;
        case ProductData.TYPE_INT32:
        case ProductData.TYPE_UINT32:
        case ProductData.TYPE_FLOAT32:
            return 4;
        case ProductData.TYPE_FLOAT64:
            return 8;
        case ProductData.TYPE_UTC:
            return 3 * 4;
        default:
            return 0;
        }
    }

}
