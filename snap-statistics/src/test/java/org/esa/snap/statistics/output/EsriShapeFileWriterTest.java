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

package org.esa.snap.statistics.output;

import org.esa.snap.core.util.FeatureUtils;
import org.esa.snap.statistics.TestUtil;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.junit.After;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class EsriShapeFileWriterTest {

    static File testdataDir;

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteTreeOnExit(testdataDir);
    }

    @Test
    public void testSingleShape() throws Exception {
        // preparation
        testdataDir = new File("target/statistics-test-io_1");
        final File targetShapefile = getTestFile("4_pixels_output.shp").getAbsoluteFile();
        final List<SimpleFeature> featureWithAdaptedStatistic = get4PixelsFeaturesWithAdaptedStatistic();

        // execution
        EsriShapeFileWriter.write(featureWithAdaptedStatistic, targetShapefile);

        // verification
        final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = FeatureUtils.getFeatureSource(targetShapefile.toURI().toURL());
        final FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource.getFeatures();

        assertEquals(1, features.size());

        final SimpleFeature simpleFeature = features.features().next();

        assertNotNull(simpleFeature.getProperty("p90_lgl2"));
        assertNotNull(simpleFeature.getProperty("p95_lgl2"));

        assertEquals(0.1, (Double) simpleFeature.getProperty("p90_lgl2").getValue(), 1E-6);
        assertEquals(0.195, (Double) simpleFeature.getProperty("p95_lgl2").getValue(), 1E-6);
    }

    private List<SimpleFeature> get4PixelsFeaturesWithAdaptedStatistic() {
        final URL originalShapefile = getClass().getResource("../4_pixels.shp");
        final FeatureStatisticsWriter featureStatisticsWriter = FeatureStatisticsWriter.createFeatureStatisticsWriter(originalShapefile, null, new BandNameCreator());
        final String[] algorithmNames = {"p90", "p95"};

        featureStatisticsWriter.initialiseOutput(StatisticsOutputContext.create(new String[]{"algal_2"}, algorithmNames));

        HashMap<String, Number> statistics = new HashMap<String, Number>();
        statistics.put("p90", 0.1);
        featureStatisticsWriter.addToOutput("algal_2", "4_pixels.1", statistics);

        statistics.clear();
        statistics.put("p95", 0.195);
        featureStatisticsWriter.addToOutput("algal_2", "4_pixels.1", statistics);

        return featureStatisticsWriter.getFeatures();
    }

    static File getTestFile(String fileName) {
        testdataDir.mkdirs();
        return new File(testdataDir, fileName);
    }
}
