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

package org.esa.beam.dataio.netcdf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides some NetCDF related utility methods.
 */
public class NetcdfReaderUtils {

    public static NcRasterDigest createRasterDigest(final Group group) {
        Map<NcRasterDim, List<Variable>> variableListMap = getVariableListMap(group);
        if (variableListMap.isEmpty()) {
            return null;
        }
        final NcRasterDim rasterDim = getBestRasterDim(variableListMap);
        final Variable[] rasterVariables = getRasterVariables(variableListMap, rasterDim);
        return new NcRasterDigest(rasterDim, rasterVariables);
    }

    public static Band createBand(final Variable variable, final NcAttributeMap attMap, DataTypeWorkarounds typeWorkarounds, final int rasterWidth, final int rasterHeight) {
        int rasterDataType = getRasterDataType(variable, typeWorkarounds);
        final Band band = new Band(NcVariableMap.getAbsoluteName(variable),
                                   rasterDataType,
                                   rasterWidth,
                                   rasterHeight);
        band.setDescription(variable.getDescription());
        band.setUnit(variable.getUnitsString());
        band.setScalingFactor(getScalingFactor(attMap));
        band.setScalingOffset(getAddOffset(attMap));
        
        final Number noDataValue = getNoDataValue(attMap);
        if (noDataValue != null) {
            band.setNoDataValue(noDataValue.doubleValue());
            band.setNoDataValueUsed(true);
        }
        return band;
    }
    
    public static double getScalingFactor(NcAttributeMap attMap) {
        Number numValue = attMap.getNumericValue(NetcdfConstants.SCALE_FACTOR_ATT_NAME);
        if (numValue == null) {
            numValue = attMap.getNumericValue(NetcdfConstants.SLOPE_ATT_NAME);
        }
        return numValue != null ? numValue.doubleValue() : 1.0;
    }
    
    public static double getAddOffset(NcAttributeMap attMap) {
        Number numValue = attMap.getNumericValue(NetcdfConstants.ADD_OFFSET_ATT_NAME);
        if (numValue == null) {
            numValue = attMap.getNumericValue(NetcdfConstants.INTERCEPT_ATT_NAME);
        }
        return numValue != null ? numValue.doubleValue() : 0.0;
    }
    
    public static Number getNoDataValue(NcAttributeMap attMap) {
        Number noDataValue = attMap.getNumericValue(NetcdfConstants.FILL_VALUE_ATT_NAME);
        if (noDataValue == null) {
            noDataValue = attMap.getNumericValue(NetcdfConstants.MISSING_VALUE_ATT_NAME);
        }
        return noDataValue;
    }
    
    public static IndexCoding createIndexCoding(final String codingName, final NcAttributeMap attMap) {
        Attribute flagValues = attMap.get("flag_values");
        String flagMeanings = attMap.getStringValue("flag_meanings");
        IndexCoding coding = null;
        if (flagValues != null && flagMeanings != null) {
            String[] meanings = flagMeanings.split(" ");
            coding = new IndexCoding(codingName);
            int numElems = flagValues.getLength();
            numElems = Math.min(numElems, meanings.length);
            for (int i = 0; i < numElems; i++) {
                Number number = flagValues.getNumericValue(i);
                if (number != null) {
                    coding.addSample(meanings[i], number.intValue(), "");
                }
            }
            if (coding.getNumAttributes() <= 1) {
                coding = null;
            }
        }
        return coding;
    }

