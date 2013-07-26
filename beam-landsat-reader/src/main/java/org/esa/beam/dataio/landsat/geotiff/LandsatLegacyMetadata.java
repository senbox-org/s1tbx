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

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Dimension;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


class LandsatLegacyMetadata extends AbstractLandsatMetadata {

    private static final Map<String, String> BAND_DESCRIPTIONS = new HashMap<String, String>();

    static {
        BAND_DESCRIPTIONS.put("1", "Visible (30m)");
        BAND_DESCRIPTIONS.put("2", "Visible (30m)");
        BAND_DESCRIPTIONS.put("3", "Visible (30m)");
        BAND_DESCRIPTIONS.put("4", "Near-Infrared (30m)");
        BAND_DESCRIPTIONS.put("5", "Near-Infrared (30m)");
        BAND_DESCRIPTIONS.put("6", "Thermal (120m)");
        BAND_DESCRIPTIONS.put("61", "Thermal - Low Gain (60m)");
        BAND_DESCRIPTIONS.put("62", "Thermal - High Gain (60m)");
        BAND_DESCRIPTIONS.put("7", "Mid-Infrared (30m)");
        BAND_DESCRIPTIONS.put("8", "Panchromatic (15m)");
    }

    private static final String SENSOR_ID = "SENSOR_ID";
    private static final float[] WAVELENGTHS = new float[]{490, 560, 660, 830, 1670, 11500, 2240, 710};
    private static final float[] BANDWIDTHS = new float[]{66, 82, 67, 128, 217, 1000, 252, 380};

    LandsatLegacyMetadata(Reader mtlReader) throws IOException {
        super(mtlReader);
    }

    public LandsatLegacyMetadata(MetadataElement root) throws IOException {
        super(root);
    }

    @Override
    public Dimension getReflectanceDim() {
        return getDimension("PRODUCT_SAMPLES_REF", "PRODUCT_LINES_REF");
    }

    @Override
    public Dimension getThermalDim() {
        return getDimension("PRODUCT_SAMPLES_THM", "PRODUCT_LINES_THM");
    }

    @Override
    public Dimension getPanchromaticDim() {
        return getDimension("PRODUCT_SAMPLES_PAN", "PRODUCT_LINES_PAN");
    }

    @Override
    public String getProductType() {
        return getProductType("PRODUCT_TYPE");
    }

    @Override
    public MetadataElement getProductMetadata() {
        return getMetaDataElementRoot().getElement("PRODUCT_METADATA");
    }

    @Override
    public double getScalingFactor(String bandId) {
        return getScalingFactor(bandId, "MIN_MAX_RADIANCE", "LMIN_BAND", "LMAX_BAND", "MIN_MAX_PIXEL_VALUE", "QCALMIN_BAND", "QCALMAX_BAND");
    }

    @Override
    public double getScalingOffset(String bandId) {
        return getScalingOffset(bandId, "MIN_MAX_RADIANCE", "LMIN_BAND", "LMAX_BAND", "MIN_MAX_PIXEL_VALUE", "QCALMIN_BAND", "QCALMAX_BAND");
    }

    @Override
    public ProductData.UTC getCenterTime() {
        return getCenterTime("ACQUISITION_DATE", "SCENE_CENTER_SCAN_TIME");
    }

    @Override
    public Pattern getOpticalBandFileNamePattern() {
        return Pattern.compile("BAND(\\d{1,2})_FILE_NAME");
    }

    @Override
    public float getWavelength(String bandNumber) {
        String bandIndexNumber = bandNumber.substring(0, 1);
        int index = Integer.parseInt(bandIndexNumber) - 1;
        return WAVELENGTHS[index];
    }

    @Override
    public float getBandwidth(String bandNumber) {
        String bandIndexNumber = bandNumber.substring(0, 1);
        int index = Integer.parseInt(bandIndexNumber) - 1;
        return BANDWIDTHS[index];
    }

    @Override
    public String getBandDescription(String bandNumber) {
        return BAND_DESCRIPTIONS.get(bandNumber);
    }

    @Override
    public String getQualityBandNameKey() {
        return null;
    }

    @Override
    public String getBandNamePrefix(String bandNumber) {
        return "radiance_" + bandNumber;
    }

    boolean isLandsatTM() {
        final MetadataElement productMetadata = getProductMetadata();
        return "TM".equals(productMetadata.getAttributeString(SENSOR_ID));
    }

    boolean isLandsatETM_Plus() {
        final MetadataElement productMetadata = getProductMetadata();
        return "ETM+".equals(productMetadata.getAttributeString(SENSOR_ID));
    }

    boolean isLegacyFormat() {
        MetadataElement metadata = getProductMetadata();
        return metadata.getAttribute("BAND1_FILE_NAME") != null;
    }

}