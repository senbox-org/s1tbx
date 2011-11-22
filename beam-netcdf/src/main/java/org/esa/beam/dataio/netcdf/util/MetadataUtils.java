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

    private MetadataUtils() {
    }

    public static void readNetcdfMetadata(NetcdfFile netcdfFile, MetadataElement root) {
        root.addElement(readAttributeList(netcdfFile.getGlobalAttributes(), GLOBAL_ATTRIBUTES));
        root.addElement(readVariableDescriptions(netcdfFile.getVariables(), VARIABLE_ATTRIBUTES));
    }

    public static MetadataElement readAttributeList(final List<Attribute> attributeList,
                                                    String elementName) {
        // todo - note that we still do not support NetCDF data type 'char' here!
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (Attribute attribute : attributeList) {
            final int productDataType = DataTypeUtils.getEquivalentProductDataType(attribute.getDataType(), false,
                                                                                   false);
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

    public static MetadataElement readVariableDescriptions(final List<Variable> variableList,
                                                           String elementName) {
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (Variable variable : variableList) {
            metadataElement.addElement(createMetadataElement(variable));
        }
        return metadataElement;
    }

    private static MetadataElement createMetadataElement(Variable variable) {
        final MetadataElement element = readAttributeList(variable.getAttributes(), variable.getName());
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
                    addAttribute(structVariable, structElem);
                }
            } else {
                addAttribute(variable, valuesElem);
            }
        }
        return element;
    }

    private static void addAttribute(Variable variable, MetadataElement valuesElem) {
        final DataType ncDataType = variable.getDataType();
        final boolean unsigned = variable.isUnsigned();
        final boolean rasterDataOnly = false;
        final int productDataType = DataTypeUtils.getEquivalentProductDataType(ncDataType, unsigned, rasterDataOnly);
        final Array values;
        try {
            long variableSize = variable.getSize();
            if (variable.getRank() == 1 && variableSize >= 100) {
                values = variable.read(new int[]{0}, new int[]{100});
                valuesElem.setDescription("Showing 100 of " + variableSize + " values.");
            } else {
                values = variable.read();
            }
            final ProductData pd = ReaderUtils.createProductData(productDataType, values);
            final MetadataAttribute attribute = new MetadataAttribute("data", pd, true);
            valuesElem.addAttribute(attribute);
        } catch (IOException e) {
            Debug.trace(e);
        } catch (InvalidRangeException e) {
            Debug.trace(e);
        }
    }
}
