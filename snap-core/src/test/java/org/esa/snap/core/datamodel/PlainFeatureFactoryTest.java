/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.junit.Assert.*;

public class PlainFeatureFactoryTest {

    @Test
    public void testPlainFeatureType() {
        SimpleFeatureType sft = PlainFeatureFactory.createPlainFeatureType("MyPoint",
                                                                           Point.class,
                                                                           DefaultGeographicCRS.WGS84);

        assertEquals("MyPoint", sft.getTypeName());

        assertNotNull(sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_GEOMETRY));
        assertFalse(sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_GEOMETRY).isNillable());
        assertNull(sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_GEOMETRY).getDefaultValue());
        assertEquals(Point.class, sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_GEOMETRY).getType().getBinding());

        assertNotNull(sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS));
        assertTrue(sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS).isNillable());
        assertNull(sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS).getDefaultValue());

        assertSame(sft.getGeometryDescriptor(), sft.getDescriptor(PlainFeatureFactory.ATTRIB_NAME_GEOMETRY));

        assertNotNull(sft.getGeometryDescriptor().getType());
        assertSame(DefaultGeographicCRS.WGS84, sft.getCoordinateReferenceSystem());
        assertSame(DefaultGeographicCRS.WGS84, sft.getGeometryDescriptor().getCoordinateReferenceSystem());
        assertSame(DefaultGeographicCRS.WGS84, sft.getGeometryDescriptor().getType().getCoordinateReferenceSystem());
    }

    @Test
    public void testCreatePlainFeature() {
        SimpleFeatureType sft = PlainFeatureFactory.createPlainFeatureType("MyPoint",
                                                                           Point.class,
                                                                           DefaultGeographicCRS.WGS84);

        final GeometryFactory gf = new GeometryFactory();
        final Point point = gf.createPoint(new Coordinate(0.5, 0.6));

        final SimpleFeature feature1 = PlainFeatureFactory.createPlainFeature(sft, "_1", point, "fill:#0033AA");

        assertEquals("_1", feature1.getID());
        assertEquals(point, feature1.getDefaultGeometry());
        assertEquals(point, feature1.getAttribute(PlainFeatureFactory.ATTRIB_NAME_GEOMETRY));
        assertEquals("fill:#0033AA", feature1.getAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS));

        SimpleFeature feature2 = PlainFeatureFactory.createPlainFeature(sft, "_2", null, "fill:#0033AA");
        assertNotNull(feature2.getDefaultGeometry());
        assertTrue(gf.createPoint(new Coordinate()).compareTo(feature2.getDefaultGeometry()) == 0);

        final SimpleFeature feature3 = PlainFeatureFactory.createPlainFeature(sft, "_3", point, null);
        assertEquals(null, feature3.getAttribute(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS));
    }
}
