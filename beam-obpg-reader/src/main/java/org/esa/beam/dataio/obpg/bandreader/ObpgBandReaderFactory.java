/*
 * $Id: ModisBandReaderFactory.java,v 1.1 2006/09/19 07:00:03 marcop Exp $
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
package org.esa.beam.dataio.obpg.bandreader;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.ObpgUtils;
import org.esa.beam.dataio.obpg.hdf.SdsInfo;
import org.esa.beam.framework.datamodel.ProductData;

public class ObpgBandReaderFactory {

    /**
     * Retrieves a band reader for the given dataset id and the layer index.
     *
     * @param sdsInfo the scientific datases identifier
     * @param prodIODataType the product IO datatype
     * @return one or more applicable band reader
     *
     * @throws ncsa.hdf.hdflib.HDFException
     */
    public static ObpgBandReader[] getReaders(final SdsInfo sdsInfo, final int prodIODataType) throws HDFException {

        ObpgBandReader[] readers = null;

        if (sdsInfo != null) {

            final boolean is3d;
            if (sdsInfo.getNumDimensions() > 2) {
                readers = new ObpgBandReader[sdsInfo.getDimensions()[0]];
                is3d = true;
            } else {
                readers = new ObpgBandReader[1];
                is3d = false;
            }

            final String bandName = sdsInfo.getName();
            int scaleMethod = ObpgBandReader.SCALE_LINEAR;
//            int scaleMethod = ObpgBandReader.decodeScalingMethod(desc.getScalingMethod());

            final int sdsId = sdsInfo.getSdsID();
            for (int i = 0; i < readers.length; i++) {
                if (prodIODataType == ProductData.TYPE_INT8) {
                    readers[i] = new ObpgInt8BandReader(sdsId, i, is3d);
                } else if (prodIODataType == ProductData.TYPE_UINT8) {
                    if ((scaleMethod == ObpgBandReader.SCALE_LINEAR)
                        // @todo IMAPP
                        || (scaleMethod == ObpgBandReader.SCALE_UNKNOWN)) {
                        // @todo IMAPP
                        readers[i] = new ObpgUint8BandReader(sdsId, i, is3d);
                    } else if (scaleMethod == ObpgBandReader.SCALE_EXPONENTIAL) {
                        readers[i] = new ObpgUint8ExpBandReader(sdsId, i, is3d);
                    }
                } else if (prodIODataType == ProductData.TYPE_UINT16) {
                    if ((scaleMethod == ObpgBandReader.SCALE_UNKNOWN) ||
                        (scaleMethod == ObpgBandReader.SCALE_LINEAR) ||
                        (scaleMethod == ObpgBandReader.SCALE_SLOPE_INTERCEPT)) {
                        readers[i] = new ObpgUint16BandReader(sdsId, i, is3d);
                    } else if (scaleMethod == ObpgBandReader.SCALE_POW_10) {
                        readers[i] = new ObpgUint16PowBandReader(sdsId, i, is3d);
                    }
                } else if (prodIODataType == ProductData.TYPE_INT16) {
                    if ((scaleMethod == ObpgBandReader.SCALE_UNKNOWN) ||
                    (scaleMethod == ObpgBandReader.SCALE_LINEAR)) {
                        readers[i] = new ObpgInt16BandReader(sdsId, i, is3d);
                    }
                } else if (prodIODataType == ProductData.TYPE_INT32) {
                    if ((scaleMethod == ObpgBandReader.SCALE_UNKNOWN) ||
                    (scaleMethod == ObpgBandReader.SCALE_LINEAR)) {
                        readers[i] = new ObpgInt32BandReader(sdsId, i, is3d);
                    }
                } else if (prodIODataType == ProductData.TYPE_FLOAT32) {
                    if ((scaleMethod == ObpgBandReader.SCALE_UNKNOWN) ||
                    (scaleMethod == ObpgBandReader.SCALE_LINEAR)) {
                        readers[i] = new ObpgFloat32BandReader(sdsId, i, is3d);
                    }
                }
                readers[i].setName(bandName);
            }
        }
        return readers;
    }
}