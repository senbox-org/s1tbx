/*
 * $Id$
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
package org.esa.beam.dataio.obpg.hdf;

import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFNativeData;
import org.esa.beam.dataio.obpg.hdf.lib.HDF;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;

import java.util.ArrayList;
import java.util.List;

public class HdfFacade {

    public synchronized SDFileInfo getSDFileInfo(int sdStart) throws HDFException {
        final int[] ints = new int[2];
        if (HDF.getInstance().SDfileinfo(sdStart, ints)) {
            return new SDFileInfo(ints[0], ints[1]);
        }
        return null;
    }

    public synchronized SdsInfo getSdsInfo(int sdStart, int index) throws HDFException {
        final int sdsID = HDF.getInstance().SDselect(sdStart, index);
        return getSdsInfo(sdsID);
    }

    public synchronized SdsInfo getSdsInfo(int sdsID) throws HDFException {
        final String[] name = new String[]{""};
        final int[] dimensions = new int[3];
        final int[] dimsDtAttribs = new int[3];
        if (HDF.getInstance().SDgetinfo(sdsID, name, dimensions, dimsDtAttribs)) {
            final int numDimensions = dimsDtAttribs[0];
            final int hdfDataType = dimsDtAttribs[1];
            final int numAttributes = dimsDtAttribs[2];
            return new SdsInfo(sdsID, name[0], hdfDataType, numAttributes, numDimensions, dimensions);
        }
        return null;
    }

    public synchronized List<HdfAttribute> readAttributes(final int sdsId, final int numAttributes) throws
                                                                                       HDFException {
        final List<HdfAttribute> attributes = new ArrayList<HdfAttribute>();

        final int[] sdAttrInfo = new int[2];
        final String[] sdVal = new String[1];
        for (int n = 0; n < numAttributes; n++) {
            sdVal[0] = "";
            if (HDF.getInstance().SDattrinfo(sdsId, n, sdVal, sdAttrInfo)) {
                final int attrSize = HDF.getInstance().DFKNTsize(sdAttrInfo[0]) * sdAttrInfo[1] + 1;
                final byte[] buf = new byte[attrSize];

                if (HDF.getInstance().SDreadattr(sdsId, n, buf)) {
                    final String attrName = sdVal[0].trim();
                    final HdfAttribute attribute
                                = decodeByteBufferToAttribute(buf, sdAttrInfo[0], sdAttrInfo[1], attrName);

                    if (attribute != null) {
                        attributes.add(attribute);
                        Debug.trace("... " + attrName + ": " + attribute.getStringValue());
                    }
                }
            }
        }
        Debug.trace("... success");
        return attributes;
    }

    /**
     * Decodes a byte buffer as read from the HDF library to a useable string.
     *
     * @return a new {@link HdfAttribute } with the decoded string as value.
     */
    public synchronized HdfAttribute decodeByteBufferToAttribute(byte[] buf, int NT, int count, String name)
                throws HDFException {
        final String strVal;
        final int incr = HDF.getInstance().DFKNTsize(NT);

        String strTemp;

        switch (NT) {
        case HDFConstants.DFNT_CHAR:
        case HDFConstants.DFNT_UCHAR8:
            strVal = new String(buf, 0, count).trim();
            break;
        case HDFConstants.DFNT_UINT8:
            strTemp = "";
            for (int n = 0; n < count; n++) {
                Byte bval = buf[n];
                Short shval;
                if (bval.shortValue() < 0) {
                    shval = (short) (bval.intValue() + 256);
                } else {
                    shval = bval.shortValue();
                }
                strTemp += shval.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        case HDFConstants.DFNT_INT8:
            strTemp = "";
            for (int n = 0; n < count; n++) {
                final int pos = n * incr;
                Byte bval = buf[pos];
                strTemp += bval.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        case HDFConstants.DFNT_INT16:
            strTemp = "";
            for (int n = 0; n < count; n++) {
                final int pos = n * incr;
                Short shval = HDFNativeData.byteToShort(buf, pos);
                strTemp += shval.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        case HDFConstants.DFNT_UINT16:
            strTemp = "";
            for (int n = 0; n < count; n++) {
                final int pos = n * incr;
                Short shval = HDFNativeData.byteToShort(buf, pos);
                Integer ival;
                if (shval < 0) {
                    ival = (shval.intValue() + 65536);
                } else {
                    ival = shval.intValue();
                }
                strTemp += ival.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        case HDFConstants.DFNT_INT32:
            strTemp = "";
            for (int n = 0; n < count; n++) {
                final int pos = n * incr;
                Integer ival = HDFNativeData.byteToInt(buf, pos);
                strTemp += ival.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        case HDFConstants.DFNT_UINT32:
            strTemp = "";
            for (int n = 0; n < count; n += incr) {
                final int pos = n * incr;
                Integer ival = HDFNativeData.byteToInt(buf, pos);
                Long lVal;

                if (ival < 0) {
                    lVal = ival.longValue() + 4294967296L;
                } else {
                    lVal = ival.longValue();
                }
                strTemp += lVal.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        case HDFConstants.DFNT_FLOAT32:
            strTemp = "";
            for (int n = 0; n < count; n++) {
                final int pos = n * incr;
                Float fval = HDFNativeData.byteToFloat(buf, pos);
                strTemp += fval.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        case HDFConstants.DFNT_DOUBLE:
            strTemp = "";
            for (int n = 0; n < count; n++) {
                final int pos = n * incr;
                Double dval = HDFNativeData.byteToDouble(buf, pos);
                strTemp += dval.toString();
                strTemp += (",");
            }
            strVal = strTemp.substring(0, strTemp.length() - 1);
            break;
        default:
            return null;
        }
        return new HdfAttribute(name, NT, strVal, count);
    }

    public int openHdfFileReadOnly(final String path) throws HDFException {
        return HDF.getInstance().Hopen(path, HDFConstants.DFACC_RDONLY);
    }

    public int openSdInterfaceReadOnly(final String path) throws HDFException {
        return HDF.getInstance().SDstart(path, HDFConstants.DFACC_RDONLY);
    }

    public boolean closeHdfFile(final int fileId) throws HDFException {
        return HDF.getInstance().Hclose(fileId);
    }


    public boolean isHdfFile(final String path) throws HDFException {
        return HDF.getInstance().Hishdf(path);
    }

    public synchronized ProductData readProductData(SdsInfo sdsInfo, ProductData data) throws HDFException {
        final int sdsID = sdsInfo.getSdsID();
        final int[] start = new int[]{0, 0, 0};
        final int[] stride = new int[]{1, 1, 1};
        final int[] count = new int[]{0, 0, 0};
        final int[] dimensions = sdsInfo.getDimensions();
        for (int i = 0; i < dimensions.length; i++) {
            count[i] = dimensions[i];
        }
        final Object buffer = data.getElems();
        HDF.getInstance().SDreaddata(sdsID, start, stride, count, buffer);
        return data;
    }
}
