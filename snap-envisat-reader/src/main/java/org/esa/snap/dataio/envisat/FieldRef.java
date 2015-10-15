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

import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;

/**
 * A class representing a refernce to a field or field element.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class FieldRef {

    /**
     * The data set name
     */
    String _datasetName;
    /**
     * The zero-based index of the field within a dataset record
     */
    int _fieldIndex;
    /**
     * The zero-based index of the referenced field element
     */
    int _elemIndex;

    /**
     * Constructs a new field reference.
     *
     * @param datasetName the name of the dataset, must not be <code>null</code>
     * @param fieldIndex  the zero-based field index, must always be greater than or equal to zero
     */
    FieldRef(String datasetName, int fieldIndex) {
        this(datasetName, fieldIndex, -1);
    }

    /**
     * Constructs a new field element reference.
     *
     * @param datasetName the name of the dataset, must not be <code>null</code> or empty
     * @param fieldIndex  the zero-based field index, must always be greater than or equal to zero
     * @param elemIndex   the zero-based element index, use <code>-1</code> if not defined
     *
     * @throws java.lang.IllegalArgumentException
     *          if one of the given arguments is invalid
     */
    FieldRef(String datasetName, int fieldIndex, int elemIndex) {
        Guardian.assertNotNullOrEmpty("datasetName", datasetName);
        if (fieldIndex < 0 || elemIndex < -1) {
            throw new IllegalArgumentException("invalid FieldRef argument"); /*I18N*/
        }
        _datasetName = datasetName;
        _fieldIndex = fieldIndex;
        _elemIndex = elemIndex;
    }

    /**
     * Parses the given field reference string and returns a corresponding field reference object.
     * <p> The formats accepted are
     * <ul>
     *     <li> <i>DatasetName</i><code>.</code><i>FieldIndex</i> </li>
     *     <li> <i>DatasetName</i><code>.</code><i>FieldIndex</i><code>.</code><i>ElemIndex</i> </li>
     * </ul>
     * with  <i>FieldIndex</i> and <i>ElemIndex</i> beeing one-based (!) integer indexes.
     *
     * @param fieldRefStr the field reference string, must not be <code>null</code>
     *
     * @throws java.lang.NumberFormatException
     *          if a parse error occurs
     */
    public static FieldRef parse(String fieldRefStr) throws NumberFormatException {
        try {
            int dotPos1 = fieldRefStr.lastIndexOf('.');
            String datasetName = fieldRefStr.substring(0, dotPos1).trim();
            String fieldIndexStr = fieldRefStr.substring(dotPos1 + 1).trim();
            int dotPos2 = datasetName.lastIndexOf('.');
            if (dotPos2 >= 0) {
                String fieldIndexTestStr = datasetName.substring(dotPos2 + 1).trim();
                if (StringUtils.isIntegerString(fieldIndexTestStr)) {
                    String elemIndexStr = fieldIndexStr;
                    fieldIndexStr = fieldIndexTestStr;
                    datasetName = datasetName.substring(0, dotPos2).trim();
                    int fieldIndex = Integer.parseInt(fieldIndexStr) - 1;
                    int elemIndex = Integer.parseInt(elemIndexStr) - 1;
                    return new FieldRef(datasetName, fieldIndex, elemIndex);
                }
            }
            int fieldIndex = Integer.parseInt(fieldIndexStr) - 1;
            return new FieldRef(datasetName, fieldIndex);
        } catch (NumberFormatException e) {
        } catch (IndexOutOfBoundsException e) {
        } catch (IllegalArgumentException e) {
        }

        throw new NumberFormatException("invalid field reference string: " + fieldRefStr); /*I18N*/
    }

    /**
     * Gets a corresponding textual representation.
     */
    public String format() {
        StringBuffer sb = new StringBuffer();
        sb.append(getDatasetName());
        sb.append('.');
        sb.append(getFieldIndex() + 1);
        if (getElemIndex() >= 0) {
            sb.append('.');
            sb.append(getElemIndex() + 1);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("FieldRef[");
        sb.append(format());
        sb.append("]");
        return sb.toString();
    }

    /**
     * Gets the name of the dataset providing the record for which this field reference is used.
     *
     * @return the name of the dataset, never <code>null</code>
     */
    public String getDatasetName() {
        return _datasetName;
    }

    /**
     * Gets the zero based element index.
     *
     * @return the zero based element index
     */
    public int getFieldIndex() {
        return _fieldIndex;
    }

    /**
     * Gets the zero based element index.
     *
     * @return the zero based element index, <code>-1</code> if not defined
     */
    public int getElemIndex() {
        return _elemIndex;
    }
}
