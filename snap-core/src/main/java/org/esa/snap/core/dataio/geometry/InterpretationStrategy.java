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

package org.esa.snap.core.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
interface InterpretationStrategy {

    void setDefaultGeometry(String defaultGeometry, CoordinateReferenceSystem featureCrs, SimpleFeatureTypeBuilder builder) throws IOException;

    void setName(SimpleFeatureTypeBuilder builder);

    int getExpectedTokenCount(int attributeCount);

    String getFeatureId(String[] tokens);

    SimpleFeature interpretLine(String[] tokens, SimpleFeatureBuilder builder, SimpleFeatureType simpleFeatureType) throws IOException, ConversionException, TransformException;

    int getStartColumn();
}
