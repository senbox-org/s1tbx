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
package org.esa.beam.dataio.netcdf.metadata.profiles.def;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

import static org.esa.beam.dataio.netcdf.util.ReaderUtils.*;

public class DefaultBandPart extends ProfilePart {

    public static final String BANDWIDTH = "bandwidth";
    public static final String WAVELENGTH = "wavelength";
    public static final String VALID_PIXEL_EXPRESSION = "valid_pixel_expression";

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final List<Variable> variables = ctx.getGlobalVariables();
        for (Variable variable : variables) {
            final List<Dimension> dimensions = variable.getDimensions();
            if (dimensions.size() != 2) {
                continue;
            }
            final int yDimIndex = 0;
            final int xDimIndex = 1;
            if (dimensions.get(yDimIndex).getLength() == p.getSceneRasterHeight()
                && dimensions.get(xDimIndex).getLength() == p.getSceneRasterWidth()) {
                final int rasterDataType = getRasterDataType(variable.getDataType(), variable.isUnsigned());
                final Band band = p.addBand(variable.getName(), rasterDataType);
                applyAttributes(band, variable);
            }
        }
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final Band[] bands = p.getBands();
        final NetcdfFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final List<Dimension> dimensions = ncFile.getRootGroup().getDimensions();
        for (Band band : bands) {
            final DataType ncDataType = CfBandPart.getNcDataType(band);
            final Variable variable = ncFile.addVariable(band.getName(), ncDataType, dimensions);
            addAttributes(variable, band);
        }
    }

    public static void applyAttributes(Band band, Variable variable) {
        CfBandPart.applyAttributes(band, variable);

        // todo se -- Log10 Scaling
        // todo se -- units for bandwidth and wavelength

        Attribute attribute = variable.findAttribute(BANDWIDTH);
        if (attribute != null) {
            band.setSpectralBandwidth((Float) attribute.getNumericValue());
        }
        attribute = variable.findAttribute(WAVELENGTH);
        if (attribute != null) {
            band.setSpectralWavelength((Float) attribute.getNumericValue());
        }
        attribute = variable.findAttribute(VALID_PIXEL_EXPRESSION);
        if (attribute != null) {
            band.setValidPixelExpression(attribute.getStringValue());
        }
    }

    public static void addAttributes(Variable variable, Band band) {
        CfBandPart.addAttributes(variable, band);

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
    }

}
