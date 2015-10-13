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
package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class BeamTiePointGridPart extends ProfilePartIO {

    public final String OFFSET_X = "offset_x";
    public final String OFFSET_Y = "offset_y";
    public final String SUBSAMPLING_X = "subsampling_x";
    public final String SUBSAMPLING_Y = "subsampling_y";

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        final List<Variable> variables = ctx.getNetcdfFile().getVariables();
        for (Variable variable : variables) {
            final List<Dimension> dimensions = variable.getDimensions();
            if (dimensions.size() != 2) {
                continue;
            }
            final Dimension dimensionY = dimensions.get(0);
            final Dimension dimensionX = dimensions.get(1);
            if (dimensionY.getLength() != p.getSceneRasterHeight()
                    || dimensionX.getLength() != p.getSceneRasterWidth()) {
                //maybe this is a tie point grid
                final String tpName = ReaderUtils.getRasterName(variable);
                if (p.containsBand(tpName)) {
                    continue;
                }
                final Attribute offsetX = variable.findAttributeIgnoreCase(OFFSET_X);
                final Attribute offsetY = variable.findAttributeIgnoreCase(OFFSET_Y);
                final Attribute subSamplingX = variable.findAttributeIgnoreCase(SUBSAMPLING_X);
                final Attribute subSamplingY = variable.findAttributeIgnoreCase(SUBSAMPLING_Y);
                if (offsetX != null && offsetY != null &&
                        subSamplingX != null && subSamplingY != null) {
                    final Array array = variable.read();
                    final float[] data = new float[(int) array.getSize()];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = array.getFloat(i);
                    }
                    final boolean containsAngles = Constants.LON_VAR_NAME.equalsIgnoreCase(tpName) ||
                            Constants.LAT_VAR_NAME.equalsIgnoreCase(tpName) ||
                            Constants.LONGITUDE_VAR_NAME.equalsIgnoreCase(tpName) ||
                            Constants.LATITUDE_VAR_NAME.equalsIgnoreCase(tpName);
                    final TiePointGrid grid = new TiePointGrid(tpName,
                                                               dimensionX.getLength(),
                                                               dimensionY.getLength(),
                                                               offsetX.getNumericValue().floatValue(),
                                                               offsetY.getNumericValue().floatValue(),
                                                               subSamplingX.getNumericValue().floatValue(),
                                                               subSamplingY.getNumericValue().floatValue(),
                                                               data,
                                                               containsAngles);
                    CfBandPart.readCfBandAttributes(variable, grid);
                    p.addTiePointGrid(grid);
                }
            }
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final HashMap<String, String> dimMap = new HashMap<String, String>();
        final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();

        for (TiePointGrid tiePointGrid : p.getTiePointGrids()) {
            final String key = "" + tiePointGrid.getGridHeight() + " " + tiePointGrid.getGridWidth();
            String dimString = dimMap.get(key);
            if (dimString == null) {
                final int size = dimMap.size();
                final String suffix = size > 0 ? "" + (size + 1) : "";
                ncFile.addDimension("tp_y" + suffix, tiePointGrid.getGridHeight());
                ncFile.addDimension("tp_x" + suffix, tiePointGrid.getGridWidth());
                dimString = "tp_y" + suffix + " " + "tp_x" + suffix;
                dimMap.put(key, dimString);
            }
            String variableName = ReaderUtils.getVariableName(tiePointGrid);
            final NVariable variable = ncFile.addVariable(variableName, DataType.FLOAT, null, dimString);
            variable.addAttribute(OFFSET_X, tiePointGrid.getOffsetX());
            variable.addAttribute(OFFSET_Y, tiePointGrid.getOffsetY());
            variable.addAttribute(SUBSAMPLING_X, tiePointGrid.getSubSamplingX());
            variable.addAttribute(SUBSAMPLING_Y, tiePointGrid.getSubSamplingY());
        }
    }

    @Override
    public void encode(ProfileWriteContext ctx, Product p) throws IOException {
        for (TiePointGrid tiePointGrid : p.getTiePointGrids()) {
            final int y = tiePointGrid.getGridHeight();
            final int x = tiePointGrid.getGridWidth();
            final int[] shape = new int[]{y, x};
            final Array values = Array.factory(DataType.FLOAT, shape, tiePointGrid.getDataElems());
            String variableName = ReaderUtils.getVariableName(tiePointGrid);
            ctx.getNetcdfFileWriteable().findVariable(variableName).writeFully(values);
        }
    }
}
