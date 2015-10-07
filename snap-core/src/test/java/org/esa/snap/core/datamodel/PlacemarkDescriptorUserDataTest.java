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

package org.esa.snap.core.datamodel;

import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class PlacemarkDescriptorUserDataTest {

    @Test
    public void testSetUserData() throws Exception {

        testSetUserDataOf(new PinDescriptor(), "org.esa.snap.core.datamodel.PinDescriptor");
        testSetUserDataOf(new GcpDescriptor(), "org.esa.snap.core.datamodel.GcpDescriptor");
        testSetUserDataOf(new TrackPointDescriptor(), "org.esa.snap.core.datamodel.TrackPointDescriptor");
        testSetUserDataOf(new GeometryDescriptor(), "org.esa.snap.core.datamodel.GeometryDescriptor");

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Horst");
        builder.setDefaultGeometry("the_geom");
        builder.add("the_geom", Point.class);
        SimpleFeatureType featureType = builder.buildFeatureType();
        testSetUserDataOf(new GenericPlacemarkDescriptor(featureType), "org.esa.snap.core.datamodel.GenericPlacemarkDescriptor");
    }

    private void testSetUserDataOf(PlacemarkDescriptor descriptor, String expected) {
        final SimpleFeatureType ft = descriptor.getBaseFeatureType();
        descriptor.setUserDataOf(ft);

        assertTrue(ft.getUserData().containsKey(AbstractPlacemarkDescriptor.PROPERTY_NAME_PLACEMARK_DESCRIPTOR));
        assertEquals(expected, ft.getUserData().get(AbstractPlacemarkDescriptor.PROPERTY_NAME_PLACEMARK_DESCRIPTOR));
    }
}
