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

package org.esa.beam.util.math;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GcpGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.util.math.Rotator;

import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 * Tests for class {@link org.esa.beam.util.math.Rotator}..
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class RotatorTest extends TestCase {

    public void testRotateX() {
        final Point2D p1 = new Point2D.Double(0.0, 0.0);
        final Point2D p2 = new Point2D.Double(0.0, 5.0);

        final Rotator rotator = new Rotator(p1, -90.0);
        rotator.transform(p1);
        rotator.transform(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(5.0, p2.getX(), 1.0E-10);
        assertEquals(0.0, p2.getY(), 1.0E-10);

        rotator.transformInversely(p1);
        rotator.transformInversely(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(0.0, p2.getX(), 1.0E-10);
        assertEquals(5.0, p2.getY(), 1.0E-10);
    }

    public void testRotateY() {
        final Point2D p1 = new Point2D.Double(0.0, 45.0);
        final Point2D p2 = new Point2D.Double(0.0, 50.0);

        final Rotator rotator = new Rotator(p1);
        rotator.transform(p1);
        rotator.transform(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(0.0, p2.getX(), 1.0E-10);
        assertEquals(5.0, p2.getY(), 1.0E-10);

        rotator.transformInversely(p1);
        rotator.transformInversely(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(45.0, p1.getY(), 1.0E-10);
        assertEquals(0.0, p2.getX(), 1.0E-10);
        assertEquals(50.0, p2.getY(), 1.0E-10);
    }

    public void testRotateZ() {
        final Point2D p1 = new Point2D.Double(45.0, 0.0);
        final Point2D p2 = new Point2D.Double(50.0, 0.0);

        final Rotator rotator = new Rotator(p1);
        rotator.transform(p1);
        rotator.transform(p2);

        assertEquals(0.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(5.0, p2.getX(), 1.0E-10);
        assertEquals(0.0, p2.getY(), 1.0E-10);

        rotator.transformInversely(p1);
        rotator.transformInversely(p2);

        assertEquals(45.0, p1.getX(), 1.0E-10);
        assertEquals(0.0, p1.getY(), 1.0E-10);
        assertEquals(50.0, p2.getX(), 1.0E-10);
        assertEquals(0.0, p2.getY(), 1.0E-10);
    }

    public void testRotateNorthPole() {
        final double[] lons = new double[]{0.0, 90.0, 180.0, -90.0, -180.0};
        final double[] lats = new double[]{80.0, 80.0, 80.0, 80.0, 80.0};

        final Rotator rotator = new Rotator(0.0, 90.0);

        rotator.transform(lons, lats);

        assertEquals(0.0, lons[0], 1.0E-10);
        assertEquals(-10.0, lats[0], 1.0E-10);
        assertEquals(10.0, lons[1], 1.0E-10);
        assertEquals(0.0, lats[1], 1.0E-10);
        assertEquals(0.0, lons[2], 1.0E-10);
        assertEquals(10.0, lats[2], 1.0E-10);
        assertEquals(-10.0, lons[3], 1.0E-10);
        assertEquals(0.0, lats[3], 1.0E-10);
        assertEquals(0.0, lons[4], 1.0E-10);
        assertEquals(10.0, lats[4], 1.0E-10);

        rotator.transformInversely(lons, lats);

        assertEquals(0.0, lons[0], 1.0E-10);
        assertEquals(80.0, lats[0], 1.0E-10);
        assertEquals(90.0, lons[1], 1.0E-10);
        assertEquals(80.0, lats[1], 1.0E-10);
        assertEquals(180.0, lons[2], 1.0E-10);
        assertEquals(80.0, lats[2], 1.0E-10);
        assertEquals(-90.0, lons[3], 1.0E-10);
        assertEquals(80.0, lats[3], 1.0E-10);
        assertEquals(-180.0, lons[4], 1.0E-10);
        assertEquals(80.0, lats[4], 1.0E-10);
    }

    public void testRotateSouthPole() {
        final double[] lons = new double[]{0.0, 90.0, 180.0, -90.0, -180.0};
        final double[] lats = new double[]{-80.0, -80.0, -80.0, -80.0, -80.0};

        final Rotator rotator = new Rotator(0.0, -90.0);

        rotator.transform(lons, lats);

        assertEquals(0.0, lons[0], 1.0E-10);
        assertEquals(10.0, lats[0], 1.0E-10);
        assertEquals(10.0, lons[1], 1.0E-10);
        assertEquals(0.0, lats[1], 1.0E-10);
        assertEquals(0.0, lons[2], 1.0E-10);
        assertEquals(-10.0, lats[2], 1.0E-10);
        assertEquals(-10.0, lons[3], 1.0E-10);
        assertEquals(0.0, lats[3], 1.0E-10);
        assertEquals(0.0, lons[4], 1.0E-10);
        assertEquals(-10.0, lats[4], 1.0E-10);

        rotator.transformInversely(lons, lats);

        assertEquals(0.0, lons[0], 1.0E-10);
        assertEquals(-80.0, lats[0], 1.0E-10);
        assertEquals(90.0, lons[1], 1.0E-10);
        assertEquals(-80.0, lats[1], 1.0E-10);
        assertEquals(180.0, lons[2], 1.0E-10);
        assertEquals(-80.0, lats[2], 1.0E-10);
        assertEquals(-90.0, lons[3], 1.0E-10);
        assertEquals(-80.0, lats[3], 1.0E-10);
        assertEquals(-180.0, lons[4], 1.0E-10);
        assertEquals(-80.0, lats[4], 1.0E-10);
    }
}
