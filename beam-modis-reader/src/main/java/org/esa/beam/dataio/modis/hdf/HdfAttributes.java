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
package org.esa.beam.dataio.modis.hdf;

import ncsa.hdf.hdflib.HDFConstants;
import org.esa.beam.util.StringUtils;

import java.util.HashMap;
import java.util.List;

public class HdfAttributes {

    private HashMap<String, HdfAttributeContainer> _attributesMap;
    private List<HdfAttributeContainer> _attributeList;

    /**
     * Creates the object with default parameters.
     *
     * @param attributes the list of global attributes
     */
    public HdfAttributes(final List<HdfAttributeContainer> attributes) {
        _attributeList = attributes;
        _attributesMap = new HashMap<String, HdfAttributeContainer>();
        for (HdfAttributeContainer container : attributes) {
            _attributesMap.put(container.getName(), container);
        }
    }

    /**
     * Retrieves the string value of the attribute with the given name. If the attribute does not exist or is not of
     * type String, the method returns null.
     *
     * @param attributeName
     *
     * @return the string value of the attribute, or <code>null</code> if the attribute does not exist or is not of
     *         type {@link String}.
     */
    public String getStringAttributeValue(String attributeName) {
        HdfAttributeContainer cont = _attributesMap.get(attributeName);
        if (cont != null && cont.getHdfType() == HDFConstants.DFNT_CHAR) {
            return cont.getStringValue();
        } else {
            return null;
        }
    }

    /**
     * Retrieves the integer value of the attribute with the given name. The integer array will hold as many elements as
     * the attribute ststes to contain. If the attribute does not exist or is not of type interger, the method returns
     * null.
     *
     * @param attributeName
     *
     * @return integer array with as many elements as the attribute contains, or <code>null</code> if the attribute
     *         does not exist or is not of type interger.
     */
    public int[] getIntAttributeValue(final String attributeName) {
        int[] nRet = null;
        final HdfAttributeContainer cont = _attributesMap.get(attributeName);

        if (cont != null) {
            if (cont.getHdfType() == HDFConstants.DFNT_INT32) {
                nRet = StringUtils.toIntArray(cont.getStringValue(), ",");
            }
        }
        return nRet;
    }

    /**
     * Retrieves the number of global attributes stored
     *
     * @return the number of global attributes
     */
    public int getNumAttributes() {
        return _attributeList.size();
    }

    /**
     * Retrieves the attribute at a given index
     *
     * @param index
     *
     * @return the attribute at the given index
     */
    public HdfAttributeContainer getAttributeAt(final int index) {
        return _attributeList.get(index);
    }

    // #############################################
    // #############   END OF PUBLIC   #############
    // #############################################

}
