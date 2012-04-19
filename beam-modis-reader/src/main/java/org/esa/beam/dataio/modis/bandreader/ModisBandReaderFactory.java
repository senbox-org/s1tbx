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
package org.esa.beam.dataio.modis.bandreader;

import org.esa.beam.dataio.modis.hdf.HdfUtils;
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.dataio.modis.productdb.ModisBandDescription;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

public class ModisBandReaderFactory {

    /**
     * Retrieves a band reader for the given dataset id and the layer index.
     *
     * @param sdsId the scientific datases identifier
     * @param desc  the modis band description
     * @return one or more applicable band reader
     * @throws IOException
     */
    public static ModisBandReader[] getReaders(final int sdsId, final ModisBandDescription desc) throws IOException {
        ModisBandReader[] readers = null;

        final String[] dsName = new String[]{""};
        final int[] nInfo = new int[3];
        final int[] nDimSizes = new int[3];

        if (HDF.getWrap().SDgetinfo(sdsId, dsName, nDimSizes, nInfo)) {
            final int prodIODataType = HdfUtils.decodeHdfDataType(nInfo[1]);

            final boolean is3d;
            if (nInfo[0] > 2) {
                readers = new ModisBandReader[nDimSizes[0]];
                is3d = true;
            } else {
                readers = new ModisBandReader[1];
                is3d = false;
            }

            final String bandName = dsName[0];
            int scaleMethod = ModisBandReader.decodeScalingMethod(desc.getScalingMethod());

            for (int i = 0; i < readers.length; i++) {
                if (prodIODataType == ProductData.TYPE_INT8) {
                    readers[i] = new ModisInt8BandReader(sdsId, i, is3d);
                } else if (prodIODataType == ProductData.TYPE_UINT8) {
                    if ((scaleMethod == ModisBandReader.SCALE_LINEAR)
                            // @todo IMAPP
                            || (scaleMethod == ModisBandReader.SCALE_UNKNOWN)) {
                        // @todo IMAPP
                        readers[i] = new ModisUint8BandReader(sdsId, i, is3d);
                    } else if (scaleMethod == ModisBandReader.SCALE_EXPONENTIAL) {
                        readers[i] = new ModisUint8ExpBandReader(sdsId, i, is3d);
                    }
                } else if (prodIODataType == ProductData.TYPE_UINT16) {
                    if ((scaleMethod == ModisBandReader.SCALE_UNKNOWN) ||
                            (scaleMethod == ModisBandReader.SCALE_LINEAR) ||
                            (scaleMethod == ModisBandReader.SCALE_SLOPE_INTERCEPT)) {
                        readers[i] = new ModisUint16BandReader(sdsId, i, is3d);
                    } else if (scaleMethod == ModisBandReader.SCALE_POW_10) {
                        readers[i] = new ModisUint16PowBandReader(sdsId, i, is3d);
                    }
                } else if (prodIODataType == ProductData.TYPE_INT16) {
                    if ((scaleMethod == ModisBandReader.SCALE_UNKNOWN) ||
                            (scaleMethod == ModisBandReader.SCALE_LINEAR)) {
                        readers[i] = new ModisInt16BandReader(sdsId, i, is3d);
                    }
                } else if (prodIODataType == ProductData.TYPE_UINT32) {
                    if ((scaleMethod == ModisBandReader.SCALE_UNKNOWN) ||
                            (scaleMethod == ModisBandReader.SCALE_LINEAR) ||
                            (scaleMethod == ModisBandReader.SCALE_SLOPE_INTERCEPT)) {
                        readers[i] = new ModisUint32BandReader(sdsId, i, is3d);
                    }
                }
                readers[i].setName(bandName);
            }
        }
        return readers;
    }
}
