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

package org.esa.beam.dataio.landsat.geotiff;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Dimension;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Thomas Storm
 */
class Landsat8Metadata extends AbstractLandsatMetadata {

    private static final Map<String, String> BAND_DESCRIPTIONS = new HashMap<String, String>();

    static {
        BAND_DESCRIPTIONS.put("1", "Coastal Aerosol (Operational Land Imager (OLI))");
        BAND_DESCRIPTIONS.put("2", "Blue (OLI)");
        BAND_DESCRIPTIONS.put("3", "Green (OLI)");
        BAND_DESCRIPTIONS.put("4", "Red (OLI)");
        BAND_DESCRIPTIONS.put("5", "Near-Infrared (NIR) (OLI)");
        BAND_DESCRIPTIONS.put("6", "Short Wavelength Infrared (SWIR) 1 (OLI)");
        BAND_DESCRIPTIONS.put("7", "SWIR 2 (OLI)");
        BAND_DESCRIPTIONS.put("8", "Panchromatic (OLI)");
        BAND_DESCRIPTIONS.put("9", "Cirrus (OLI)");
        BAND_DESCRIPTIONS.put("10", "Thermal Infrared Sensor (TIRS) 1");
        BAND_DESCRIPTIONS.put("11", "TIRS 2");
    }

    private final MetadataElement root;

    public Landsat8Metadata(Reader fileReader) throws IOException {
        root = parseMTL(fileReader);
    }

    @Override
    public MetadataElement getProductMetadata() {
        return root.getElement("PRODUCT_METADATA");
    }

    @Override
    public MetadataElement getMetaDataElementRoot() {
        return root;
    }

    @Override
    public Dimension getReflectanceDim() {
        return getDimension("REFLECTIVE_SAMPLES", "REFLECTIVE_LINES");
    }

    @Override
    public Dimension getThermalDim() {
        return getDimension("THERMAL_SAMPLES", "THERMAL_LINES");
    }

    @Override
    public Dimension getPanchromaticDim() {
        return getDimension("PANCHROMATIC_SAMPLES", "PANCHROMATIC_LINES");
    }

    @Override
    public String getProductType() {
        return getProductType("DATA_TYPE");
    }

    @Override
    public double getScalingFactor(String bandId) {
        return getScalingFactor(bandId, "MIN_MAX_RADIANCE", "RADIANCE_MINIMUM_BAND_", "RADIANCE_MAXIMUM_BAND_", "MIN_MAX_PIXEL_VALUE", "QUANTIZE_CAL_MIN_BAND_", "QUANTIZE_CAL_MAX_BAND_");
    }

    @Override
    public double getScalingOffset(String bandId) {
        return getScalingOffset(bandId, "MIN_MAX_RADIANCE", "RADIANCE_MINIMUM_BAND_", "RADIANCE_MAXIMUM_BAND_", "MIN_MAX_PIXEL_VALUE", "QUANTIZE_CAL_MIN_BAND_", "QUANTIZE_CAL_MAX_BAND_");
    }

    @Override
    public ProductData.UTC getCenterTime() {
        return getCenterTime("DATE_ACQUIRED", "SCENE_CENTER_TIME");
    }

    @Override
    public Pattern getBandFileNamePattern() {
        return Pattern.compile("FILE_NAME_BAND_(\\d{1,2})");
    }

    @Override
    public float[] getWavelengths() {
        return new float[]{433, 482, 562, 655, 865, 1610, 2200, 590, 1375, 10800, 12000};
    }

    @Override
    public float[] getBandwidths() {
        return new float[getWavelengths().length];
    }

    @Override
    public String getBandDescription(String bandNumber) {
        return BAND_DESCRIPTIONS.get(bandNumber);
    }
}
