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
import java.util.regex.Pattern;

/**
 * @author Thomas Storm
 */
class Landsat8Metadata extends AbstractLandsatMetadata {

    private static final String[] BAND_DESCRIPTIONS = {
            "Coastal Aerosol (Operational Land Imager (OLI))",
            "Blue (OLI)",
            "Green (OLI)",
            "Red (OLI)",
            "Near-Infrared (NIR) (OLI)",
            "Short Wavelength Infrared (SWIR) 1 (OLI)",
            "SWIR 2 (OLI)",
            "Panchromatic (OLI)",
            "Cirrus (OLI)",
            "Thermal Infrared Sensor (TIRS) 1",
            "TIRS 2"
    };

    private static final float[] WAVELENGTHS = new float[]{
            433,
            482,
            562,
            655,
            865,
            1610,
            2200,
            590,
            1375,
            10800,
            12000
    };
    private static final String[] BAND_NAMES = {
            "coastal_aerosol",
            "blue",
            "green",
            "red",
            "near_infrared",
            "swir_1",
            "swir_2",
            "panchromatic",
            "cirrus",
            "thermal_infrared_(tirs)_1",
            "thermal_infrared_(tirs)_2",
    };

    private static final float[] BANDWIDTHS = new float[]{20, 60, 60, 30, 30, 80, 180, 380, 20, 590, 1010};

    public Landsat8Metadata(Reader fileReader) throws IOException {
        super(fileReader);
    }

    public Landsat8Metadata(MetadataElement root) throws IOException {
        super(root);
    }

    @Override
    public MetadataElement getProductMetadata() {
        return getMetaDataElementRoot().getElement("PRODUCT_METADATA");
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
    public Pattern getOpticalBandFileNamePattern() {
        return Pattern.compile("FILE_NAME_BAND_(\\d{1,2})");
    }

    @Override
    public String getQualityBandNameKey() {
        return "FILE_NAME_BAND_QUALITY";
    }

    @Override
    public float getWavelength(String bandIndexNumber) {
        int index = getIndex(bandIndexNumber);
        return WAVELENGTHS[index];
    }

    @Override
    public float getBandwidth(String bandIndexNumber) {
        int index = getIndex(bandIndexNumber);
        return BANDWIDTHS[index];
    }

    @Override
    public String getBandDescription(String bandNumber) {
        int index = getIndex(bandNumber);
        return BAND_DESCRIPTIONS[index];
    }

    @Override
    public String getBandNamePrefix(String bandNumber) {
        int index = getIndex(bandNumber);
        return BAND_NAMES[index];
    }

    private static int getIndex(String bandIndexNumber) {
        return Integer.parseInt(bandIndexNumber) - 1;
    }
}
