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
import org.esa.snap.core.util.FeatureUtils;
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
class GeometryNoFeatureTypeStrategy extends AbstractInterpretationStrategy {

    private static int idCount = 0;
    private String geometryName;

    public GeometryNoFeatureTypeStrategy(String geometryName) {
        this.geometryName = geometryName;
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
        builder.setName(FeatureUtils.createFeatureTypeName(builder.getDefaultGeometry()));
    }

    @Override
    public int getExpectedTokenCount(int attributeCount) {
        return attributeCount;
    }

    @Override
    public String getFeatureId(String[] tokens) {
        return FeatureUtils.createFeatureId(idCount++);
    }

    @Override
    public SimpleFeature interpretLine(String[] tokens, SimpleFeatureBuilder builder, SimpleFeatureType simpleFeatureType) throws IOException, ConversionException {
        for (int attributeIndex = 0; attributeIndex < tokens.length; attributeIndex++) {
            String token = tokens[attributeIndex];
            setAttributeValue(builder, simpleFeatureType, attributeIndex, token);
        }

        String featureId = getFeatureId(tokens);
        return builder.buildFeature(featureId);
    }

    @Override
    public int getStartColumn() {
        return 0;
    }
}
