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
package org.esa.beam.dataio.netcdf.metadata.profiles.beam;

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfMultiLevelImage;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class BeamBandPart extends ProfilePartIO {

    public static final String BANDWIDTH = "bandwidth";
    public static final String WAVELENGTH = "wavelength";
    public static final String VALID_PIXEL_EXPRESSION = "valid_pixel_expression";
    public static final String AUTO_GROUPING = "auto_grouping";
    public static final String QUICKLOOK_BAND_NAME = "quicklook_band_name";
    public static final String SOLAR_FLUX = "solar_flux";
    public static final String SPECTRAL_BAND_INDEX = "spectral_band_index";


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
                final Band band = p.addBand(variable.getFullName(), rasterDataType);
                CfBandPart.readCfBandAttributes(variable, band);
                readBeamBandAttributes(variable, band);
                band.setSourceImage(new NetcdfMultiLevelImage(band, variable, ctx));
            }
        }
        // Work around for a bug in version 1.0.101
        // The solar flux and spectral band index were not preserved.
        // In order to overcome this bug, without having to rewrite the NetCDF files,
        // the following method was introduced at 16.06.2011.
        // The fix is mainly needed for the CoastColour project and only considers MERIS data.
        maybeApplySpectralIndexAndSolarFluxFromMetadata(p);

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
        final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final String dimensions = ncFile.getDimensions();
        for (Band band : p.getBands()) {
            if (isPixelGeoCodingBand(band)) {
                continue;
            }
            int dataType;
            if (band.isLog10Scaled()) {
                dataType = band.getGeophysicalDataType();
                // In order to inform the writer that it shall write the geophysical values of log-scaled bands
                // we set this property here.
                ctx.setProperty(Constants.CONVERT_LOGSCALED_BANDS_PROPERTY, true);
            } else {
                dataType = band.getDataType();
            }

            final DataType ncDataType = DataTypeUtils.getNetcdfDataType(dataType);
            String variableName = ReaderUtils.getVariableName(band);
            java.awt.Dimension tileSize = ImageManager.getPreferredTileSize(p);
            final NVariable variable = ncFile.addVariable(variableName, ncDataType, tileSize, dimensions);
            CfBandPart.writeCfBandAttributes(band, variable);
            writeBeamBandAttributes(band, variable);
        }
        Product.AutoGrouping autoGrouping = p.getAutoGrouping();
        if (autoGrouping != null) {
            ncFile.addGlobalAttribute(AUTO_GROUPING, autoGrouping.toString());
        }
        String quicklookBandName = p.getQuicklookBandName();
        if (quicklookBandName != null && !quicklookBandName.isEmpty()) {
            ncFile.addGlobalAttribute(QUICKLOOK_BAND_NAME, quicklookBandName);
        }
    }

    private boolean isPixelGeoCodingBand(Band band) {
        final GeoCoding geoCoding = band.getGeoCoding();
        if (geoCoding instanceof PixelGeoCoding) {
            PixelGeoCoding pixelGeoCoding = (PixelGeoCoding) geoCoding;
            return pixelGeoCoding.getLatBand() == band || pixelGeoCoding.getLonBand() == band;
        }
        return false;

    }

    private static void readBeamBandAttributes(Variable variable, Band band) {
        // todo se -- units for bandwidth and wavelength

        Attribute attribute = variable.findAttribute(BANDWIDTH);
        if (attribute != null) {
            band.setSpectralBandwidth(attribute.getNumericValue().floatValue());
        }
        attribute = variable.findAttribute(WAVELENGTH);
        if (attribute != null) {
            band.setSpectralWavelength(attribute.getNumericValue().floatValue());
        }
        attribute = variable.findAttribute(SPECTRAL_BAND_INDEX);
        if (attribute != null) {
            band.setSpectralBandIndex(attribute.getNumericValue().intValue());
        }
        attribute = variable.findAttribute(VALID_PIXEL_EXPRESSION);
        if (attribute != null) {
            band.setValidPixelExpression(attribute.getStringValue());
        }
        attribute = variable.findAttribute(SOLAR_FLUX);
        if (attribute != null) {
            band.setSolarFlux(attribute.getNumericValue().floatValue());
        }

        band.setName(ReaderUtils.getRasterName(variable));
    }


    private void maybeApplySpectralIndexAndSolarFluxFromMetadata(Product p) {
        List<Band> bands = Arrays.asList(p.getBands());
        int spectralIndex = 0;
        for (Band band : bands) {
            boolean isSpectralBand = band.getSpectralWavelength() != 0.0f;
            boolean isSpectralBandIndexSet = band.getSpectralBandIndex() != -1;
            if (isSpectralBand && !isSpectralBandIndexSet) {
                band.setSpectralBandIndex(spectralIndex);
                boolean isSolarFluxSet = band.getSolarFlux() != 0.0f;
                if (!isSolarFluxSet) {
                    applySolarFluxFromMetadata(band, spectralIndex);
                }
                spectralIndex++;
            }
        }
    }

    private static void applySolarFluxFromMetadata(Band band, int spectralIndex) {
        MetadataElement metadataRoot = band.getProduct().getMetadataRoot();
        band.setSolarFlux(getSolarFluxFromMetadata(metadataRoot, spectralIndex));
    }

    private static float getSolarFluxFromMetadata(MetadataElement metadataRoot, int bandIndex) {
        if (metadataRoot != null) {
            MetadataElement scalingFactorGads = metadataRoot.getElement("Scaling_Factor_GADS");
            if (scalingFactorGads != null) {
                MetadataAttribute sunSpecFlux = scalingFactorGads.getAttribute("sun_spec_flux");
                ProductData data = sunSpecFlux.getData();
                if (data.getNumElems() > bandIndex) {
                    return data.getElemFloatAt(bandIndex);
                }
            }
        }
        return 0.0f;
    }

    public static void writeBeamBandAttributes(Band band, NVariable variable) throws IOException {
        // todo se -- units for bandwidth and wavelength

        final float spectralBandwidth = band.getSpectralBandwidth();
        if (spectralBandwidth > 0) {
            variable.addAttribute(BANDWIDTH, spectralBandwidth);
        }
        final float spectralWavelength = band.getSpectralWavelength();
        if (spectralWavelength > 0) {
            variable.addAttribute(WAVELENGTH, spectralWavelength);
        }
        final String validPixelExpression = band.getValidPixelExpression();
        if (validPixelExpression != null && validPixelExpression.trim().length() > 0) {
            variable.addAttribute(VALID_PIXEL_EXPRESSION, validPixelExpression);
        }
        final float solarFlux = band.getSolarFlux();
        if (solarFlux > 0) {
            variable.addAttribute(SOLAR_FLUX, solarFlux);
        }
        final float spectralBandIndex = band.getSpectralBandIndex();
        if (spectralBandIndex >= 0) {
            variable.addAttribute(SPECTRAL_BAND_INDEX, spectralBandIndex);
        }
        if (!band.getName().equals(variable.getName())) {
            variable.addAttribute(Constants.ORIG_NAME_ATT_NAME, band.getName());
        }
    }
}
