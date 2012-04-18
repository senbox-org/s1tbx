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

package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.jai.ImageManager;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
class LatLonNoFeatureTypeStrategy extends AbstractInterpretationStrategy {

    private FeatureIdCreator idCreator;

    private double lat;
    private double lon;
    private int latIndex;
    private int lonIndex;
    private GeoCoding geoCoding;

    LatLonNoFeatureTypeStrategy(GeoCoding geoCoding, int latIndex, int lonIndex) {
        this.latIndex = latIndex;
        this.lonIndex = lonIndex;
        this.geoCoding = geoCoding;
        idCreator = new FeatureIdCreator();
    }

    @Override
    public void setDefaultGeometry(SimpleFeatureTypeBuilder builder) {
        builder.add("geoPos", Point.class, DefaultGeographicCRS.WGS84);
        builder.add("pixelPos", Point.class, geoCoding.getImageCRS());
        builder.setDefaultGeometry("pixelPos");
    }

    @Override
    public void setName(SimpleFeatureTypeBuilder builder) {
        builder.setName(
                builder.getDefaultGeometry() + "_" +
                        new SimpleDateFormat("dd-MMM-yyyy'T'HH:mm:ss").format(Calendar.getInstance().getTime()));
    }

    @Override
    public int getExpectedTokenCount(int attributeCount) {
        return attributeCount - 2; // pixelPos and geoPos added as attributes
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
        builder.set("geoPos", new GeometryFactory().createPoint(new Coordinate(lon, lat)));
        PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos((float) lat, (float) lon), null);

        if (pixelPos.isValid()) {
            Geometry geometry = createPointGeometry(pixelPos);
            CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(geoCoding);
            AffineTransform imageToModelTransform = ImageManager.getImageToModelTransform(geoCoding);
            GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(new AffineTransform2D(imageToModelTransform));
            transformer.setCoordinateReferenceSystem(modelCrs);
            geometry = transformer.transform(geometry);
            builder.set("pixelPos", geometry);
            String featureId = getFeatureId(tokens);
            return builder.buildFeature(featureId);
        }
        return null;
    }


    @Override
    public String getFeatureId(String[] tokens) {
        return idCreator.createFeatureId();
    }

    @Override
    public void transformGeoPosToPixelPos(SimpleFeature simpleFeature) {
        // not needed
    }

    @Override
    public int getStartColumn() {
        return 0;
    }

    private static class FeatureIdCreator {

        private static int count = 0;

        private String createFeatureId() {
            return "ID" + String.format("%08d", count++);
        }
    }
}
