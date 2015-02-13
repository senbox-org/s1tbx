/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.support;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import java.io.FileReader;
import java.io.IOException;

/**
 * This program test how big the effort is to do a point-in-polygon test.
 *
 * The used polygon should be BIG.
 * I did test with a 17000 points lake victoria boundary.
 *
 * The Geometry.contains operation is way more expensive then the
 * PreparedGeometry.contains operation.
 *
 * @author MarcoZ
 */
public class RegionPerformanceTest {

    public static void main(String[] args) throws IOException, ParseException {
        String productFile = args[0];
        String wktFile = args[1];

        Product product = ProductIO.readProduct(productFile);

        GeometryFactory geometryFactory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(geometryFactory);
        Geometry geometry = wktReader.read(new FileReader(wktFile));

        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            int length = polygon.getCoordinates().length;
            System.out.println("geometry.coordinates.length = " + length);
        }
        Geometry envelope = geometry.getEnvelope();
        if (envelope instanceof Polygon) {
            Polygon polygon = (Polygon) envelope;
            int length = polygon.getCoordinates().length;
            System.out.println("envelope.coordinates.length = " + length);
        }
        System.out.println("envelope.wkt = " + envelope);
        System.out.println();

        int width = product.getSceneRasterWidth();
        int height = product.getSceneRasterHeight();
        System.out.println("num product pixels = " + height * width);
        GeoCoding geoCoding = product.getGeoCoding();
        GeoPos geoPos = new GeoPos();
        PixelPos pixelPos = new PixelPos();
        PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(geometry);
        long t1, t2;
        int matches;

        System.out.println();

        ////////////////////////////////////////////
        // Envelope.contains
        matches = 0;
        t1 = System.currentTimeMillis();
        for (int y = 0; y < height; y++) {
            pixelPos.y = y + 0.5f;
            for (int x = 0; x < width; x++) {
                pixelPos.x = x + 0.5f;
                geoCoding.getGeoPos(pixelPos, geoPos);
                Point point = geometryFactory.createPoint(new Coordinate(geoPos.lon, geoPos.lat));
                if (envelope.contains(point)) {
                    matches++;
                }
            }
        }
        t2 = System.currentTimeMillis();
        System.out.println("envelope matches = " + matches);
        System.out.println("envelope times   = " + (t2 - t1));
        System.out.println();

        //////////////////////////////////////////
        // PreparedGeometry.contains
        matches = 0;
        t1 = System.currentTimeMillis();
        for (int y = 0; y < height; y++) {
            pixelPos.y = y + 0.5f;
            for (int x = 0; x < width; x++) {
                pixelPos.x = x + 0.5f;
                geoCoding.getGeoPos(pixelPos, geoPos);
                Point point = geometryFactory.createPoint(new Coordinate(geoPos.lon, geoPos.lat));
                if (preparedGeometry.contains(point)) {
                    matches++;
                }
            }
        }
        t2 = System.currentTimeMillis();
        System.out.println("prepared geometry matches = " + matches);
        System.out.println("prepared geometry times   = " + (t2 - t1));
        System.out.println();

        //////////////////////////////////////////
        // PreparedGeometry.containsProperly
        matches = 0;
        t1 = System.currentTimeMillis();
        for (int y = 0; y < height; y++) {
            pixelPos.y = y + 0.5f;
            for (int x = 0; x < width; x++) {
                pixelPos.x = x + 0.5f;
                geoCoding.getGeoPos(pixelPos, geoPos);
                Point point = geometryFactory.createPoint(new Coordinate(geoPos.lon, geoPos.lat));
                if (preparedGeometry.containsProperly(point)) {
                    matches++;
                }
            }
        }
        t2 = System.currentTimeMillis();
        System.out.println("prepared geometry properly matches = " + matches);
        System.out.println("prepared geometry properly times   = " + (t2 - t1));
        System.out.println();

        //////////////////////////////////////////
        // NOOP
        matches = 0;
        t1 = System.currentTimeMillis();
        for (int y = 0; y < height; y++) {
            pixelPos.y = y + 0.5f;
            for (int x = 0; x < width; x++) {
                pixelPos.x = x + 0.5f;
                geoCoding.getGeoPos(pixelPos, geoPos);
                Point point = geometryFactory.createPoint(new Coordinate(geoPos.lon, geoPos.lat));
                if (point != null) {
                    matches++;
                }
            }
        }
        t2 = System.currentTimeMillis();
        System.out.println("NOOP matches = " + matches);
        System.out.println("NOOP times   = " + (t2 - t1));
        System.out.println();

        ////////////////////////////////////////////
        // Geometry.contains
        matches = 0;
        t1 = System.currentTimeMillis();
        for (int y = 0; y < height; y += 10) {
            pixelPos.y = y + 0.5f;
            for (int x = 0; x < width; x += 10) {
                pixelPos.x = x + 0.5f;
                geoCoding.getGeoPos(pixelPos, geoPos);
                Point point = geometryFactory.createPoint(new Coordinate(geoPos.lon, geoPos.lat));
                if (geometry.contains(point)) {
                    matches++;
                }
            }
        }
        t2 = System.currentTimeMillis();
        System.out.println("geometry matches = " + matches);
        System.out.println("geometry times   = " + (t2 - t1));
    }
}
