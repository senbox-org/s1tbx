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

package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class AbstractPlacemarkDescriptorTest {

    @Test
    public void testSetUserData() throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("name");
        builder.setDefaultGeometry("geom");
        builder.add("geom", Point.class);
        SimpleFeatureType featureType = builder.buildFeatureType();

        new PinDescriptor().setUserData(featureType);

        assertTrue(featureType.getUserData().containsKey(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));
        assertEquals("org.esa.beam.framework.datamodel.PinDescriptor", featureType.getUserData().get(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));

        new TrackDescriptor().setUserData(featureType);

        assertTrue(featureType.getUserData().containsKey(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));
        assertEquals("org.esa.beam.framework.datamodel.TrackDescriptor", featureType.getUserData().get(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));

        new GcpDescriptor().setUserData(featureType);

        assertTrue(featureType.getUserData().containsKey(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));
        assertEquals("org.esa.beam.framework.datamodel.GcpDescriptor", featureType.getUserData().get(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));

        new GeometryDescriptor().setUserData(featureType);

        assertTrue(featureType.getUserData().containsKey(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));
        assertEquals("org.esa.beam.framework.datamodel.GeometryDescriptor", featureType.getUserData().get(PlacemarkDescriptor.PLACEMARK_DESCRIPTOR_KEY));



    }
}
