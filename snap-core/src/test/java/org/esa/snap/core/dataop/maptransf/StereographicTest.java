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

package org.esa.snap.core.dataop.maptransf;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.GeoPos;

import java.awt.geom.Point2D;


/**
 * Description of StereographicTest
 *
 * @author Marco Peters
 */
public class StereographicTest extends TestCase {

    private static final double EPSILON = 1.0e-6;
    private final double[] DEFAULT_PARAMS = new double[]{
        Ellipsoid.WGS_84.getSemiMajor(), // semi_major (meter)
        Ellipsoid.WGS_84.getSemiMinor(), // semi_minor (meter)
        90.0, // central parallel (degree)
        0.0, // central meridian (degree)
        1.0, //  scale factor
        0.0, // false_easting (meter)
        0.0 // false_northing (meter)
    };

    public void testForwardTransform() {
        final MapTransform transform = MapTransformFactory.createTransform(StereographicDescriptor.TYPE_ID,
                                                                           DEFAULT_PARAMS);
        final float lat = 90.0f, lon = 25.0f;
        GeoPos geoPos = new GeoPos(lat, lon);

        Point2D mapPoint = transform.forward(geoPos, null);

        assertEquals(0.0, mapPoint.getX(), EPSILON);
        assertEquals(0.0, mapPoint.getY(), EPSILON);
    }

    public void testForwardAndInverseTransform() {
        final MapTransform transform = MapTransformFactory.createTransform(StereographicDescriptor.TYPE_ID,
                                                                           DEFAULT_PARAMS);

        float lat = 90f, lon = 11.87305555f;
        GeoPos geoPos = new GeoPos(lat, lon);

        Point2D mapPoint = transform.forward(geoPos, null);

        GeoPos testPos = transform.inverse(mapPoint, null);
        assertEquals(90.0, testPos.getLat(), EPSILON);
        assertEquals(0, testPos.getLon(), EPSILON);

        lat = 53.786666666f;
        lon = 11.87305555f;
        geoPos = new GeoPos(lat, lon);

        mapPoint = transform.forward(geoPos, null);

        testPos = transform.inverse(mapPoint, null);
        assertEquals(lat, testPos.getLat(), EPSILON);
        assertEquals(lon, testPos.getLon(), EPSILON);
    }

}
