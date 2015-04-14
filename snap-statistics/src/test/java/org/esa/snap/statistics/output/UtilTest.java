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

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import javax.media.jai.Histogram;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class UtilTest {

    @Test
    public void testGetFeatureName() throws Exception {
        assertEquals("myName", Util.getFeatureName(createFeature("name", "myName")));
        assertEquals("myOtherName", Util.getFeatureName(createFeature("NAME", "myOtherName")));
        assertEquals("someThirdName", Util.getFeatureName(createFeature("NAME", "someThirdName")));
        assertEquals("id", Util.getFeatureName(createFeature("noName", "myName")));
    }

    public static SimpleFeature createFeature(String nameAttribute, String nameValue) {
        final SimpleFeatureBuilder featureBuilder = createSimpleFeatureBuilder(nameAttribute);
        featureBuilder.set(nameAttribute, nameValue);
        return featureBuilder.buildFeature("id");
    }

    private static SimpleFeatureBuilder createSimpleFeatureBuilder(String nameAttribute) {
        final SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.add(nameAttribute, String.class);
        typeBuilder.add("geom", Geometry.class);
        typeBuilder.setName("someFeatureType");
        return new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
    }

    @Test
    public void testGetBinWidth() throws Exception {
        assertEquals(0.01, Util.getBinWidth(new Histogram(100, 0.0, 1.0, 1)), 1E-4);
        assertEquals(0.001, Util.getBinWidth(new Histogram(1000, 0.0, 1.0, 1)), 1E-4);
        assertEquals(0.5, Util.getBinWidth(new Histogram(100, -25.0, 25.0, 1)), 1E-4);
        assertEquals(0.01, Util.getBinWidth(new Histogram(100, -1.0, 0.0, 1)), 1E-4);
    }

    @Test
    public void testComputeBinCount() throws Exception {
        assertEquals(1000, Util.computeBinCount(3));
        assertEquals(10000, Util.computeBinCount(4));
        assertEquals(1000000, Util.computeBinCount(6));
    }

    @Test
    public void testComputeBinCount_IllegalArgumentCases() {
        tryIllegalArgument(7);
        tryIllegalArgument(700);
        tryIllegalArgument(-1);
    }

    private void tryIllegalArgument(int accuracy) {
        try {
            Util.computeBinCount(accuracy);
            fail();
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

}
