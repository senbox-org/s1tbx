package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;


class LandsatMetadata {

    private final MetadataElement root;

    LandsatMetadata(Reader mtlReader) throws IOException {
         root = parseMTL(mtlReader);
    }

    private MetadataElement parseMTL(Reader mtlReader) throws IOException {

        MetadataElement base = null;
        MetadataElement currentElement = null;
        BufferedReader reader = new BufferedReader(mtlReader);
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("GROUP")) {
                int i = line.indexOf('=');
                String groupName = line.substring(i+1).trim();
                MetadataElement element = new MetadataElement(groupName);
                if (base == null) {
                    base = element;
                    currentElement = element;
                } else {
                    currentElement.addElement(element);
                    currentElement = element;
                }
            } else if (line.startsWith("END_GROUP") && currentElement != null) {
                currentElement = currentElement.getParentElement();
            } else if (line.equals("END")) {
                return base;
            } else if (currentElement != null) {
                MetadataAttribute attribute = createAttribute(line);
                currentElement.addAttribute(attribute);
            }
        }
        return base;
    }

    private MetadataAttribute createAttribute(String line) {
        int i = line.indexOf('=');
        String name = line.substring(0, i).trim();
        String value = line.substring(i+1).trim();
        ProductData pData;
        if (value.startsWith("\"")) {
            value = value.substring(1, value.length()-1);
            pData = ProductData.createInstance(value);
        } else if (value.contains(".")) {
            try {
                double d = Double.parseDouble(value);
                pData = ProductData.createInstance(new double[]{d});
            } catch (NumberFormatException e) {
                 pData = ProductData.createInstance(value);
            }
        } else {
            try {
                int integer = Integer.parseInt(value);
                pData = ProductData.createInstance(new int[]{integer});
            } catch (NumberFormatException e) {
                 pData = ProductData.createInstance(value);
            }
        }
        return new MetadataAttribute(name, pData, true);
    }

    MetadataElement getMetaDataElementRoot() {
        return root;
    }

    Dimension getReflectanceDim() {
        return getDimension("PRODUCT_SAMPLES_REF", "PRODUCT_LINES_REF");
    }

    Dimension getThermalDim() {
        return getDimension("PRODUCT_SAMPLES_THM", "PRODUCT_LINES_THM");
    }

    Dimension getPanchromaticDim() {
        return getDimension("PRODUCT_SAMPLES_PAN", "PRODUCT_LINES_PAN");
    }

    private Dimension getDimension(String widthAttributeName, String heightAttributeName) {
        MetadataElement metadata = root.getElement("PRODUCT_METADATA");
        MetadataAttribute widthAttribute = metadata.getAttribute(widthAttributeName);
        MetadataAttribute heightAttribute = metadata.getAttribute(heightAttributeName);
        if (widthAttribute != null && heightAttribute != null) {
            int width = widthAttribute.getData().getElemInt();
            int height = heightAttribute.getData().getElemInt();
            return new Dimension(width, height);
        } else {
            return null;
        }
    }

    String getProductType() {
        return root.getElement("PRODUCT_METADATA").getAttribute("PRODUCT_TYPE").getData().getElemString();
    }

    double getScalingFactor(String bandId) {
        try {
            MetadataElement minMaxRadiance = root.getElement("MIN_MAX_RADIANCE");
            double lMax = minMaxRadiance.getAttributeDouble("LMAX_BAND" + bandId);
            double lMin = minMaxRadiance.getAttributeDouble("LMIN_BAND" + bandId);

            MetadataElement minMaxPixels = root.getElement("MIN_MAX_PIXEL_VALUE");
            double qMax = minMaxPixels.getAttributeDouble("QCALMAX_BAND" + bandId);
            double qMin = minMaxPixels.getAttributeDouble("QCALMIN_BAND" + bandId);

            return (lMax - lMin) / (qMax - qMin);
        } catch (Exception e) {
            return 1;
        }
    }

     double getScalingOffset(String bandId) {
        try {
            MetadataElement minMaxRadiance = root.getElement("MIN_MAX_RADIANCE");
            double lMax = minMaxRadiance.getAttributeDouble("LMAX_BAND" + bandId);
            double lMin = minMaxRadiance.getAttributeDouble("LMIN_BAND" + bandId);

            MetadataElement minMaxPixels = root.getElement("MIN_MAX_PIXEL_VALUE");
            double qMax = minMaxPixels.getAttributeDouble("QCALMAX_BAND" + bandId);
            double qMin = minMaxPixels.getAttributeDouble("QCALMIN_BAND" + bandId);

            return lMin - ((lMax - lMin) / (qMax - qMin)) * qMin;
        } catch (Exception e) {
            return 0;
        }
    }
}