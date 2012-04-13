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

import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.io.CsvReader;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class VectorDataNodeReader2Test {

    private static final String INPUT1 = "# BEAM pin export table\n" +
                                        "#\n" +
                                        "# Product:\tsubset_1_MER_RR__1PQBCM20030809_101416_000002002018_00466_07534_0168\n" +
                                        "# Created on:\tThu Apr 12 14:48:36 CEST 2012\n" +
                                        "\n" +
                                        "# Wavelength:\t\t\t\t\t\t\t884.94403\n" +
                                        "org.esa.beam.TrackPoint\tName:String\tX:Double\tY:Double\tLon:Double\tLat:Double\tLabel:String\tDesc:String\tradiance_14:Double\n" +
                                        "0\tpin_1\t689.5\t151.5\t7.777766\t47.96903\tPin 1\tp1\t59.383057\n" +
                                        "1\tpin_2\t317.5\t488.5\t1.5681322\t45.38434\tPin 2\tp2\t93.759186\n" +
                                        "2\tpin_3\t241.5\t475.5\t0.6210307\t45.669746\tPin 3\tp3\t90.469284\n" +
                                        "3\tpin_4\t831.5\t534.5\t7.8942046\t43.675922\tPin 4\tp4\t7.208489\n" +
                                        "4\tpin_5\t665.5\t263.5\t6.9614143\t46.88921\tPin 5\tp5\t80.520226\n" +
                                        "5\tpin_6\t532.5\t313.5\t5.0080223\t46.710358\tPin 6\tp6\t75.52739\n";

    private static final String INPUT2 = "# BEAM pin export table\n" +
                                        "#\n" +
                                        "# Product:\tsubset_1_MER_RR__1PQBCM20030809_101416_000002002018_00466_07534_0168\n" +
                                        "# Created on:\tThu Apr 12 14:48:36 CEST 2012\n" +
                                        "\n" +
                                        "# Wavelength:\t\t\t\t\t\t\t884.94403\n" +
                                        "org.esa.beam.TrackPoint\tName:String\tX:Double\tY:Double\tLat:Double\tLabel:String\tDesc:String\tLon:Double\tradiance_14:Double\n" +
                                        "0\tpin_1\t689.5\t151.5 \t47.96903\tPin 1\tp1\t7.777766 \t59.383057\n" +
                                        "1\tpin_2\t317.5\t488.5\t45.38434 \tPin 2\tp2\t1.5681322\t93.759186\n" +
                                        "2\tpin_3\t241.5\t475.5\t45.669746\tPin 3\tp3\t0.6210307\t90.469284\n" +
                                        "3\tpin_4\t831.5\t534.5\t43.675922\tPin 4\tp4\t7.8942046\t7.208489\n" +
                                        "4\tpin_5\t665.5\t263.5\t46.88921 \tPin 5\tp5\t6.9614143\t80.520226\n" +
                                        "5\tpin_6\t532.5\t313.5\t46.710358\tPin 6\tp6\t5.0080223\t75.52739\n";

    @Test
    public void testTrackFeatureTypeWithInput1() throws Exception {
        List<AttributeDescriptor> attributeDescriptors = getAttributeDescriptors(INPUT1);

        assertEquals("Name", attributeDescriptors.get(0).getLocalName());
        assertEquals("X", attributeDescriptors.get(1).getLocalName());
        assertEquals("Y", attributeDescriptors.get(2).getLocalName());
        assertEquals("Lon", attributeDescriptors.get(3).getLocalName());
        assertEquals("Lat", attributeDescriptors.get(4).getLocalName());
        assertEquals("Label", attributeDescriptors.get(5).getLocalName());
        assertEquals("Desc", attributeDescriptors.get(6).getLocalName());
        assertEquals("radiance_14", attributeDescriptors.get(7).getLocalName());
        assertEquals("geoPos", attributeDescriptors.get(8).getLocalName());
        assertEquals("pixelPos", attributeDescriptors.get(9).getLocalName());

        assertEquals(String.class, attributeDescriptors.get(0).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(1).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(2).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(3).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(4).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(5).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(6).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(7).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(8).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(9).getType().getBinding());
    }

    @Test
    public void testTrackFeatureTypeWithInput2() throws Exception {
        List<AttributeDescriptor> attributeDescriptors = getAttributeDescriptors(INPUT2);

        assertEquals("Name", attributeDescriptors.get(0).getLocalName());
        assertEquals("X", attributeDescriptors.get(1).getLocalName());
        assertEquals("Y", attributeDescriptors.get(2).getLocalName());
        assertEquals("Lat", attributeDescriptors.get(3).getLocalName());
        assertEquals("Label", attributeDescriptors.get(4).getLocalName());
        assertEquals("Desc", attributeDescriptors.get(5).getLocalName());
        assertEquals("Lon", attributeDescriptors.get(6).getLocalName());
        assertEquals("radiance_14", attributeDescriptors.get(7).getLocalName());
        assertEquals("geoPos", attributeDescriptors.get(8).getLocalName());
        assertEquals("pixelPos", attributeDescriptors.get(9).getLocalName());

        assertEquals(String.class, attributeDescriptors.get(0).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(1).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(2).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(3).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(4).getType().getBinding());
        assertEquals(String.class, attributeDescriptors.get(5).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(6).getType().getBinding());
        assertEquals(Double.class, attributeDescriptors.get(7).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(8).getType().getBinding());
        assertEquals(Point.class, attributeDescriptors.get(9).getType().getBinding());
    }

    @Test
    public void testTrackFeatureClassesWithMultipleInputs() throws Exception {
        testTrackFeatureClasses(INPUT1);
        testTrackFeatureClasses(INPUT2);
    }

    private void testTrackFeatureClasses(String input) throws IOException {
        CsvReader csvReader = new CsvReader(new StringReader(input), new char[]{'\t'}, true, "#");
        VectorDataNodeReader2 vectorDataNodeReader = new VectorDataNodeReader2(new DummyGeoCoding(), null, null);
        FeatureCollection<SimpleFeatureType,SimpleFeature> featureCollection = vectorDataNodeReader.readFeatures(csvReader);

        FeatureIterator<SimpleFeature> features = featureCollection.features();
        assertEquals(6, featureCollection.size());

        List<SimpleFeature> simpleFeatures = new ArrayList<SimpleFeature>();

        while (features.hasNext()) {
            simpleFeatures.add(features.next());
        }

        for (SimpleFeature simpleFeature : simpleFeatures) {
            assertTrue(simpleFeature.getDefaultGeometry() instanceof Point);
        }

        assertEquals("pin_1", simpleFeatures.get(0).getAttribute("Name").toString());
        assertEquals("pin_6", simpleFeatures.get(5).getAttribute("Name").toString());

        assertEquals("151.5", simpleFeatures.get(0).getAttribute("Y").toString());
        assertEquals("313.5", simpleFeatures.get(5).getAttribute("Y").toString());

        assertEquals(7.777766, ((Point)simpleFeatures.get(0).getAttribute("geoPos")).getX(), 1E-3);
        assertEquals(47.96903, ((Point)simpleFeatures.get(0).getAttribute("geoPos")).getY(), 1E-3);

        assertEquals(5.0080223, ((Point)simpleFeatures.get(5).getAttribute("geoPos")).getX(), 1E-3);
        assertEquals(46.710358, ((Point)simpleFeatures.get(5).getAttribute("geoPos")).getY(), 1E-3);

        assertEquals(7.77766, ((Point)simpleFeatures.get(0).getAttribute("pixelPos")).getX(), 1E-3);
        assertEquals(47.96903, ((Point)simpleFeatures.get(0).getAttribute("pixelPos")).getY(), 1E-3);

        assertEquals(5.0080223, ((Point)simpleFeatures.get(5).getAttribute("pixelPos")).getX(), 1E-3);
        assertEquals(46.710358, ((Point)simpleFeatures.get(5).getAttribute("pixelPos")).getY(), 1E-3);

    }

    private static List<AttributeDescriptor> getAttributeDescriptors(String input) throws IOException {
        CsvReader csvReader = new CsvReader(new StringReader(input), new char[]{'\t'}, true, "#");
        SimpleFeatureType simpleFeatureType = new VectorDataNodeReader2(new DummyGeoCoding(), null, null).readFeatureType(csvReader);

        assertNotNull(simpleFeatureType);
        assertEquals(10, simpleFeatureType.getAttributeCount());

        return simpleFeatureType.getAttributeDescriptors();
    }

    private static class DummyGeoCoding extends AbstractGeoCoding {

        @Override
        public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
            return false;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            return false;
        }

        @Override
        public boolean canGetPixelPos() {
            return false;
        }

        @Override
        public boolean canGetGeoPos() {
            return false;
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            pixelPos.x = geoPos.lon;
            pixelPos.y = geoPos.lat;
            return null;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            return null;
        }

        @Override
        public Datum getDatum() {
            return null;
        }

        @Override
        public void dispose() {
        }
    }
}
