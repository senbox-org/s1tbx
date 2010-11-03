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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfMultiLevelImage;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class CfBandPart extends ProfilePart {

    private static final DataTypeWorkarounds dataTypeWorkarounds = new DataTypeWorkarounds();

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        for (Variable variable : ctx.getRasterDigest().getRasterVariables()) {
            final int rasterDataType = getRasterDataType(variable, dataTypeWorkarounds);
            final Band band = p.addBand(variable.getName(), rasterDataType);
            readCfBandAttributes(variable, band);
            band.setSourceImage(new NetcdfMultiLevelImage(band, variable, ctx));
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        defineRasterDataNodes(ctx, p.getBands());
    }

    public static void readCfBandAttributes(Variable variable, RasterDataNode rasterDataNode) {
        rasterDataNode.setDescription(variable.getDescription());
        rasterDataNode.setUnit(variable.getUnitsString());

        rasterDataNode.setScalingFactor(getScalingFactor(variable));
        rasterDataNode.setScalingOffset(getAddOffset(variable));

        final Number noDataValue = getNoDataValue(variable);
        if (noDataValue != null) {
            rasterDataNode.setNoDataValue(noDataValue.doubleValue());
            rasterDataNode.setNoDataValueUsed(true);
        }
    }

    public static void writeCfBandAttributes(RasterDataNode rasterDataNode, Variable variable) {
        final String description = rasterDataNode.getDescription();
        if (description != null) {
            variable.addAttribute(new Attribute("long_name", description));
        }
        final String unit = rasterDataNode.getUnit();
        if (unit != null) {
            variable.addAttribute(new Attribute("units", unit));
        }
        final boolean unsigned = isUnsigned(rasterDataNode);
        if (unsigned) {
            variable.addAttribute(new Attribute("_Unsigned", String.valueOf(unsigned)));
        }
        final double scalingFactor = rasterDataNode.getScalingFactor();
        if (scalingFactor != 1.0) {
            variable.addAttribute(new Attribute(Constants.SCALE_FACTOR_ATT_NAME, scalingFactor));
        }
        final double scalingOffset = rasterDataNode.getScalingOffset();
        if (scalingOffset != 0.0) {
            variable.addAttribute(new Attribute(Constants.ADD_OFFSET_ATT_NAME, scalingOffset));
        }
        if (rasterDataNode.isNoDataValueUsed()) {
            variable.addAttribute(new Attribute(Constants.FILL_VALUE_ATT_NAME, rasterDataNode.getNoDataValue()));
        }
    }

    public static void defineRasterDataNodes(ProfileWriteContext ctx, RasterDataNode[] rasterDataNodes) {
        final NetcdfFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final List<Dimension> dimensions = ncFile.getRootGroup().getDimensions();
        for (RasterDataNode rasterDataNode : rasterDataNodes) {
            String variableName = ReaderUtils.getVariableName(rasterDataNode);
            final Variable variable = ncFile.addVariable(variableName, DataTypeUtils.getNetcdfDataType(rasterDataNode),
                                                         dimensions);
            writeCfBandAttributes(rasterDataNode, variable);
        }
    }

    private static double getScalingFactor(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.SCALE_FACTOR_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.SLOPE_ATT_NAME);
        }
        return attribute != null ? attribute.getNumericValue().doubleValue() : 1.0;
    }

    private static double getAddOffset(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.ADD_OFFSET_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.INTERCEPT_ATT_NAME);
        }
        return attribute != null ? attribute.getNumericValue().doubleValue() : 0.0;
    }

    private static Number getNoDataValue(Variable variable) {
        Attribute attribute = variable.findAttribute(Constants.FILL_VALUE_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Constants.MISSING_VALUE_ATT_NAME);
        }
        return attribute != null ? attribute.getNumericValue().doubleValue() : null;
    }

    private static int getRasterDataType(Variable variable, DataTypeWorkarounds workarounds) {
        if (workarounds != null && workarounds.hasWorkaround(variable.getName(), variable.getDataType())) {
            return workarounds.getRasterDataType(variable.getName(), variable.getDataType());
        }
        return DataTypeUtils.getRasterDataType(variable);
    }

    private static boolean isUnsigned(DataNode dataNode) {
        return ProductData.isUIntType(dataNode.getDataType());
    }

}
