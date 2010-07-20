package org.esa.beam.dataio.netcdf.metadata.profiles.def;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class DefaultMetadataPart extends ProfilePart {

    private static final String SPLITTER = "/";
    private static final String METADATA_VARIABLE = "metadata";

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final NetcdfFile netcdfFile = ctx.getNetcdfFile();
        Variable metadata = netcdfFile.findVariable(METADATA_VARIABLE);
        if (metadata != null) {
            final MetadataElement metadataRoot = p.getMetadataRoot();
            final List<Attribute> attributeList = metadata.getAttributes();
            for (Attribute attribute : attributeList) {
                String attrName = attribute.getName();
                if (attrName.startsWith(SPLITTER)) {
                    attrName = attrName.substring(1, attrName.length());
                }
                if (attrName.contains(SPLITTER)) {
                    String prefix = attrName.split(SPLITTER)[0];
                    readMetaData(attribute, metadataRoot, prefix);
                } else {
                    ProductData attributeValue = extractValue(attribute);
                    metadataRoot.addAttribute(new MetadataAttribute(attrName, attributeValue, true));
                }
            }
        }
    }

    private void readMetaData(Attribute attribute, MetadataElement metadataRoot, String prefix) {
        // create new subgroup or take existing one
        String[] splittedPrefix = prefix.split(SPLITTER);
        String metaDataElementName = prefix;
        if (splittedPrefix.length > 1) {
            metaDataElementName = splittedPrefix[1];
        }
        MetadataElement metadataElement = metadataRoot.getElement(metaDataElementName);
        if (metadataElement == null) {
            metadataElement = new MetadataElement(metaDataElementName);
            metadataRoot.addElement(metadataElement);
        }
        // cut prefix of attribute name
        String temp = attribute.getName().replaceFirst(SPLITTER + prefix, "");
        if (temp.startsWith(SPLITTER)) {
            temp = temp.substring(1, temp.length());
        }
        String[] splittedAttrName = temp.split(SPLITTER);
        temp = splittedAttrName[0];
        if (splittedAttrName.length > 1) {
            // recursive call
            readMetaData(attribute, metadataElement, prefix + SPLITTER + temp);
        } else {
            // attribute is leaf, add attribute into subgroup
            String newAttributeName = attribute.getName().replace(prefix, "").replace(SPLITTER, "");
            ProductData attributeValue = extractValue(attribute);
            MetadataAttribute newAttribute = new MetadataAttribute(newAttributeName, attributeValue, true);
            metadataElement.addAttribute(newAttribute);
        }
    }

    private ProductData extractValue(Attribute attribute) {
        ProductData attributeValue;
        int productDataType = ReaderUtils.getEquivalentProductDataType(attribute.getDataType(), false, false);
        if (productDataType == ProductData.TYPE_ASCII) {
            attributeValue = ProductData.createInstance(attribute.getStringValue());
        } else {
            attributeValue = ProductData.createInstance(productDataType, attribute.getValues().copyTo1DJavaArray());
        }
        return attributeValue;
    }

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        final MetadataElement root = p.getMetadataRoot();
        if (root != null) {
            final NetcdfFileWriteable ncFile = ctx.getNetcdfFileWriteable();
            final Variable variable = ncFile.addVariable(METADATA_VARIABLE, DataType.BYTE, "");
            writeMetadataElement(root, variable, "");
        }
    }

    private void writeMetadataElement(MetadataElement element, Variable var, String prefix) throws
                                                                                            IOException {
        for (int i = 0; i < element.getNumAttributes(); i++) {
            MetadataAttribute attribute = element.getAttributeAt(i);
            writeMetadataAttribute(attribute, var, prefix);
        }
        for (int i = 0; i < element.getNumElements(); i++) {
            MetadataElement subElement = element.getElementAt(i);
            writeMetadataElement(subElement, var, prefix + SPLITTER + subElement.getName());
        }
    }

    private void writeMetadataAttribute(MetadataAttribute metadataAttr, Variable var, String prefix) throws
                                                                                                     IOException {
        final ProductData productData = metadataAttr.getData();
        if (productData instanceof ProductData.ASCII || productData instanceof ProductData.UTC) {
            var.addAttribute(new Attribute(prefix + SPLITTER + metadataAttr.getName(), productData.getElemString()));
        } else {
            var.addAttribute(
                    new Attribute(prefix + SPLITTER + metadataAttr.getName(), Array.factory(productData.getElems())));
        }
        if (metadataAttr.getUnit() != null) {
            var.addAttribute(
                    new Attribute(prefix + SPLITTER + metadataAttr.getName() + ".unit", metadataAttr.getUnit()));
        }
        if (metadataAttr.getDescription() != null) {
            var.addAttribute(
                    new Attribute(prefix + SPLITTER + metadataAttr.getName() + ".descr",
                                  metadataAttr.getDescription()));
        }
    }
}
