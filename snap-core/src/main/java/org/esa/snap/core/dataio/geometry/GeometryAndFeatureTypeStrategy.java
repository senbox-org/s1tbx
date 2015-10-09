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

import java.io.IOException;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
class GeometryAndFeatureTypeStrategy extends AbstractInterpretationStrategy {

    private String geometryName;
    private String featureTypeName;

    GeometryAndFeatureTypeStrategy(String geometryName, String featureTypeName) {
        this.geometryName = geometryName;
        this.featureTypeName = featureTypeName;
    }

    @Override
    public void setDefaultGeometry(String defaultGeometry, CoordinateReferenceSystem featureCrs, SimpleFeatureTypeBuilder builder) {
        if (defaultGeometry != null) {
            builder.setDefaultGeometry(defaultGeometry);
        }
        builder.setDefaultGeometry(geometryName);
    }

    @Override
    public void setName(SimpleFeatureTypeBuilder builder) {
        builder.setName(featureTypeName);
    }

    @Override
    public int getExpectedTokenCount(int attributeCount) {
        return attributeCount + 1;  // (has feature type name);
    }

    @Override
    public SimpleFeature interpretLine(String[] tokens, SimpleFeatureBuilder builder, SimpleFeatureType simpleFeatureType) throws IOException, ConversionException {
        for (int columnIndex = 1; columnIndex < tokens.length; columnIndex++) {
            String token = tokens[columnIndex];
            setAttributeValue(builder, simpleFeatureType, columnIndex - 1, token);
        }
        String featureId = getFeatureId(tokens);
        return builder.buildFeature(featureId);
    }

    @Override
    public String getFeatureId(String[] tokens) {
        return tokens[0];
    }

    @Override
    public int getStartColumn() {
        return 1;
    }
}
