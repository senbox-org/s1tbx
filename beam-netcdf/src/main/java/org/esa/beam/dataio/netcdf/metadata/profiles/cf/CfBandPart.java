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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeWorkarounds;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class CfBandPart extends ProfilePart {

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final Variable[] variables = ctx.getRasterDigest().getRasterVariables();
        for (Variable variable : variables) {
            final DataTypeWorkarounds dataTypeWorkarounds = DataTypeWorkarounds.getInstance();
            final int rasterDataType = getRasterDataType(variable, dataTypeWorkarounds);
            final Band band = p.addBand(variable.getName(), rasterDataType);
            applyAttributes(band, variable);
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        final NetcdfFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final List<Dimension> dimensions = ncFile.getRootGroup().getDimensions();
        for (Band band : bands) {
            final Variable variable = ncFile.addVariable(band.getName(), getNcDataType(band), dimensions);
            addAttributes(variable, band);
        }
    }

    public static void applyAttributes(RasterDataNode rasterDataNode, Variable variable) {
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

    public static void addAttributes(Variable variable, RasterDataNode rasterDataNode) {
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
        return ReaderUtils.getRasterDataType(variable.getDataType(), variable.isUnsigned());
    }

    public static boolean isUnsigned(DataNode dataNode) {
        return ProductData.isUIntType(dataNode.getDataType());
    }

    public static DataType getNcDataType(DataNode dataNode) {
        final int nodeType = dataNode.getDataType();
        if (nodeType == ProductData.TYPE_INT8 || nodeType == ProductData.TYPE_UINT8) {
            return DataType.BYTE;
        } else if (nodeType == ProductData.TYPE_INT16 || nodeType == ProductData.TYPE_UINT16) {
            return DataType.SHORT;
        } else if (nodeType == ProductData.TYPE_INT32 || nodeType == ProductData.TYPE_UINT32) {
            return DataType.INT;
        } else if (nodeType == ProductData.TYPE_FLOAT32) {
            return DataType.FLOAT;
        } else if (nodeType == ProductData.TYPE_FLOAT64) {
            return DataType.DOUBLE;
        } else if (nodeType == ProductData.TYPE_ASCII) {
            return DataType.STRING;
        } else if (nodeType == ProductData.TYPE_UTC) {
            return DataType.STRING;
        } else {
            return null;
        }
    }
}
