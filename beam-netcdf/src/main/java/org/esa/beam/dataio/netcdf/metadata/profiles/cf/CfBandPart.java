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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.ProfileReadContext;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfMultiLevelImage;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DataNode;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ForLoop;
import org.esa.beam.util.StringUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CfBandPart extends ProfilePartIO {

    private static final DataTypeWorkarounds dataTypeWorkarounds = new DataTypeWorkarounds();

    @Override
    public void decode(final ProfileReadContext ctx, final Product p) throws IOException {
        for (final Variable variable : ctx.getRasterDigest().getRasterVariables()) {
            final List<Dimension> dimensions = variable.getDimensions();
            final int rank = dimensions.size();
            final String bandBasename = variable.getName();

            if (rank == 2) {
                addBand(ctx, p, variable, new int[]{}, bandBasename);
            } else {
                final int[] sizeArray = new int[rank - 2];
                System.arraycopy(variable.getShape(), 0, sizeArray, 0, sizeArray.length);
                ForLoop.execute(sizeArray, new ForLoop.Body() {
                    @Override
                    public void execute(int[] indexes, int[] sizes) {
                        final StringBuilder bandNameBuilder = new StringBuilder(bandBasename);
                        for (int i = 0; i < sizes.length; i++) {
                            final Dimension zDim = dimensions.get(i);
                            String zName = zDim.getName();
                            final String skipPrefix = "n_";
                            if (zName.toLowerCase().startsWith(skipPrefix)
                                && zName.length() > skipPrefix.length()) {
                                zName = zName.substring(skipPrefix.length());
                            }
                            if (zDim.getLength() > 1) {
                                bandNameBuilder.append(String.format("_%s%d", zName, (indexes[i] + 1)));
                            }

                        }
                        addBand(ctx, p, variable, indexes, bandNameBuilder.toString());
                    }
                });
            }
        }
        p.setAutoGrouping(getAutoGrouping(ctx));
    }

    private static void addBand(ProfileReadContext ctx, Product p, Variable variable, int[] origin, String bandBasename) {
        final int rasterDataType = getRasterDataType(variable, dataTypeWorkarounds);
        if (variable.getDataType() == DataType.LONG) {
            final Band lowerBand = p.addBand(bandBasename + "_lsb", rasterDataType);
            readCfBandAttributes(variable, lowerBand);
            if (lowerBand.getDescription() != null) {
                lowerBand.setDescription(lowerBand.getDescription() + "(least significant bytes)");
            }
            lowerBand.setSourceImage(new NetcdfMultiLevelImage(lowerBand, variable, origin, ctx));
            addFlagCodingIfApplicable(p, lowerBand, variable, variable.getName() + "_lsb", false);

            final Band upperBand = p.addBand(bandBasename + "_msb", rasterDataType);
            readCfBandAttributes(variable, upperBand);
            if (upperBand.getDescription() != null) {
                upperBand.setDescription(upperBand.getDescription() + "(most significant bytes)");
            }
            upperBand.setSourceImage(new NetcdfMultiLevelImage(upperBand, variable, origin, ctx));
            addFlagCodingIfApplicable(p, upperBand, variable, variable.getName() + "_msb", true);
        } else {
            final Band band = p.addBand(bandBasename, rasterDataType);
            readCfBandAttributes(variable, band);
            band.setSourceImage(new NetcdfMultiLevelImage(band, variable, origin, ctx));
            addFlagCodingIfApplicable(p, band, variable, variable.getName(), false);
        }
    }

    private String getAutoGrouping(ProfileReadContext ctx) {
        ArrayList<String> bandNames = new ArrayList<String>();
        for (final Variable variable : ctx.getRasterDigest().getRasterVariables()) {
            final List<Dimension> dimensions = variable.getDimensions();
            int rank = dimensions.size();
            for (int i = 0; i < rank - 2; i++) {
                Dimension dim = dimensions.get(i);
                if (dim.getLength() > 1) {
                    bandNames.add(variable.getName());
                    break;
                }
            }
        }
        return StringUtils.join(bandNames, ":");
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        // In order to inform the writer that it shall write the geophysical values of log-scaled bands
        // we set this property here.
        ctx.setProperty(Constants.CONVERT_LOGSCALED_BANDS_PROPERTY, true);
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

    public static void writeCfBandAttributes(RasterDataNode rasterDataNode, NVariable variable) throws IOException {
        final String description = rasterDataNode.getDescription();
        if (description != null) {
            variable.addAttribute("long_name", description);
        }
        String unit = rasterDataNode.getUnit();
        if (unit != null) {
            unit = CfCompliantUnitMapper.tryFindUnitString(unit);
            variable.addAttribute("units", unit);
        }
        final boolean unsigned = isUnsigned(rasterDataNode);
        if (unsigned) {
            variable.addAttribute("_Unsigned", String.valueOf(unsigned));
        }

        double noDataValue;
        if (!rasterDataNode.isLog10Scaled()) {
            final double scalingFactor = rasterDataNode.getScalingFactor();
            if (scalingFactor != 1.0) {
                variable.addAttribute(Constants.SCALE_FACTOR_ATT_NAME, scalingFactor);
            }
            final double scalingOffset = rasterDataNode.getScalingOffset();
            if (scalingOffset != 0.0) {
                variable.addAttribute(Constants.ADD_OFFSET_ATT_NAME, scalingOffset);
            }
            noDataValue = rasterDataNode.getNoDataValue();
        } else {
            // scaling information is not written anymore for log10 scaled bands
            // instead we always write geophysical values
            // we do this because log scaling is not supported by NetCDF-CF conventions
            noDataValue = rasterDataNode.getGeophysicalNoDataValue();
        }
        if (rasterDataNode.isNoDataValueUsed()) {
            Number fillValue = DataTypeUtils.convertTo(noDataValue, variable.getDataType());
            variable.addAttribute(Constants.FILL_VALUE_ATT_NAME, fillValue);
        }
        variable.addAttribute("coordinates", "lat lon");
    }

    public static void defineRasterDataNodes(ProfileWriteContext ctx, RasterDataNode[] rasterDataNodes) throws
                                                                                                        IOException {
        final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
        final String dimensions = ncFile.getDimensions();
        for (RasterDataNode rasterDataNode : rasterDataNodes) {
            String variableName = ReaderUtils.getVariableName(rasterDataNode);

            int dataType;
            if (rasterDataNode.isLog10Scaled()) {
                dataType = rasterDataNode.getGeophysicalDataType();
            } else {
                dataType = rasterDataNode.getDataType();
            }
            DataType netcdfDataType = DataTypeUtils.getNetcdfDataType(dataType);
            java.awt.Dimension tileSize = ImageManager.getPreferredTileSize(rasterDataNode.getProduct());
            final NVariable variable = ncFile.addVariable(variableName, netcdfDataType, tileSize, dimensions);
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
        int rasterDataType = DataTypeUtils.getRasterDataType(variable);
        if (rasterDataType == -1) {
            if (variable.getDataType() == DataType.LONG) {
                rasterDataType = variable.isUnsigned() ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
            }
        }
        return rasterDataType;
    }

    private static boolean isUnsigned(DataNode dataNode) {
        return ProductData.isUIntType(dataNode.getDataType());
    }

    private static void addFlagCodingIfApplicable(Product p, Band band, Variable variable, String flagCodingName,
                                                  boolean msb) {
        final Attribute flagMaskAttribute = variable.findAttribute("flag_masks");
        final Attribute flagMeaningsAttribute = variable.findAttribute("flag_meanings");

        if (flagMaskAttribute != null && flagMeaningsAttribute != null) {
            if (!p.getFlagCodingGroup().contains(flagCodingName)) {
                final FlagCoding flagCoding = new FlagCoding(flagCodingName);
                final String[] flagMeanings = flagMeaningsAttribute.getStringValue().split(" ");
                for (int i = 0; i < flagMaskAttribute.getLength(); i++) {
                    if (i < flagMeanings.length) {
                        final String flagMeaning = flagMeanings[i];
                        switch (flagMaskAttribute.getDataType()) {
                            case BYTE:
                                flagCoding.addFlag(flagMeaning,
                                                   DataType.unsignedByteToShort(
                                                           flagMaskAttribute.getNumericValue(i).byteValue()), null);
                                break;
                            case SHORT:
                                flagCoding.addFlag(flagMeaning,
                                                   DataType.unsignedShortToInt(
                                                           flagMaskAttribute.getNumericValue(i).shortValue()), null);
                                break;
                            case INT:
                                flagCoding.addFlag(flagMeaning, flagMaskAttribute.getNumericValue(i).intValue(), null);
                                break;
                            case LONG:
                                final long value = flagMaskAttribute.getNumericValue(i).longValue();
                                if (msb) {
                                    final long flagMask = value >>> 32;
                                    if (flagMask > 0) {
                                        flagCoding.addFlag(flagMeaning, (int) flagMask, null);
                                    }
                                } else {
                                    final long flagMask = value & 0x00000000FFFFFFFFL;
                                    if (flagMask > 0) {
                                        flagCoding.addFlag(flagMeaning, (int) flagMask, null);
                                    }
                                }
                                break;
                        }
                    }
                }
                p.getFlagCodingGroup().add(flagCoding);
            }
            band.setSampleCoding(p.getFlagCodingGroup().get(flagCodingName));
        }
    }
}
