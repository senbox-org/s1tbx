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
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * @author Thomas Storm
 */
class LandsatReprocessedMetadata extends AbstractLandsatMetadata {

    private static final float[] ETM_PLUS_WAVELENGTHS = {
            485.0f,
            560.0f,
            660.0f,
            835.0f,
            1650.0f,
            11450.0f,
            2220.0f,
            710.0f
    };
    private static final float[] ETM_PLUS_BANDWIDTHS = {
            70.0f,
            80.0f,
            60.0f,
            130.0f,
            200.0f,
            2100.0f,
            260.0f,
            380.0f
    };
    private final LandsatLegacyMetadata landsatLegacyMetadataDelegate;
    private final Landsat8Metadata landsat8MetadataDelegate;
    private final String sensorId;

    public LandsatReprocessedMetadata(FileReader fileReader) throws IOException {
        super(fileReader);
        landsatLegacyMetadataDelegate = new LandsatLegacyMetadata(getMetaDataElementRoot());
        landsat8MetadataDelegate = new Landsat8Metadata(getMetaDataElementRoot());
        sensorId = getMetaDataElementRoot().getElement("PRODUCT_METADATA").getAttribute("SENSOR_ID").getData().getElemString();
    }

    @Override
    public Dimension getReflectanceDim() {
        return landsat8MetadataDelegate.getReflectanceDim();
    }

    @Override
    public Dimension getThermalDim() {
        return landsat8MetadataDelegate.getThermalDim();
    }

    @Override
    public Dimension getPanchromaticDim() {
        return landsat8MetadataDelegate.getPanchromaticDim();
    }

    @Override
    public String getProductType() {
        return landsat8MetadataDelegate.getProductType();
    }

    @Override
    public MetadataElement getProductMetadata() {
        return landsat8MetadataDelegate.getProductMetadata();
    }

    @Override
    public double getScalingFactor(String bandId) {
        return landsat8MetadataDelegate.getScalingFactor(bandId);
    }

    @Override
    public double getScalingOffset(String bandId) {
        return landsat8MetadataDelegate.getScalingOffset(bandId);
    }

    @Override
    public ProductData.UTC getCenterTime() {
        return landsat8MetadataDelegate.getCenterTime();
    }

    @Override
    public Pattern getOpticalBandFileNamePattern() {
        return landsat8MetadataDelegate.getOpticalBandFileNamePattern();
    }

    @Override
    public float getWavelength(String bandNumber) {
        if (sensorId.startsWith("ETM")) {
            String bandIndexNumber = bandNumber.substring(0, 1);
            int index = Integer.parseInt(bandIndexNumber) - 1;
            return ETM_PLUS_WAVELENGTHS[index];
        }
        return landsatLegacyMetadataDelegate.getWavelength(bandNumber);
    }

    @Override
    public float getBandwidth(String bandNumber) {
        if (sensorId.startsWith("ETM")) {
            String bandIndexNumber = bandNumber.substring(0, 1);
            int index = Integer.parseInt(bandIndexNumber) - 1;
            return ETM_PLUS_BANDWIDTHS[index];
        }
        return landsatLegacyMetadataDelegate.getBandwidth(bandNumber);
    }

    @Override
    public String getBandDescription(String bandNumber) {
        return landsatLegacyMetadataDelegate.getBandDescription(bandNumber);
    }

    @Override
    public String getQualityBandNameKey() {
        return landsatLegacyMetadataDelegate.getQualityBandNameKey();
    }

    @Override
    public String getBandNamePrefix(String bandNumber) {
        return landsatLegacyMetadataDelegate.getBandNamePrefix(bandNumber);
    }
}
