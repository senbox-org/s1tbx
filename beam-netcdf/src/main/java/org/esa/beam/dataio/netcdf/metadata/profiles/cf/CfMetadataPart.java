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

import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.dataio.netcdf.metadata.Profile;
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class CfMetadataPart extends ProfilePart {

    @Override
    public void read(Profile profile, Product p) throws IOException {
        transferMetadata(profile.getFileInfo().getNetcdfFile(), p.getMetadataRoot());
    }

    @Override
    public void define(Profile ctx, Product p, NetcdfFileWriteable ncFile) throws IOException {

    }

    public static void transferMetadata(NetcdfFile netcdfFile, MetadataElement root) throws IOException {
        root.addElement(createMetadataElementFromAttributeList(netcdfFile.getGlobalAttributes(), "MPH"));
        root.addElement(createMetadataElementFromVariableList(netcdfFile.getVariables(), "DSD"));
    }

    public static MetadataElement createMetadataElementFromAttributeList(final List<Attribute> attributeList,
                                                                         String elementName) {
        // todo - note that we still do not support NetCDF data type 'char' here!
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (Attribute attribute : attributeList) {
            final int productDataType = ReaderUtils.getEquivalentProductDataType(attribute.getDataType(), false,
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

    public static MetadataElement createMetadataElementFromVariableList(final List<Variable> variableList,
                                                                        String elementName) throws IOException {
        MetadataElement metadataElement = new MetadataElement(elementName);
        for (Variable variable : variableList) {
            metadataElement.addElement(createMetadataElement(variable));
        }
        return metadataElement;
    }

    private static MetadataElement createMetadataElement(Variable variable) throws IOException {
        final MetadataElement element = createMetadataElementFromAttributeList(
                variable.getAttributes(), variable.getName());
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

    private static void addAttribute(Variable variable, MetadataElement valuesElem) throws IOException {
        final DataType ncDataType = variable.getDataType();
        final boolean unsigned = variable.isUnsigned();
        final boolean rasterDataOnly = false;
        final int productDataType = ReaderUtils.getEquivalentProductDataType(ncDataType, unsigned, rasterDataOnly);
        final Array values = variable.read();
        final ProductData pd = ReaderUtils.createProductData(productDataType, values);
        final MetadataAttribute attribute = new MetadataAttribute("data", pd, true);
        valuesElem.addAttribute(attribute);
    }

    public static MetadataElement createMetadataElement(NetcdfFile netcdfFile) {
        return createMetadataElementFromAttributeList(netcdfFile.getGlobalAttributes(),
                                                      Constants.GLOBAL_ATTRIBUTES_NAME);
    }

    public static MetadataElement createMetadataElement(Group group) {
        return createMetadataElementFromAttributeList(group.getAttributes(), group.getName());
    }
}
