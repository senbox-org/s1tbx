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
package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfMultiLevelImage;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class BeamBandPart extends ProfilePartIO {

    public static final String BANDWIDTH = "bandwidth";
    public static final String WAVELENGTH = "wavelength";
    public static final String VALID_PIXEL_EXPRESSION = "valid_pixel_expression";
    public static final String AUTO_GROUPING = "auto_grouping";
    public static final String QUICKLOOK_BAND_NAME = "quicklook_band_name";


    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final List<Variable> variables = netcdfFile.getVariables();
        for (Variable variable : variables) {
            final List<Dimension> dimensions = variable.getDimensions();
            if (dimensions.size() != 2) {
                continue;
            }
            final int yDimIndex = 0;
            final int xDimIndex = 1;
            if (dimensions.get(yDimIndex).getLength() == p.getSceneRasterHeight()
                    && dimensions.get(xDimIndex).getLength() == p.getSceneRasterWidth()) {
                final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
                final Band band = new Band(variable.getName(), rasterDataType, p.getSceneRasterWidth(),
                                           p.getSceneRasterHeight());
                CfBandPart.readCfBandAttributes(variable, band);
                readBeamBandAttributes(variable, band);
                band.setSourceImage(new NetcdfMultiLevelImage(band, variable, ctx));
                p.addBand(band);
            }
        }
        Attribute autoGroupingAttribute = netcdfFile.findGlobalAttribute(AUTO_GROUPING);
        if (autoGroupingAttribute != null) {
            String autoGrouping = autoGroupingAttribute.getStringValue();
            if (autoGrouping != null) {
                p.setAutoGrouping(autoGrouping);
            }
        }
        Attribute quicklookBandNameAttribute = netcdfFile.findGlobalAttribute(QUICKLOOK_BAND_NAME);
        if (quicklookBandNameAttribute != null) {
            String quicklookBandName = quicklookBandNameAttribute.getStringValue();
            if (quicklookBandName != null) {
                p.setQuicklookBandName(quicklookBandName);
            }
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final NetcdfFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final List<Dimension> dimensions = ncFile.getRootGroup().getDimensions();
        for (Band band : p.getBands()) {
            final DataType ncDataType = DataTypeUtils.getNetcdfDataType(band);
            String variableName = ReaderUtils.getVariableName(band);
            final Variable variable = ncFile.addVariable(variableName, ncDataType, dimensions);
            CfBandPart.writeCfBandAttributes(band, variable);
            writeBeamBandAttributes(band, variable);
        }
        Product.AutoGrouping autoGrouping = p.getAutoGrouping();
        if (autoGrouping != null) {
            ncFile.addAttribute(null, new Attribute(AUTO_GROUPING, autoGrouping.toString()));
        }
        String quicklookBandName = p.getQuicklookBandName();
        if (quicklookBandName != null && !quicklookBandName.isEmpty()) {
            ncFile.addAttribute(null, new Attribute(QUICKLOOK_BAND_NAME, quicklookBandName));
        }
    }

    private static void readBeamBandAttributes(Variable variable, Band band) {
        // todo se -- Log10 Scaling
        // todo se -- units for bandwidth and wavelength

        Attribute attribute = variable.findAttribute(BANDWIDTH);
        if (attribute != null) {
            band.setSpectralBandwidth(attribute.getNumericValue().floatValue());
        }
        attribute = variable.findAttribute(WAVELENGTH);
        if (attribute != null) {
            band.setSpectralWavelength(attribute.getNumericValue().floatValue());
        }
        attribute = variable.findAttribute(VALID_PIXEL_EXPRESSION);
        if (attribute != null) {
            band.setValidPixelExpression(attribute.getStringValue());
        }
        band.setName(ReaderUtils.getRasterName(variable));
    }

    public static void writeBeamBandAttributes(Band band, Variable variable) {
        // todo se -- Log10 Scaling
        // todo se -- units for bandwidth and wavelength

        final float spectralBandwidth = band.getSpectralBandwidth();
        if (spectralBandwidth > 0) {
            variable.addAttribute(new Attribute(BANDWIDTH, spectralBandwidth));
        }
        final float spectralWavelength = band.getSpectralWavelength();
        if (spectralWavelength > 0) {
            variable.addAttribute(new Attribute(WAVELENGTH, spectralWavelength));
        }
        final String validPixelExpression = band.getValidPixelExpression();
        if (validPixelExpression != null && validPixelExpression.trim().length() > 0) {
            variable.addAttribute(new Attribute(VALID_PIXEL_EXPRESSION, validPixelExpression));
        }
        if (!band.getName().equals(variable.getName())) {
            variable.addAttribute(new Attribute(Constants.ORIG_NAME_ATT_NAME, band.getName()));
        }
    }
}
