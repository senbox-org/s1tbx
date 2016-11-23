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

package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.esa.snap.dataio.netcdf.util.MetadataUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BeamMetadataPart extends ProfilePartIO {

    private static final String SPLITTER = ":";
    private static final String SPLITTER2 = "%3a";
    private static final String METADATA_VARIABLE = "metadata";
    private static final String DESCRIPTION_SUFFIX = "descr";
    private static final String UNIT_SUFFIX = "unit";
    private static final List<String> SNAP_GLOBAL_ATTRIBUTES = Arrays.asList(new String[] {"Conventions", "TileSize", "product_type", "metadata_profile", "metadata_version", "start_date", "stop_date", "auto_grouping", "quicklook_band_name", "tiepoint_coordinates", "title" });

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        final NetcdfFile netcdfFile = ctx.getNetcdfFile();
        Variable metadata = netcdfFile.getRootGroup().findVariable(METADATA_VARIABLE);
        final MetadataElement metadataRoot = p.getMetadataRoot();
        MetadataUtils.readNetcdfMetadata(netcdfFile, p.getMetadataRoot());
        if (metadata != null) {
            for (Attribute attribute : metadata.getAttributes()) {
                String attrName = attribute.getShortName();
                if (attrName.startsWith(SPLITTER)) {
                    attrName = attrName.substring(SPLITTER.length(), attrName.length());
                } else if (attrName.startsWith(SPLITTER2)) {
                    attrName = attrName.substring(SPLITTER2.length(), attrName.length());
                }
                if (attrName.contains(SPLITTER)) {
                    String prefix = attrName.split(SPLITTER)[0];
                    readMetadata(attribute, metadataRoot, prefix, SPLITTER);
                } else if (attrName.contains(SPLITTER2)) {
                    String prefix = attrName.split(SPLITTER2)[0];
                    readMetadata(attribute, metadataRoot, prefix, SPLITTER2);
                } else {
                    ProductData attributeValue = DataTypeUtils.createProductData(attribute);
                    metadataRoot.addAttribute(new MetadataAttribute(attrName, attributeValue, true));
                }
            }
        }
    }

    private void readMetadata(Attribute attribute, MetadataElement metadataRoot, String prefix, String splitter) {
        // create new subgroup or take existing one
        String[] splittedPrefix = prefix.split(splitter);
        String metaDataElementName = prefix;
        if (splittedPrefix.length > 1) {
            metaDataElementName = splittedPrefix[splittedPrefix.length - 1];
        }
        MetadataElement metadataElement = metadataRoot.getElement(metaDataElementName);
        if (metadataElement == null) {
            metadataElement = new MetadataElement(metaDataElementName);
            metadataRoot.addElement(metadataElement);
        }
        // cut prefix of attribute name
        String temp = attribute.getShortName();
        if (temp.startsWith(splitter)) {
            temp = temp.substring(splitter.length(), temp.length());
        }
        temp = temp.replaceFirst(prefix, "");
        if (temp.startsWith(splitter)) {
            temp = temp.substring(splitter.length(), temp.length());
        }

        String[] splittedAttrName = temp.split(splitter);
        temp = splittedAttrName[0];
        if (splittedAttrName.length > 1) {
            // recursive call
            readMetadata(attribute, metadataElement, prefix + splitter + temp, splitter);
        } else {
            // attribute is leaf, add attribute into subgroup
            String newAttributeName = attribute.getShortName().replaceFirst(prefix, "").replace(splitter, "");
            if (newAttributeName.endsWith("." + UNIT_SUFFIX)) {
                // setting the unit this way requires that it is written AFTER its attribute
                newAttributeName = newAttributeName.substring(0, newAttributeName.length() - UNIT_SUFFIX.length() - 1);
                MetadataAttribute anAttribute = metadataElement.getAttribute(newAttributeName);
                String value = attribute.getStringValue();
                if (value != null) {
                    anAttribute.setUnit(value);
                }
            } else if (newAttributeName.endsWith("." + DESCRIPTION_SUFFIX)) {
                // setting the description this way requires that it is written AFTER its attribute
                newAttributeName = newAttributeName.substring(0, newAttributeName.length() - DESCRIPTION_SUFFIX.length() - 1);
                MetadataAttribute anAttribute = metadataElement.getAttribute(newAttributeName);
                String value = attribute.getStringValue();
                if (value != null) {
                    anAttribute.setDescription(value);
                }
            } else {
                ProductData attributeValue = DataTypeUtils.createProductData(attribute);
                MetadataAttribute newAttribute = new MetadataAttribute(newAttributeName, attributeValue, true);
                metadataElement.addAttribute(newAttribute);
            }
        }
    }

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        final MetadataElement root = p.getMetadataRoot();
        if (root != null) {
            final NFileWriteable ncFile = ctx.getNetcdfFileWriteable();
            final NVariable variable = ncFile.addScalarVariable(METADATA_VARIABLE, DataType.BYTE);
            writeMetadataElement(ncFile, root, variable, "");
        }
        final MetadataElement globalAttributes = root.getElement(MetadataUtils.GLOBAL_ATTRIBUTES);
        if (globalAttributes != null) {
            for (int i = 0; i < globalAttributes.getNumAttributes(); i++) {
                final MetadataAttribute attribute = globalAttributes.getAttributeAt(i);
                if (!SNAP_GLOBAL_ATTRIBUTES.contains(attribute.getName())) {
                    final ProductData productData = attribute.getData();
                    if (productData.isInt()) {
                        final Number value = productData.getElemInt();
                        ctx.getNetcdfFileWriteable().addGlobalAttribute(attribute.getName(), value);
                    } else if (productData instanceof ProductData.Double) {
                        final Number value = productData.getElemDouble();
                        ctx.getNetcdfFileWriteable().addGlobalAttribute(attribute.getName(), value);
                    } else if (productData instanceof ProductData.Float) {
                        final Number value = productData.getElemFloat();
                        ctx.getNetcdfFileWriteable().addGlobalAttribute(attribute.getName(), value);
                    } else if (productData instanceof ProductData.ASCII || productData instanceof ProductData.UTC) {
                        final String value = productData.getElemString();
                        ctx.getNetcdfFileWriteable().addGlobalAttribute(attribute.getName(), value);
                    } else {
                        final String value = Arrays.asList(productData.getElems()).stream().map(x -> String.valueOf(x)).collect(Collectors.joining(","));
                        ctx.getNetcdfFileWriteable().addGlobalAttribute(attribute.getName(), value);
                    }
                }
            }
        }
    }

    private void writeMetadataElement(NFileWriteable ncFile, MetadataElement element, NVariable ncVariable, String prefix) throws IOException {
        for (int i = 0; i < element.getNumAttributes(); i++) {
            MetadataAttribute attribute = element.getAttributeAt(i);
            writeMetadataAttribute(ncFile, attribute, ncVariable, prefix);
        }
        for (int i = 0; i < element.getNumElements(); i++) {
            MetadataElement subElement = element.getElementAt(i);
            final String subElementName = subElement.getName();
            if (!isGlobalAttributesElement(subElementName)) {
                final String name;
                if (prefix.isEmpty()) {
                    name = subElementName;
                } else {
                    name = prefix + SPLITTER + subElementName;
                }
                writeMetadataElement(ncFile, subElement, ncVariable, name);
            }
        }
    }

    private boolean isGlobalAttributesElement(String subElementName) {
        return MetadataUtils.GLOBAL_ATTRIBUTES.equals(subElementName) ||
                MetadataUtils.VARIABLE_ATTRIBUTES.equals(subElementName);
    }

    private void writeMetadataAttribute(NFileWriteable ncFile, MetadataAttribute metadataAttr, NVariable ncVariable, String prefix) throws IOException {
        final ProductData productData = metadataAttr.getData();
        String ncAttributeName;
        if (prefix.isEmpty()) {
            ncAttributeName = metadataAttr.getName();
        } else {
            ncAttributeName = prefix + SPLITTER + metadataAttr.getName();
        }
        if(!ncFile.isNameValid(ncAttributeName)) {
            ncAttributeName = ncFile.makeNameValid(ncAttributeName);
        }

        if (productData instanceof ProductData.ASCII || productData instanceof ProductData.UTC) {
            ncVariable.addAttribute(ncAttributeName, productData.getElemString());
        } else {
            ncVariable.addAttribute(ncAttributeName, Array.factory(productData.getElems()));
        }
        if (metadataAttr.getUnit() != null) {
            ncVariable.addAttribute(ncAttributeName + "." + UNIT_SUFFIX, metadataAttr.getUnit());
        }
        if (metadataAttr.getDescription() != null) {
            ncVariable.addAttribute(ncAttributeName + "." + DESCRIPTION_SUFFIX, metadataAttr.getDescription());
        }
    }
}
