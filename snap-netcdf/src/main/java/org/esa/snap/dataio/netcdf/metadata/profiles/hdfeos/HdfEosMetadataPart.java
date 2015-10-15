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

package org.esa.snap.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.metadata.ProfilePartIO;
import org.esa.snap.dataio.netcdf.util.MetadataUtils;
import org.jdom2.Attribute;
import org.jdom2.Element;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class HdfEosMetadataPart extends ProfilePartIO {

    @Override
    public void decode(ProfileReadContext ctx, Product p) throws IOException {
        NetcdfFile ncFile = ctx.getNetcdfFile();
        MetadataElement root = p.getMetadataRoot();
        root.addElement(MetadataUtils.readAttributeList(ncFile.getGlobalAttributes(), "MPH"));
        MetadataElement eosElem = new MetadataElement("EOS");
        createMDE(HdfEosUtils.STRUCT_METADATA, ctx, eosElem);
        createMDE(HdfEosUtils.CORE_METADATA, ctx, eosElem);
        createMDE(HdfEosUtils.ARCHIVE_METADATA, ctx, eosElem);
        root.addElement(eosElem);
        List<Variable> ncVariables = ncFile.getVariables();
        filterVariableList(ncVariables);
        root.addElement(MetadataUtils.readVariableDescriptions(ncVariables, "DSD"));
    }

    private void filterVariableList(List<Variable> ncVariables) {
        Iterator<Variable> variableIterator = ncVariables.iterator();
        while (variableIterator.hasNext()) {
            Variable variable = variableIterator.next();
            String varName = variable.getFullName();
            if (varName.startsWith(HdfEosUtils.STRUCT_METADATA) ||
                    varName.startsWith(HdfEosUtils.CORE_METADATA) ||
                    varName.startsWith(HdfEosUtils.ARCHIVE_METADATA)) {
                variableIterator.remove();
            }
        }
    }

    private static void createMDE(String name, ProfileReadContext ctx, MetadataElement eosElem) throws IOException {
        Element element = (Element) ctx.getProperty(name);
        if (element != null) {
            MetadataElement metadataElement = new MetadataElement(name);
            addDomToMetadata(element, metadataElement);
            eosElem.addElement(metadataElement);
        }
    }

    private static void addDomToMetadata(Element parentDE, MetadataElement parentME) {
        final HashMap<String, List<Element>> map = new HashMap<String, List<Element>>(25);
        List<Element> children = parentDE.getChildren();
        for (Element childDE : children) {
            final String name = childDE.getName();
            List<Element> elementList = map.get(name);
            if (elementList == null) {
                elementList = new ArrayList<Element>(3);
                map.put(name, elementList);
            }
            elementList.add(childDE);
        }
        for (Map.Entry<String, List<Element>> entry : map.entrySet()) {
            String name = entry.getKey();
            final List<Element> elementList = entry.getValue();
            if (elementList.size() > 1) {
                for (int i = 0; i < elementList.size(); i++) {
                    addDomToMetadata(elementList.get(i), name + "." + i, parentME);
                }
            } else {
                addDomToMetadata(elementList.get(0), name, parentME);
            }
        }
    }

    private static void addDomToMetadata(Element childDE, String name, MetadataElement parentME) {
        if (childDE.getChildren().size() > 0 || childDE.getAttributes().size() > 0) {
            final MetadataElement childME = new MetadataElement(name);
            addDomToMetadata(childDE, childME);
            parentME.addElement(childME);

            if (childDE.getAttributes().size() != 0) {
                List attrList = childDE.getAttributes();
                for (Object o : attrList) {
                    Attribute attribute = (Attribute) o;
                    String attributeName = attribute.getName();
                    String attributeValue = attribute.getValue();
                    final ProductData valueMEAtrr = ProductData.createInstance(attributeValue);
                    final MetadataAttribute mdAttribute = new MetadataAttribute(attributeName, valueMEAtrr, true);
                    childME.addAttribute(mdAttribute);
                }
            }
        } else {
            String valueDE = childDE.getValue();
            if (valueDE == null) {
                valueDE = "";
            }
            final ProductData valueME = ProductData.createInstance(valueDE);
            final MetadataAttribute attribute = new MetadataAttribute(name, valueME, true);
            parentME.addAttribute(attribute);
        }
    }


    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        throw new IllegalStateException();
    }
}