    public static MapInfoX createMapInfoX(final Variable lonVar,
                                          final Variable latVar,
                                          int sceneRasterWidth,
                                          final int sceneRasterHeight) throws IOException {
        double pixelX;
        double pixelY;
        double easting;
        double northing;
        double pixelSizeX;
        double pixelSizeY;

        final NcAttributeMap lonAttrMap = NcAttributeMap.create(lonVar);
        final Number lonValidMin = lonAttrMap.getNumericValue(NetcdfConstants.VALID_MIN_ATT_NAME);
        final Number lonStep = lonAttrMap.getNumericValue(NetcdfConstants.STEP_ATT_NAME);

        final NcAttributeMap latAttrMap = NcAttributeMap.create(latVar);
        final Number latValidMin = latAttrMap.getNumericValue(NetcdfConstants.VALID_MIN_ATT_NAME);
        final Number latStep = latAttrMap.getNumericValue(NetcdfConstants.STEP_ATT_NAME);

        boolean yFlipped;
        if (lonValidMin != null && lonStep != null && latValidMin != null && latStep != null) {
            // COARDS convention uses 'valid_min' and 'step' attributes
            pixelX = 0.5;
            pixelY = (sceneRasterHeight - 1.0) + 0.5;
            easting = lonValidMin.doubleValue();
            northing = latValidMin.doubleValue();
            pixelSizeX = lonStep.doubleValue();
            pixelSizeY = latStep.doubleValue();
            // must flip
            yFlipped = true; // todo - check
        } else {
            // CF convention

            final Array lonData = lonVar.read();
            final Array latData = latVar.read();

            final Index i0 = lonData.getIndex().set(0);
            final Index i1 = lonData.getIndex().set(1);
            pixelSizeX = lonData.getDouble(i1) - lonData.getDouble(i0);
            easting = lonData.getDouble(i0);

            int latSize = (int) latVar.getSize();
            final Index j0 = latData.getIndex().set(0);
            final Index j1 = latData.getIndex().set(1);
            pixelSizeY = latData.getDouble(j1) - latData.getDouble(j0);

            pixelX = 0.5f;
            pixelY = 0.5f;

            // this should be the 'normal' case
            if (pixelSizeY < 0) {
                pixelSizeY = -pixelSizeY;
                yFlipped = false;
                northing = latData.getDouble(latData.getIndex().set(0));
            } else {
                yFlipped = true;
                northing = latData.getDouble(latData.getIndex().set(latSize - 1));
            }
        }

        if (pixelSizeX <= 0 || pixelSizeY <= 0) {
            return null;
        }

        final MapProjection projection = MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME);
        final MapInfo mapInfo = new MapInfo(projection,
                                            (float) pixelX, (float) pixelY,
                                            (float) easting, (float) northing,
                                            (float) pixelSizeX, (float) pixelSizeY,
                                            Datum.WGS_84);
        mapInfo.setSceneWidth(sceneRasterWidth);
        mapInfo.setSceneHeight(sceneRasterHeight);
        return new MapInfoX(mapInfo, yFlipped);
    }
    
    public static int getRasterDataType(Variable variable, DataTypeWorkarounds workarounds) {
        if (workarounds != null && workarounds.hasWorkaroud(variable.getName(), variable.getDataType())) {
            return workarounds.getRasterDataType(variable.getName(), variable.getDataType());
        }
        return getRasterDataType(variable.getDataType(), variable.isUnsigned());
    }

    public static boolean isValidRasterDataType(final DataType dataType) {
        return getRasterDataType(dataType, false) != -1;
    }

    public static int getRasterDataType(final DataType dataType, boolean unsigned) {
        return getProductDataType(dataType, unsigned, true);
    }

    public static int getProductDataType(DataType dataType, boolean unsigned, boolean rasterDataOnly) {
        if (dataType == DataType.BYTE) {
            return unsigned ? ProductData.TYPE_UINT8 : ProductData.TYPE_INT8;
        } else if (dataType == DataType.SHORT) {
            return unsigned ? ProductData.TYPE_UINT16 : ProductData.TYPE_INT16;
        } else if (dataType == DataType.INT) {
            return unsigned ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        } else if (dataType == DataType.FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else if (dataType == DataType.DOUBLE) {
            return ProductData.TYPE_FLOAT64;
        } else if (!rasterDataOnly) {
            if (dataType == DataType.CHAR) {
                // return ProductData.TYPE_ASCII; todo - handle this case
            } else if (dataType == DataType.STRING) {
                return ProductData.TYPE_ASCII;
            }
        }
        return -1;
    }

    public static void transferMetadata(NetcdfFile netcdfFile, MetadataElement root) {
        root.addElement(createMetadataElementFromAttributeList(netcdfFile.getGlobalAttributes(), "MPH"));
        root.addElement(createMetadataElementFromVariableList(netcdfFile.getVariables(), "DSD"));
    }

    public static MetadataElement createMetadataElement(NetcdfFile netcdfFile) {
        return createMetadataElementFromAttributeList(netcdfFile.getGlobalAttributes(),
                                                      NetcdfConstants.GLOBAL_ATTRIBUTES_NAME);
    }

    public static MetadataElement createMetadataElement(Group group) {
        return createMetadataElementFromAttributeList(group.getAttributes(), group.getName());
    }

    public static MetadataElement createMetadataElement(Variable variable) {
        return createMetadataElementFromAttributeList(variable.getAttributes(), variable.getName());
    }

    private static MetadataElement createMetadataElementFromVariableList(final List<Variable> variableList, String elementName) {
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (int i = 0; i < variableList.size(); i++) {
            Variable variable = variableList.get(i);
            metadataElement.addElement(createMetadataElement(variable));
        }
        return metadataElement;
    }

    private static MetadataElement createMetadataElementFromAttributeList(final List<Attribute> attributeList,
                                                                          String elementName) {
        // todo - note that we still do not support NetCDF data type 'char' here!
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (int i = 0; i < attributeList.size(); i++) {
            Attribute attribute = attributeList.get(i);
            final int productDataType = getProductDataType(attribute.getDataType(), false, false);
            if (productDataType != -1) {
                ProductData productData;
                if (attribute.isString()) {
                    productData = ProductData.createInstance(attribute.getStringValue());
                } else if (attribute.isArray()) {
                    productData = ProductData.createInstance(productDataType, attribute.getLength());
                    productData.setElems(attribute.getValues().getStorage());
                } else {
                    productData = ProductData.createInstance(productDataType, 1);
                    productData.setElems(attribute.getValues().getStorage());
                }
                MetadataAttribute metadataAttribute = new MetadataAttribute(attribute.getName(),
                                                                            productData,
                                                                            true);
                metadataElement.addAttribute(metadataAttribute);
            }
        }
        return metadataElement;
    }

    public static String getProductType(final NcAttributeMap attMap) {
        String productType = attMap.getStringValue("Conventions");
        if (productType == null) {
            productType = NetcdfConstants.FORMAT_NAME;
        }
        return productType;
    }

    public static String getProductDescription(final NcAttributeMap attMap) {
        String description = attMap.getStringValue("description");
        if (description == null) {
            description = attMap.getStringValue("title");
            if (description == null) {
                description = attMap.getStringValue("comment");
                if (description == null) {
                    description = NetcdfConstants.FORMAT_DESCRIPTION;
                }
            }
        }
        return description;
    }

    public static ProductData.UTC getSceneRasterStartTime(NcAttributeMap globalAttributes) {
        return getSceneRasterTime(globalAttributes,
                                  NetcdfConstants.START_DATE_ATT_NAME,
                                  NetcdfConstants.START_TIME_ATT_NAME);
    }

    public static ProductData.UTC getSceneRasterStopTime(NcAttributeMap globalAttributes) {
        return getSceneRasterTime(globalAttributes,
                                  NetcdfConstants.STOP_DATE_ATT_NAME,
                                  NetcdfConstants.STOP_TIME_ATT_NAME);
    }

    public static ProductData.UTC getSceneRasterTime(NcAttributeMap globalAttributes,
                                                     final String dateAttName,
                                                     final String timeAttName) {
        final String dateStr = globalAttributes.getStringValue(dateAttName);
        final String timeStr = globalAttributes.getStringValue(timeAttName);
        final String dateTimeStr = getDateTimeString(dateStr, timeStr);

        if (dateTimeStr != null) {
            try {
                return parseDateTime(dateTimeStr);
            } catch (ParseException e) {
                BeamLogManager.getSystemLogger().warning(
                        "Failed to parse time string '" + dateTimeStr + "'");
            }
        }

        return null;
    }

    public static String getDateTimeString(String dateStr, String timeStr) {
        if (dateStr != null && dateStr.endsWith("UTC")) {
            dateStr = dateStr.substring(0, dateStr.length() - 3).trim();
        }
        if (timeStr != null && timeStr.endsWith("UTC")) {
            timeStr = timeStr.substring(0, timeStr.length() - 3).trim();
        }
        if (dateStr != null && timeStr != null) {
            return dateStr + " " + timeStr;
        }
        if (dateStr != null) {
            return dateStr + (dateStr.indexOf(':') == -1 ? " 00:00:00" : "");
        }
        if (timeStr != null) {
            return timeStr + (timeStr.indexOf(':') == -1 ? " 00:00:00" : "");
        }
        return null;
    }

    public static ProductData.UTC parseDateTime(String dateTimeStr) throws ParseException {
        return ProductData.UTC.parse(dateTimeStr, NetcdfConstants.DATE_TIME_PATTERN);
    }

    private NetcdfReaderUtils() {
    }

    private static Variable[] getRasterVariables(Map<NcRasterDim, List<Variable>> variableLists,
                                                 NcRasterDim rasterDim) {
        final List<Variable> list = variableLists.get(rasterDim);
        return list.toArray(new Variable[list.size()]);
    }

    private static NcRasterDim getBestRasterDim(Map<NcRasterDim, List<Variable>> variableListMap) {
        final NcRasterDim[] keys = variableListMap.keySet().toArray(new NcRasterDim[0]);
        if (keys.length == 0) {
            return null;
        }

        NcRasterDim bestRasterDim = null;
        List<Variable> bestVarList = null;
        for (int i = 0; i < keys.length; i++) {
            final NcRasterDim rasterDim = keys[i];
            // CF-Convention for regular lat/lon grids
            if (rasterDim.isTypicalRasterDim()) {
                return rasterDim;
            }
            // Otherwise, the best is the one which holds the most variables
            final List<Variable> varList = variableListMap.get(rasterDim);
            if (bestVarList == null || varList.size() > bestVarList.size()) {
                bestRasterDim = rasterDim;
                bestVarList = varList;
            }
        }

        return bestRasterDim;
    }

    private static Map<NcRasterDim, List<Variable>> getVariableListMap(final Group group) {
        Map<NcRasterDim, List<Variable>> variableLists = new HashMap<NcRasterDim, List<Variable>>(31);
        collectVariableLists(group, variableLists);
        return variableLists;
    }

    private static void collectVariableLists(Group group, Map<NcRasterDim, List<Variable>> variableLists) {
        final List<Variable> variables = group.getVariables();
        for (final Variable variable : variables) {
            final int rank = variable.getRank();
            if (rank >= 2 && isValidRasterDataType(variable.getDataType())) {
                final Dimension dimX = variable.getDimension(rank - 1);
                final Dimension dimY = variable.getDimension(rank - 2);
                if (dimX.getLength() > 1 && dimY.getLength() > 1) {
                    NcRasterDim rasterDim = new NcRasterDim(dimX, dimY);
                    List<Variable> list = variableLists.get(rasterDim);
                    if (list == null) {
                        list = new ArrayList<Variable>();
                        variableLists.put(rasterDim, list);
                    }
                    list.add(variable);
                }
            }
        }
        final List<Group> subGroups = group.getGroups();
        for (final Group subGroup : subGroups) {
            collectVariableLists(subGroup, variableLists);
        }
    }


    /**
     * Return type of the {@link NetcdfReaderUtils#createMapInfoX}()
     * method. Comprises a {@link MapInfo} and a boolean indicating that the reader
     * should flip data along the Y-axis.
     */
    public static class MapInfoX {

        final MapInfo _mapInfo;
        final boolean _yFlipped;

        public MapInfoX(MapInfo mapInfo, boolean yFlipped) {
            _mapInfo = mapInfo;
            _yFlipped = yFlipped;
        }

        public MapInfo getMapInfo() {
            return _mapInfo;
        }

        public boolean isYFlipped() {
            return _yFlipped;
        }
    }
}
