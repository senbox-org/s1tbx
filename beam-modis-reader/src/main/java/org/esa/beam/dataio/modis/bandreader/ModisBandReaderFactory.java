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
package org.esa.beam.dataio.modis.bandreader;

import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.beam.dataio.modis.hdf.HdfUtils;
import org.esa.beam.dataio.modis.productdb.ModisBandDescription;
import org.esa.beam.framework.datamodel.ProductData;

public class ModisBandReaderFactory {

    /**
     * Retrieves a band reader for the given dataset id and the layer index.
     *
     * @param sdsId the scientific datases identifier
     * @param desc  the modis band description
     *
     * @return one or more applicable band reader
     *
     * @throws ncsa.hdf.hdflib.HDFException
     */
    public static ModisBandReader[] getReaders(final int sdsId, final ModisBandDescription desc) throws HDFException {
        ModisBandReader[] readers = null;

        final String[] dsName = new String[]{""};
        final int[] nInfo = new int[3];
        final int[] nDimSizes = new int[3];

        if (HDFLibrary.SDgetinfo(sdsId, dsName, nDimSizes, nInfo)) {
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

            for (int n = 0; n < readers.length; n++) {
                switch (prodIODataType) {
                case ProductData.TYPE_INT8:
                    readers[n] = new ModisInt8BandReader(sdsId, n, is3d);
                    break;

                case ProductData.TYPE_UINT8:
                    if (scaleMethod == ModisBandReader.SCALE_LINEAR
                        // @todo IMAPP
                        || scaleMethod == ModisBandReader.SCALE_UNKNOWN) {
                        // @todo IMAPP
                        readers[n] = new ModisUint8BandReader(sdsId, n, is3d);
                    } else if (scaleMethod == ModisBandReader.SCALE_EXPONENTIAL) {
                        readers[n] = new ModisUint8ExpBandReader(sdsId, n, is3d);
                    }
                    break;

                case ProductData.TYPE_UINT16:
                    if ((scaleMethod == ModisBandReader.SCALE_LINEAR)
                        || (scaleMethod == ModisBandReader.SCALE_SLOPE_INTERCEPT)) {
                        readers[n] = new ModisUint16BandReader(sdsId, n, is3d);
                    } else if (scaleMethod == ModisBandReader.SCALE_POW_10) {
                        readers[n] = new ModisUint16PowBandReader(sdsId, n, is3d);
                    }
                    break;
                }
                readers[n].setName(bandName);
            }
        }
        return readers;
    }
}
