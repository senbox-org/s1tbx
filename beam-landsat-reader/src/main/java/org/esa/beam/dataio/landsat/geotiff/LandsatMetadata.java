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

package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;


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
        try {
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
        } finally {
            reader.close();
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
        MetadataElement metadata = getProductMetadata();
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
        return getProductMetadata().getAttribute("PRODUCT_TYPE").getData().getElemString();
    }

    MetadataElement getProductMetadata() {
        return root.getElement("PRODUCT_METADATA");
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

    boolean isLandsatTM() {
        MetadataElement productMetadata = getProductMetadata();
        return "TM".equals(productMetadata.getAttributeString("SENSOR_ID"));
    }

    public ProductData.UTC getCenterTime() {
        MetadataElement productMetadata = getProductMetadata();
        String dateString = productMetadata.getAttributeString("ACQUISITION_DATE");
        String timeString = productMetadata.getAttributeString("SCENE_CENTER_SCAN_TIME");

        try {
            if  (dateString != null && timeString != null)  {
                timeString = timeString.substring(0, 12);
                final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                final Date date = dateFormat.parse(dateString + " " + timeString);
                String milliSeconds = timeString.substring(timeString.length()-3);
                return ProductData.UTC.create(date, Long.parseLong(milliSeconds)*1000);
            }
        } catch (ParseException ignored) {
            // ignore
        }
        return null;
    }
}