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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.Rectangle2D;

import static org.junit.Assert.*;

public class SimpleFeatureNodeTest {

    private static final String ATTRIBUTE_NAME_GROUND_SHAPE = "groundShape";

    private StringBuilder eventTrace;
    private SimpleFeatureNode simpleFeatureNode;

    @Before
    public void setup() {
        eventTrace = new StringBuilder();
        final Rectangle2D rectangle = new Rectangle2D.Double(0.0, 0.0, 0.5, 0.5);
        simpleFeatureNode = new SimpleFeatureNode(newPyramidFeature("Cheops", rectangle)) {
            @Override
            public void fireProductNodeChanged(String propertyName, Object oldValue, Object newValue) {
                super.fireProductNodeChanged(propertyName, oldValue, newValue);
                if (eventTrace.length() > 0) {
                    eventTrace.append("; ");
                }
                eventTrace.append(propertyName);
            }
        };
    }

    @Test
    public void initialState() {
        final Object property = simpleFeatureNode.getSimpleFeatureAttribute(ATTRIBUTE_NAME_GROUND_SHAPE);
        final Object geometry = simpleFeatureNode.getDefaultGeometry();
        assertNotNull(property);
        assertNotNull(geometry);

        assertSame(geometry, property);
    }

    @Test
    public void setDefaultGeometry() {
        final Geometry geometry = newGeometry(new Rectangle2D.Double(0.0, 0.0, 2.0, 2.0));
        simpleFeatureNode.setDefaultGeometry(geometry);
        assertSame(geometry, simpleFeatureNode.getDefaultGeometry());
        assertSame(geometry, simpleFeatureNode.getSimpleFeatureAttribute(ATTRIBUTE_NAME_GROUND_SHAPE));
        assertEquals(ATTRIBUTE_NAME_GROUND_SHAPE, eventTrace.toString());
    }

    @Test
    public void setAttribute() {
        final Geometry geometry = newGeometry(new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0));
        simpleFeatureNode.setSimpleFeatureAttribute(ATTRIBUTE_NAME_GROUND_SHAPE, geometry);
        final Object attribute = simpleFeatureNode.getSimpleFeatureAttribute(ATTRIBUTE_NAME_GROUND_SHAPE);
        assertNotNull(attribute);
        assertSame(geometry, attribute);
        assertSame(geometry, simpleFeatureNode.getDefaultGeometry());
        assertEquals(ATTRIBUTE_NAME_GROUND_SHAPE, eventTrace.toString());
    }

    private static SimpleFeature newPyramidFeature(String name, Rectangle2D rectangle) {
        final SimpleFeatureType type = newPyramidFeatureType();
        final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        final Geometry polygon = newGeometry(rectangle);
        featureBuilder.set(ATTRIBUTE_NAME_GROUND_SHAPE, polygon);

        return featureBuilder.buildFeature(name);
    }

    private static SimpleFeatureType newPyramidFeatureType() {
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("PyramidType");
        builder.add(ATTRIBUTE_NAME_GROUND_SHAPE, Polygon.class);
        builder.setDefaultGeometry(ATTRIBUTE_NAME_GROUND_SHAPE);

        return builder.buildFeatureType();
    }

    private static Polygon newGeometry(Rectangle2D rectangle) {
        final GeometryFactory geometryFactory = new GeometryFactory();
        final double x = rectangle.getX();
        final double y = rectangle.getY();
        final double w = rectangle.getWidth();
        final double h = rectangle.getHeight();
        final LinearRing linearRing = geometryFactory.createLinearRing(new Coordinate[]{
                new Coordinate(x, y),
                new Coordinate(x + w, y),
                new Coordinate(x + w, y + h),
                new Coordinate(x, y + h),
                new Coordinate(x, y),
        });
        return geometryFactory.createPolygon(linearRing, null);
    }
}
