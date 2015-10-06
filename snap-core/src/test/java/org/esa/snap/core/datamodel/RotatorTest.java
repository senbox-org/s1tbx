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

import junit.framework.TestCase;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Arrays;

/**
 * Tests for class {@link Rotator}..
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class RotatorTest extends TestCase {

    public void testRotatedPoleTransform() throws Exception {
        // Values are taken from http://www.arolla.ethz.ch/IDL/RotateGridInfo.pdf
        final double rotatedPoleLon = 10.0;
        final double rotatedPoleLat = 32.5;
        final Rotator rotator = new Rotator(rotatedPoleLon, 90.0 - rotatedPoleLat);

        final Point2D p1 = new Point.Double(-10.0, 35.0);
        rotator.transform(p1);

        assertEquals(-17.339, p1.getX(), 0.05);
        assertEquals(-19.939, p1.getY(), 0.05);

        final Point2D p2 = new Point.Double(-11.0, 44.0);
        rotator.transform(p2);

        assertEquals(-15.232, p2.getX(), 0.05);
        assertEquals(-11.137, p2.getY(), 0.05);
    }

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

    public void testRotateToGcpCenter() {
        final double[] lats = new double[]{
                    85, 84, 83,
                    75, 74, 73,
                    65, 64, 63
        };
        final double[] lons = new double[]{
                    -15, -5, 5,
                    -16, -6, 4,
                    -17, -7, 3
        };

        final GeoPos geoPos = GcpGeoCoding.calculateCentralGeoPos(lons, lats);

        final Rotator rotator = new Rotator(geoPos.lon, geoPos.lat);
        final double[] lons2 = Arrays.copyOf(lons, lons.length);
        final double[] lats2 = Arrays.copyOf(lats, lats.length);

        rotator.transform(lons2, lats2);
        rotator.transformInversely(lons2, lats2);

        for (int i = 0; i < lats.length; i++) {
            assertEquals(lats[i], lats2[i], 1.0E-6);
            assertEquals(lons[i], lons2[i], 1.0E-6);
        }
    }

    // test that rotating really improves the GCP approximation
    public void doNotTestRotationOfRealSceneOfItalianLakes() {
        double[] x = new double[]{
                    43.5,
                    37.5,
                    523.5,
                    530.5,
                    1075.5,
                    1074.5,
                    832.5,
                    229.5,
                    524.5
        };
        double[] y = new double[]{
                    22.5,
                    284.5,
                    289.5,
                    18.5,
                    17.5,
                    284.5,
                    157.5,
                    157.5,
                    155.5
        };
        double[] lats = new double[]{
                    49.27275,
                    46.573524,
                    45.553078,
                    48.319298,
                    46.770004,
                    44.09146,
                    46.09978,
                    47.545685,
                    46.92791
        };
        double[] lons = new double[]{
                    6.051173,
                    5.255776,
                    11.624002,
                    12.795015,
                    19.971725,
                    18.575577,
                    16.156733,
                    8.208557,
                    12.158185
        };

        final GeoPos geoPos = GcpGeoCoding.calculateCentralGeoPos(lons, lats);

        final Rotator rotator = new Rotator(geoPos.lon, geoPos.lat);
        final double[] lons2 = Arrays.copyOf(lons, lons.length);
        final double[] lats2 = Arrays.copyOf(lats, lats.length);

        rotator.transform(lons2, lats2);

        final GcpGeoCoding.RationalFunctionMap2D forwardMap =
                    new GcpGeoCoding.RationalFunctionMap2D(2, 0, x, y, lons, lats);
        final GcpGeoCoding.RationalFunctionMap2D forwardMap2 =
                    new GcpGeoCoding.RationalFunctionMap2D(2, 0, x, y, lons2, lats2);

        System.out.println("forwardMap.getRmseU() = " + forwardMap.getRmseU());
        System.out.println("forwardMap.getRmseV() = " + forwardMap.getRmseV());
        System.out.println("forwardMap2.getRmseU() = " + forwardMap2.getRmseU());
        System.out.println("forwardMap2.getRmseV() = " + forwardMap2.getRmseV());
        assertTrue(forwardMap.getRmseU() > forwardMap2.getRmseU());
        assertTrue(forwardMap.getRmseV() > forwardMap2.getRmseV());

        final GcpGeoCoding.RationalFunctionMap2D inverseMap =
                    new GcpGeoCoding.RationalFunctionMap2D(2, 0, lons, lats, x, y);
        final GcpGeoCoding.RationalFunctionMap2D inverseMap2 =
                    new GcpGeoCoding.RationalFunctionMap2D(2, 0, lons2, lats2, x, y);

        System.out.println("inverseMap.getRmseU() = " + inverseMap.getRmseU());
        System.out.println("inverseMap.getRmseV() = " + inverseMap.getRmseV());
        System.out.println("inverseMap2.getRmseU() = " + inverseMap2.getRmseU());
        System.out.println("inverseMap2.getRmseV() = " + inverseMap2.getRmseV());
        assertTrue(inverseMap.getRmseU() > inverseMap2.getRmseU());
        // here the rotation worsens the GCP approximation! Why?
        assertTrue(inverseMap.getRmseV() > inverseMap2.getRmseV());

        rotator.transformInversely(lons2, lats2);

        for (int i = 0; i < lats.length; i++) {
            assertEquals(lats[i], lats2[i], 1.0E-6);
            assertEquals(lons[i], lons2[i], 1.0E-6);
        }
    }

    public void testRotate10_20to0_0() {
        final Rotator rotator = new Rotator(20, 10);

        final Point2D.Double rotatedPoint1 = new Point2D.Double(0d,0d);
        final Point2D.Double rotatedPoint2 = new Point2D.Double(-5d, 0);
        final Point2D.Double rotatedPoint3 = new Point2D.Double(5d, 0);

        rotator.transformInversely(rotatedPoint1);
        rotator.transformInversely(rotatedPoint2);
        rotator.transformInversely(rotatedPoint3);

        assertEquals(20d, rotatedPoint1.getX(), 1e-14);
        assertEquals(10d,rotatedPoint1.getY(), 1e-14);
        assertEquals(14.9232, rotatedPoint2.getX(), 1e-4);
        assertEquals(9.9615, rotatedPoint2.getY(), 1e-4);
        assertEquals(25.0767, rotatedPoint3.getX(), 1e-4);
        assertEquals(9.9615, rotatedPoint3.getY(), 1e-4);
    }
}
