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

package org.esa.beam.statistics;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class ShapefileOutputterTest {

    static final File TESTDATA_DIR = new File("target/statistics-test-io");

    @Before
    public void setUp() throws Exception {
        TESTDATA_DIR.mkdirs();
        if (!TESTDATA_DIR.isDirectory()) {
            fail("Can't create test I/O directory: " + TESTDATA_DIR);
        }
    }

    @Ignore // todo - make run and comment in
    @Test
    public void testSingleShape() throws Exception {
        final URL originalShapefile = getClass().getResource("9_pixels.shp");
        final String targetShapefile = getTestFile("9_pixels_output.shp").getAbsolutePath();
        final ShapefileOutputter shapefileOutputter = new ShapefileOutputter(originalShapefile, targetShapefile);
        final String[] algorithmNames = {"p90", "p95"};

        shapefileOutputter.initialiseOutput(new Product[0], new String[] {"algal_2", "algal_2"}, algorithmNames,
                                            null, null, null);

        HashMap<String, Double> statistics = new HashMap<String, Double>();
        statistics.put("p90", 0.1);
        shapefileOutputter.addToOutput("algal_2", "9_pixels.1", statistics);

        statistics.clear();
        statistics.put("p95", 0.195);
        shapefileOutputter.addToOutput("algal_2", "9_pixels.1", statistics);

        shapefileOutputter.finaliseOutput();

        final FeatureSource<SimpleFeatureType,SimpleFeature> featureSource = FeatureUtils.getFeatureSource(new File(targetShapefile).toURI().toURL());
        final FeatureCollection<SimpleFeatureType,SimpleFeature> features = featureSource.getFeatures();

        assertEquals(1, features.size());

        final SimpleFeature simpleFeature = features.features().next();

        assertNotNull(simpleFeature.getProperty("p90_lgl2"));
        assertNotNull(simpleFeature.getProperty("p95_lgl2"));

        assertEquals(0.1, (Double)simpleFeature.getProperty("p90_lgl2").getValue(), 1E-6);
        assertEquals(0.195, (Double)simpleFeature.getProperty("p95_lgl2").getValue(), 1E-6);
    }

    @Test
    public void testThreeShapes() throws Exception {
        final URL originalShapefile = getClass().getResource("polygons.shp");
        final String targetShapefile = getTestFile("polygons_output.shp").getAbsolutePath();
        final ShapefileOutputter shapefileOutputter = new ShapefileOutputter(originalShapefile, targetShapefile);
        final String[] algorithmNames = {"p90", "p95"};

        shapefileOutputter.initialiseOutput(new Product[0], new String[] {"algal_2", "algal_2"}, algorithmNames,
                                            null, null, null);

        HashMap<String, Double> statistics = new HashMap<String, Double>();

        statistics.put("p90", 1.90);
        shapefileOutputter.addToOutput("algal_2", "polygons.1", statistics);

        statistics.clear();
        statistics.put("p90", 2.90);
        shapefileOutputter.addToOutput("algal_2", "polygons.2", statistics);

        statistics.clear();
        statistics.put("p90", 3.90);
        shapefileOutputter.addToOutput("algal_2", "polygons.3", statistics);

        statistics.clear();
        statistics.put("p95", 1.95);
        shapefileOutputter.addToOutput("algal_2", "polygons.1", statistics);

        statistics.clear();
        statistics.put("p95", 2.95);
        shapefileOutputter.addToOutput("algal_2", "polygons.2", statistics);

        statistics.clear();
        statistics.put("p95", 3.95);
        shapefileOutputter.addToOutput("algal_2", "polygons.3", statistics);

        shapefileOutputter.finaliseOutput();

        assertEquals(3, shapefileOutputter.features.size());

        for (SimpleFeature feature : shapefileOutputter.features) {
            if (feature.getID().contains("1")) {
                assertNotNull(feature.getProperty("p90_lgl2"));
                assertNotNull(feature.getProperty("p95_lgl2"));

                assertEquals(1.90, (Double) feature.getProperty("p90_lgl2").getValue(), 1E-6);
                assertEquals(1.95, (Double) feature.getProperty("p95_lgl2").getValue(), 1E-6);
            } else if(feature.getID().contains("2")) {
                assertNotNull(feature.getProperty("p90_lgl2"));
                assertNotNull(feature.getProperty("p95_lgl2"));

                assertEquals(2.90, (Double) feature.getProperty("p90_lgl2").getValue(), 1E-6);
                assertEquals(2.95, (Double)feature.getProperty("p95_lgl2").getValue(), 1E-6);
            }  else if(feature.getID().contains("3")) {
                assertNotNull(feature.getProperty("p90_lgl2"));
                assertNotNull(feature.getProperty("p95_lgl2"));

                assertEquals(3.90, (Double)feature.getProperty("p90_lgl2").getValue(), 1E-6);
                assertEquals(3.95, (Double)feature.getProperty("p95_lgl2").getValue(), 1E-6);
            }
        }
    }

    @Test
    public void testCreateAttributeName() throws Exception {
        final int[] called = new int[1];
        final Handler handler = new Handler() {

            @Override
            public void publish(LogRecord record) {
                assertEquals(Level.WARNING, record.getLevel());
                assertTrue(record.getMessage().contains("exceeds 10 characters in length. Shortened to"));
                called[0]++;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        BeamLogManager.getSystemLogger().addHandler(handler);

        final ShapefileOutputter shapefileOutputter = new ShapefileOutputter(null, null);
        String attributeName1 = shapefileOutputter.createUniqueAttributeName("median", "radiance_12");
        String attributeName2 = shapefileOutputter.createUniqueAttributeName("median", "radiance_13");
        String attributeName3 = shapefileOutputter.createUniqueAttributeName("p90", "radiance_12");
        String attributeName4 = shapefileOutputter.createUniqueAttributeName("p90", "radiance_13");
        String attributeName5 = shapefileOutputter.createUniqueAttributeName("p90", "algal2");
        String attributeName6 = shapefileOutputter.createUniqueAttributeName("p90", "algal1");

        assertEquals("mdn_rdnc12", attributeName1);
        assertEquals("mdn_rdnc13", attributeName2);
        assertEquals("p90_rdnc12", attributeName3);
        assertEquals("p90_rdnc13", attributeName4);
        assertEquals("p90_algal2", attributeName5);
        assertEquals("p90_algal1", attributeName6);

        try {
            shapefileOutputter.createUniqueAttributeName("median", "saharan_dust_index");
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("Too long combination of algorithm name and band name: 'median', 'saharan_dust_index'. " +
                         "Combination must not exceed 10 characters in length.", expected.getMessage());
        }

        assertEquals(4, called[0]);

        BeamLogManager.getSystemLogger().removeHandler(handler);
    }

    static File getTestFile(String fileName) {
        return new File(TESTDATA_DIR, fileName);
    }
}
