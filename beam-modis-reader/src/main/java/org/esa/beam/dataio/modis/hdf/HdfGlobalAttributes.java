/*
 * $Id: HdfGlobalAttributes.java,v 1.1 2006/09/19 07:00:03 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.modis.hdf;

import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HdfGlobalAttributes {

    private HashMap _attributes;
    private List _attributeList;

    /**
     * Creates the object with default parameters.
     */
    public HdfGlobalAttributes() {
        _attributes = new HashMap();
        _attributeList = new ArrayList();
    }

    /**
     * Reads the global attributes from the hdf file passed in.
     *
     * @param sdId the HD interface identifier of the file
     */
    public void read(final int sdId) throws HDFException {
        Debug.trace("reading global attributes ...");
        final int[] fileInfo = new int[2];

        // request number of datasets (fileInfo[0]) and number of global attributes (fileInfo[1])
        if (HDFLibrary.SDfileinfo(sdId, fileInfo)) {
            final int[] sdAttrInfo = new int[2];
            final String[] sdVal = new String[1];

            for (int n = 0; n < fileInfo[1]; n++) {
                sdVal[0] = "";
                if (HDFLibrary.SDattrinfo(sdId, n, sdVal, sdAttrInfo)) {
                    final int attrSize = HDFLibrary.DFKNTsize(sdAttrInfo[0]) * sdAttrInfo[1] + 1;
                    final byte[] buf = new byte[attrSize];

                    if (HDFLibrary.SDreadattr(sdId, n, buf)) {
                        final String attrName = sdVal[0].trim();
                        final HdfAttributeContainer attribute
                                = HdfUtils.decodeByteBufferToAttribute(buf, sdAttrInfo[0], sdAttrInfo[1], attrName);

                        if (attribute != null) {
                            _attributes.put(attrName, attribute);
                            _attributeList.add(attribute);
                            Debug.trace("... " + attrName + ": " + attribute.getStringValue());
                        }
                    }
                }
            }
        }

        Debug.trace("... success");
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
        HdfAttributeContainer cont = (HdfAttributeContainer) _attributes.get(attributeName);
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
        final HdfAttributeContainer cont = (HdfAttributeContainer) _attributes.get(attributeName);

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
        return (HdfAttributeContainer) _attributeList.get(index);
    }

}
