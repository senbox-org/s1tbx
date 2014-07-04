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

package org.esa.beam.dataio.netcdf.util;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;


/**
 * Methods for reading and writing {@link MetadataAttribute} and
 * {@link MetadataElement}
 */
public class MetadataUtils {

    public static final String GLOBAL_ATTRIBUTES = "Global_Attributes";
    public static final String VARIABLE_ATTRIBUTES = "Variable_Attributes";
    private static final String PROPERTY_KEY_METADATA_ELEMENT_LIMIT = "beam.netcdf.metadataElementLimit";
    private static final String DEFAULT_MAX_NUM_VALUES_READ = "100";

    private MetadataUtils() {
    }

    public static void readNetcdfMetadata(NetcdfFile netcdfFile, MetadataElement root) {
        readNetcdfMetadata(netcdfFile, root, getMaxNumValuesRead());
    }

    public static void readNetcdfMetadata(NetcdfFile netcdfFile, MetadataElement root, int maxNumValuesRead) {
        root.addElement(readAttributeList(netcdfFile.getGlobalAttributes(), GLOBAL_ATTRIBUTES));
        root.addElement(readVariableDescriptions(netcdfFile.getVariables(), VARIABLE_ATTRIBUTES, maxNumValuesRead));
    }

    public static MetadataElement readAttributeList(final List<Attribute> attributeList,
                                                    String elementName) {
        // todo - note that we still do not support NetCDF data type 'char' here!
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (Attribute attribute : attributeList) {
            final int productDataType = DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false,
                                                                                   false);
            if (productDataType != -1) {
                ProductData productData = null;
                if (attribute.isString()) {
                    final String stringValue = attribute.getStringValue();
                    if (stringValue != null) {
                        productData = ProductData.createInstance(stringValue);
                    }
                } else if (attribute.isArray()) {
                    final Array values = attribute.getValues();
                    if (values != null) {
                        productData = ProductData.createInstance(productDataType, attribute.getLength());
                        productData.setElems(values.getStorage());
                    }
                } else {
                    final Array values = attribute.getValues();
                    if (values != null) {
                        productData = ProductData.createInstance(productDataType, 1);
                        productData.setElems(values.getStorage());
                    }
                }
                if (productData != null) {
                    MetadataAttribute metadataAttribute = new MetadataAttribute(attribute.getShortName(),
                                                                                productData,
                                                                                true);
                    metadataElement.addAttribute(metadataAttribute);
                }
            }
        }
        return metadataElement;
    }

    public static MetadataElement readVariableDescriptions(final List<Variable> variableList,
                                                           String elementName) {
        return readVariableDescriptions(variableList, elementName, getMaxNumValuesRead());
    }

    public static MetadataElement readVariableDescriptions(final List<Variable> variableList,
                                                           String elementName, int maxNumValuesRead) {
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (Variable variable : variableList) {
            metadataElement.addElement(createMetadataElement(variable, maxNumValuesRead));
        }
        return metadataElement;
    }

    private static int getMaxNumValuesRead() {
        String maxNumValuesRead = System.getProperty(PROPERTY_KEY_METADATA_ELEMENT_LIMIT);
        if (maxNumValuesRead == null) {
            BeamLogManager.getSystemLogger().warning("Missing system property '" + PROPERTY_KEY_METADATA_ELEMENT_LIMIT + ". Falling back to default");
            maxNumValuesRead = DEFAULT_MAX_NUM_VALUES_READ;
        }
        return Integer.parseInt(maxNumValuesRead);
    }

    private static MetadataElement createMetadataElement(Variable variable, int maxNumValuesRead) {
        final MetadataElement element = readAttributeList(variable.getAttributes(), variable.getFullName());
        if (variable.getRank() == 1) {
            final MetadataElement valuesElem = new MetadataElement("Values");
            element.addElement(valuesElem);
            if (variable.getDataType() == DataType.STRUCTURE) {
                final Structure structure = (Structure) variable;
                final List<Variable> structVariables = structure.getVariables();
                for (Variable structVariable : structVariables) {
                    final String name = structVariable.getShortName();
                    final MetadataElement structElem = new MetadataElement(name);
                    valuesElem.addElement(structElem);
                    addAttribute(structVariable, structElem, maxNumValuesRead);
                }
            } else {
                addAttribute(variable, valuesElem, maxNumValuesRead);
            }
        }
        return element;
    }

    private static void addAttribute(Variable variable, MetadataElement valuesElem, int maxNumValuesRead) {
        final DataType ncDataType = variable.getDataType();
        final boolean unsigned = variable.isUnsigned();
        final boolean rasterDataOnly = false;
        final int productDataType = DataTypeUtils.getEquivalentProductDataType(ncDataType, unsigned, rasterDataOnly);
        if (productDataType == -1) {
            return;
        }
        final Array values;
        try {
            long variableSize = variable.getSize();
            if (variableSize >= maxNumValuesRead && maxNumValuesRead >= 0) {
                values = variable.read(new int[]{0}, new int[]{maxNumValuesRead});
                valuesElem.setDescription("Showing " + maxNumValuesRead + " of " + variableSize + " values.");
            } else {
                values = variable.read();
            }
            final ProductData pd = ReaderUtils.createProductData(productDataType, values);
            final MetadataAttribute attribute = new MetadataAttribute("data", pd, true);
            valuesElem.addAttribute(attribute);
        } catch (IOException | InvalidRangeException e) {
            Debug.trace(e);
        }
    }
}
