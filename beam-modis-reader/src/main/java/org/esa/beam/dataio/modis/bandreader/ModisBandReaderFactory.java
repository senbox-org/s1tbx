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

import org.esa.beam.dataio.modis.ModisUtils;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.dataio.modis.productdb.ModisBandDescription;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.IOException;

public class ModisBandReaderFactory {

    public static ModisBandReader[] getReaders(NetCDFVariables netCDFVariables, final ModisBandDescription desc) throws IOException {
        final Variable variable = netCDFVariables.get(desc.getName());
        if (variable == null) {
            return new ModisBandReader[0];
        }

        int scaleMethod = ModisBandReader.decodeScalingMethod(desc.getScalingMethod());
        final int productDataType = DataTypeUtils.getEquivalentProductDataType(variable.getDataType(), variable.isUnsigned(), true);
        boolean is3d = is3d(variable);
        final ModisBandReader[] modisBandReaders = createBandReaderArray(variable, is3d);

        for (int i = 0; i < modisBandReaders.length; i++) {
            if (productDataType == ProductData.TYPE_INT8) {
                modisBandReaders[i] = new ModisInt8BandReader(variable, i, is3d);
            } else if (productDataType == ProductData.TYPE_UINT8) {
                if ((scaleMethod == ModisBandReader.SCALE_LINEAR)
                        // @todo IMAPP
                        || (scaleMethod == ModisBandReader.SCALE_UNKNOWN)) {
                    // @todo IMAPP
                    modisBandReaders[i] = new ModisUint8BandReader(variable, i, is3d);
                } else if (scaleMethod == ModisBandReader.SCALE_EXPONENTIAL) {
                    modisBandReaders[i] = new ModisUint8ExpBandReader(variable, i, is3d);
                }
            } else if (productDataType == ProductData.TYPE_UINT16) {
                if ((scaleMethod == ModisBandReader.SCALE_UNKNOWN) ||
                        (scaleMethod == ModisBandReader.SCALE_LINEAR) ||
                        (scaleMethod == ModisBandReader.SCALE_SLOPE_INTERCEPT)) {
                    modisBandReaders[i] = new ModisUint16BandReader(variable, i, is3d);
                } else if (scaleMethod == ModisBandReader.SCALE_POW_10) {
                    modisBandReaders[i] = new ModisUint16PowBandReader(variable, i, is3d);
                }
            } else if (productDataType == ProductData.TYPE_INT16) {
                if ((scaleMethod == ModisBandReader.SCALE_UNKNOWN) ||
                        (scaleMethod == ModisBandReader.SCALE_LINEAR)) {
                    modisBandReaders[i] = new ModisInt16BandReader(variable, i, is3d);
                }
            } else if (productDataType == ProductData.TYPE_UINT32) {
                if ((scaleMethod == ModisBandReader.SCALE_UNKNOWN) ||
                        (scaleMethod == ModisBandReader.SCALE_LINEAR) ||
                        (scaleMethod == ModisBandReader.SCALE_SLOPE_INTERCEPT)) {
                    modisBandReaders[i] = new ModisUint32BandReader(variable, i, is3d);
                }
            }
            final String bandName = ModisUtils.extractBandName(variable.getFullName());
            modisBandReaders[i].setName(bandName);
        }

        return modisBandReaders;
    }

    private static ModisBandReader[] createBandReaderArray(Variable variable, boolean is3d) {
        final ModisBandReader[] modisBandReaders;
        if (is3d) {
            final Dimension numLayers = variable.getDimension(0);
            modisBandReaders = new ModisBandReader[numLayers.getLength()];
        } else {
            modisBandReaders = new ModisBandReader[1];
        }
        return modisBandReaders;
    }

    private static boolean is3d(Variable variable) {
        final int rank = variable.getRank();
        return rank > 2;
    }
}
