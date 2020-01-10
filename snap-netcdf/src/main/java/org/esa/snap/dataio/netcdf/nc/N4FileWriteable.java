/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.nc;

import org.esa.snap.dataio.netcdf.NetCDF4Chunking;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.DimKey;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

///**
/// * A wrapper around the netCDF 4 {@link edu.ucar.ral.nujan.netcdf.NhFileWriter}.
// *
// * @author MarcoZ
// */
public class N4FileWriteable extends NFileWriteable {


    N4FileWriteable(String filename) throws IOException {
        netcdfFileWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filename, new NetCDF4Chunking());
    }

    @Override
    public NVariable addScalarVariable(String name, DataType dataType) {
        Variable variable = netcdfFileWriter.addVariable(null, name, dataType, new ArrayList<Dimension>());
        NVariable nVariable = new N4Variable(variable, null, netcdfFileWriter);
        variables.put(name, nVariable);
        return nVariable;
    }

    @Override
    public NVariable addVariable(String name, DataType dataType, boolean unsigned, java.awt.Dimension tileSize, String dimensions, int compressionLevel) {
        String[] dims = dimensions.split(" ");
        ucar.nc2.Dimension[] nhDims = new ucar.nc2.Dimension[dims.length];
        Integer[] chunkLens = new Integer[dims.length];
        for (int i = 0; i < dims.length; i++) {
            nhDims[i] = dimensionsMap.get(dims[i]);
        }
        if (!dims[0].equals("")) {
            DimKey dimKey = new DimKey(nhDims);
            int indexWidth = dimKey.findXDimensionIndex();
            int indexHeight = dimKey.findYDimensionIndex();

            if (tileSize != null) {
                chunkLens[indexHeight] = tileSize.height;
                chunkLens[indexWidth] = tileSize.width;
                // compute tile size so that number of tiles is considerably smaller than Short.MAX_VALUE
                int imageWidth = nhDims[indexWidth].getLength();
                int imageHeight = nhDims[indexHeight].getLength();
                long imageSize = (long) imageHeight * imageWidth;
                for (int scalingFactor = 2; imageSize / (chunkLens[indexHeight] * chunkLens[indexWidth]) > Short.MAX_VALUE / 2; scalingFactor *= 2) {
                    chunkLens[indexHeight] = tileSize.height * scalingFactor;
                    chunkLens[indexWidth] = tileSize.width * scalingFactor;
                }
                // ensure that chunklens <= scene width/height
                chunkLens[indexWidth] = Math.min(chunkLens[indexWidth], imageWidth);
                chunkLens[indexHeight] = Math.min(chunkLens[indexHeight], imageHeight);
                tileSize = new java.awt.Dimension(chunkLens[indexWidth], chunkLens[indexHeight]);
            } else {
                for (int i = 0; i < dims.length; i++) {
                    chunkLens[i] = nhDims[i].getLength();
                }
            }
        } else {
            chunkLens[0] = 1;
        }
        //Object fillValue = null; // TODO
        for (int i = 0; i < chunkLens.length; i++) {
            if (chunkLens[i] == null) {
                chunkLens[i] = 1;
            }
        }
        Variable variable = netcdfFileWriter.addVariable(null, name, dataType.withSignedness((unsigned ? DataType.Signedness.UNSIGNED : DataType.Signedness.SIGNED)), dimensions);
        Attribute chunksizes = new Attribute("_ChunkSizes", Arrays.asList(chunkLens));
        variable.addAttribute(chunksizes);
        NVariable nVariable = new N4Variable(variable, tileSize, netcdfFileWriter);
        variables.put(name, nVariable);
        return nVariable;
    }

    @Override
    public DataType getNetcdfDataType(int dataType) {
        return DataTypeUtils.getNetcdf4DataType(dataType);
    }

    ;
}
