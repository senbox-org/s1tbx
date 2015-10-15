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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.snap.core.util.FeatureUtils;
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
class LatLonNoFeatureTypeStrategy extends AbstractInterpretationStrategy {

    private static int count = 0;

    private double lat;
    private double lon;
    private int latIndex;
    private int lonIndex;

    LatLonNoFeatureTypeStrategy(int latIndex, int lonIndex) {
        this.latIndex = latIndex;
        this.lonIndex = lonIndex;
    }

    @Override
    public void setDefaultGeometry(String defaultGeometry, CoordinateReferenceSystem featureCrs, SimpleFeatureTypeBuilder builder) throws IOException {
        builder.add("geometry", Point.class, featureCrs);
        builder.setDefaultGeometry("geometry");
    }

    @Override
    public void setName(SimpleFeatureTypeBuilder builder) {
        builder.setName(FeatureUtils.createFeatureTypeName(builder.getDefaultGeometry()));
    }

    @Override
    public int getExpectedTokenCount(int attributeCount) {
        return attributeCount - 1; // geometry added as attributes
    }

    @Override
    public SimpleFeature interpretLine(String[] tokens, SimpleFeatureBuilder builder, SimpleFeatureType simpleFeatureType) throws IOException, ConversionException, TransformException {
        int attributeIndex = 0;
        for (int columnIndex = 0; columnIndex < tokens.length; columnIndex++) {
            String token = tokens[columnIndex];
            if (columnIndex == latIndex) {
                lat = Double.parseDouble(token);
            } else if (columnIndex == lonIndex) {
                lon = Double.parseDouble(token);
            }
            setAttributeValue(builder, simpleFeatureType, attributeIndex, token);
            attributeIndex++;
        }
        builder.set("geometry", new GeometryFactory().createPoint(new Coordinate(lon, lat)));

        String featureId = getFeatureId(tokens);
        return builder.buildFeature(featureId);
    }


    @Override
    public String getFeatureId(String[] tokens) {
        return FeatureUtils.createFeatureId(count++);
    }

    @Override
    public int getStartColumn() {
        return 0;
    }
}
