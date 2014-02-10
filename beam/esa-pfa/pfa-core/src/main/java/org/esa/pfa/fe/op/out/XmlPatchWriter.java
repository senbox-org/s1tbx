package org.esa.pfa.fe.op.out;

import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.datamodel.Product;
import org.esa.pfa.fe.op.AttributeType;
import org.esa.pfa.fe.op.Feature;
import org.esa.pfa.fe.op.FeatureType;
import org.esa.pfa.fe.op.Patch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Writes a single XML file "fex-metadata.xml" for each product.
 *
 * @author Norman Fomferra
 */
public class XmlPatchWriter implements PatchWriter {

    private static final String OVERVIEW_XML_FILE_NAME = "fex-overview.xml";
    private static final String OVERVIEW_XSL_FILE_NAME = "fex-overview.xsl";
    private static final String OVERVIEW_CSS_FILE_NAME = "fex-overview.css";

    private final File productTargetDir;
    private Writer xmlWriter;
    private Product sourceProduct;
    private FeatureType[] featureTypes;

    public XmlPatchWriter(File productTargetDir) throws IOException {
        this.productTargetDir = productTargetDir;
    }

    @Override
    public void initialize(PropertySet configuration, Product sourceProduct, FeatureType... featureTypes) throws IOException {
        this.sourceProduct = sourceProduct;
        this.featureTypes = featureTypes;
        PatchWriterHelpers.copyResource(getClass(), OVERVIEW_XSL_FILE_NAME, productTargetDir);
        PatchWriterHelpers.copyResource(getClass(), OVERVIEW_CSS_FILE_NAME, productTargetDir);

        xmlWriter = new FileWriter(new File(productTargetDir, OVERVIEW_XML_FILE_NAME));
        xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlWriter.write("<?xml-stylesheet type=\"text/xsl\" href=\"fex-overview.xsl\"?>\n");
        xmlWriter.write("<featureExtraction source=\"" + this.sourceProduct.getName() + "\">\n");
        writeFeatureTypeXml();
    }

    @Override
    public void writePatch(Patch patch, Feature... features) throws IOException {
        xmlWriter.write(String.format("<patch id=\"%s\" patchX=\"%s\" patchY=\"%s\">\n",
                                      patch.getPatchName(), patch.getPatchX(), patch.getPatchY()));
        for (Feature feature : features) {
            if (PatchWriterHelpers.isImageFeatureType(feature.getFeatureType())) {
                writeImageFeatureXml(feature, patch.getPatchName() + "/" + feature.getName() + ".png");
            } else if (PatchWriterHelpers.isProductFeatureType(feature.getFeatureType())) {
                writeProductFeatureXml(feature, patch.getPatchName() + "/" + feature.getName() + ".dim");
            } else {
                writeAttributedFeatureXml(feature);
            }
        }
        xmlWriter.write("</patch>\n");
    }

    @Override
    public void close() throws IOException {
        xmlWriter.write("</featureExtraction>\n");
        xmlWriter.close();
    }


    private void writeFeatureTypeXml() throws IOException {
        for (FeatureType featureType : featureTypes) {
            if (featureType.hasAttributes()) {
                AttributeType[] attributeTypes = featureType.getAttributeTypes();
                xmlWriter.write(String.format("<featureType name=\"%s\">\n", featureType.getName()));
                for (AttributeType attributeType : attributeTypes) {
                    xmlWriter.write(String.format("\t<attributeType name=\"%s\" valueType=\"%s\"/>\n", attributeType.getName(), attributeType.getValueType().getSimpleName()));
                }
                xmlWriter.write(String.format("</featureType>\n"));
            } else {
                xmlWriter.write(String.format("<featureType name=\"%s\" valueType=\"%s\"/>\n", featureType.getName(), featureType.getValueType().getSimpleName()));
            }
        }

    }

    private void writeProductFeatureXml(Feature feature, String productPath) throws IOException {
        xmlWriter.write(String.format("\t<feature name=\"%s\" type=\"raw\">%s</feature>\n", feature.getName(), productPath));
    }

    private void writeImageFeatureXml(Feature feature, String imagePath) throws IOException {
        xmlWriter.write(String.format("\t<feature name=\"%s\" type=\"img\">%s</feature>\n", feature.getName(), imagePath));
    }

    private void writeAttributedFeatureXml(Feature feature) throws IOException {
        if (feature.hasAttributes()) {
            Object[] attributeValues = feature.getAttributeValues();
            xmlWriter.write(String.format("\t<feature name=\"%s\">\n", feature.getName()));
            for (int i = 0; i < attributeValues.length; i++) {
                Object value = attributeValues[i];
                String tagName = feature.getFeatureType().getAttributeTypes()[i].getName();
                if (value instanceof Double || value instanceof Float) {
                    xmlWriter.write(String.format("\t\t<%s>%.5f</%s>\n", tagName, value, tagName));
                } else if (value != null) {
                    xmlWriter.write(String.format("\t\t<%s>%s</%s>\n", tagName, value, tagName));
                } else {
                    xmlWriter.write(String.format("\t\t<%s/>\n", tagName));
                }
            }
            xmlWriter.write(String.format("\t</feature>\n"));
        } else {
            Object value = feature.getValue();
            if (value instanceof Double || value instanceof Float) {
                xmlWriter.write(String.format("\t<feature name=\"%s\">%.5f</feature>\n", feature.getName(), value));
            } else if (value != null) {
                xmlWriter.write(String.format("\t<feature name=\"%s\">%s</feature>\n", feature.getName(), value));
            } else {
                xmlWriter.write(String.format("\t<feature name=\"%s\"/>\n", feature.getName()));
            }
        }
    }

}
