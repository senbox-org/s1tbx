package org.esa.beam.dataio.netcdf4.convention.hdfeos;

import org.esa.beam.dataio.netcdf4.convention.HeaderDataWriter;
import org.esa.beam.dataio.netcdf4.convention.Model;
import org.esa.beam.dataio.netcdf4.convention.ModelPart;
import org.esa.beam.dataio.netcdf4.convention.cf.CfMetadataPart;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.jdom.Attribute;
import org.jdom.Element;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class HdfEosMetadata implements ModelPart {

    @Override
    public void read(Product p, Model model) throws IOException {
        NetcdfFile ncFile = model.getReaderParameters().getNetcdfFile();
        MetadataElement root = p.getMetadataRoot();
        root.addElement(CfMetadataPart.createMetadataElementFromAttributeList(ncFile.getGlobalAttributes(), "MPH"));
        MetadataElement eosElem = new MetadataElement("EOS");
        createMDE(HdfEosUtils.STRUCT_METADATA, ncFile.getRootGroup(), eosElem);
        createMDE(HdfEosUtils.CORE_METADATA, ncFile.getRootGroup(), eosElem);
        createMDE(HdfEosUtils.ARCHIVE_METADATA, ncFile.getRootGroup(), eosElem);
        root.addElement(eosElem);
        List<Variable> ncVariables = ncFile.getVariables();
        filterVariableList(ncVariables);
        root.addElement(CfMetadataPart.createMetadataElementFromVariableList(ncVariables, "DSD"));
    }

    private void filterVariableList(List<Variable> ncVariables) {
        Iterator<Variable> variableIterator = ncVariables.iterator();
        while (variableIterator.hasNext()) {
            Variable variable = variableIterator.next();
            String varName = variable.getName();
            if (varName.startsWith(HdfEosUtils.STRUCT_METADATA) ||
                varName.startsWith(HdfEosUtils.CORE_METADATA) ||
                varName.startsWith(HdfEosUtils.ARCHIVE_METADATA)) {
                variableIterator.remove();
            }
        }
    }

    private static void createMDE(String name, Group eosGroup, MetadataElement eosElem) throws IOException {
        Element element = HdfEosUtils.getEosElement(name, eosGroup);
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
    public void write(Product p, NetcdfFileWriteable ncFile, HeaderDataWriter hdw, Model model) throws IOException {
        throw new IllegalStateException();
    }
}
