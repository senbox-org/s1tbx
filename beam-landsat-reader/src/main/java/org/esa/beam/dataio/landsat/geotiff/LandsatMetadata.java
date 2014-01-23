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
import java.util.regex.Pattern;

/**
 * @author Thomas Storm
 */
interface LandsatMetadata {

    MetadataElement getMetaDataElementRoot();

    Dimension getReflectanceDim();

    Dimension getThermalDim();

    Dimension getPanchromaticDim();

    String getProductType();

    MetadataElement getProductMetadata();

    double getScalingFactor(String bandId);

    double getScalingOffset(String bandId);

    ProductData.UTC getCenterTime();

    Pattern getOpticalBandFileNamePattern();

    float getWavelength(String bandNumber);

    float getBandwidth(String bandNumber);

    String getBandDescription(String bandNumber);

    String getQualityBandNameKey();

    String getBandNamePrefix(String bandNumber);
}
