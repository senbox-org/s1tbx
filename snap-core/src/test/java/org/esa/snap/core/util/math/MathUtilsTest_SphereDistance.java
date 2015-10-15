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

package org.esa.snap.core.util.math;

import junit.framework.TestCase;

public class MathUtilsTest_SphereDistance extends TestCase {

    public void testSphereDistance_AtEquator() {
        final double meanEarthRadius_km = 6371.0;
        final double meanEarthDiameter_km = meanEarthRadius_km * 2;
        final double sphereOutline = meanEarthDiameter_km * Math.PI;
        double lam1_rad = MathUtils.DTOR * 90.0;
        final double phi1_rad = MathUtils.DTOR * 0.0;
        double lam2_rad = MathUtils.DTOR * 80.0;
        final double phi2_rad = MathUtils.DTOR * 0.0;

        // expected value
        final double expected = sphereOutline / 36;

        // method under Test
        double distance = MathUtils.sphereDistance(meanEarthRadius_km,
                                                   lam1_rad, phi1_rad,
                                                   lam2_rad, phi2_rad);

        assertEquals(expected, distance, 1.0e-11);

        lam1_rad = MathUtils.DTOR * 80.0;
        lam2_rad = MathUtils.DTOR * 90.0;
        distance = MathUtils.sphereDistance(meanEarthRadius_km,
                                            lam1_rad, phi1_rad,
                                            lam2_rad, phi2_rad);
        assertEquals(expected, distance, 1.0e-11);

        lam1_rad = MathUtils.DTOR * -175.0;
        lam2_rad = MathUtils.DTOR * 175.0;
        distance = MathUtils.sphereDistance(meanEarthRadius_km,
                                            lam1_rad, phi1_rad,
                                            lam2_rad, phi2_rad);

        assertEquals(expected, distance, 1.0e-11);
    }

    public void testSphereDistance_VertivalAcrossTheEquator() {
        final double meanEarthRadius_km = 6371.0;
        final double meanEarthDiameter_km = meanEarthRadius_km * 2;
        final double sphereOutline = meanEarthDiameter_km * Math.PI;
        final double lam1_rad = MathUtils.DTOR * 55.0;
        double phi1_rad = MathUtils.DTOR * -5.0;
        final double lam2_rad = MathUtils.DTOR * 55.0;
        double phi2_rad = MathUtils.DTOR * 5.0;

        // expected value
        final double expected = sphereOutline / 36;

        // method under Test
        double distance = MathUtils.sphereDistance(meanEarthRadius_km,
                                                   lam1_rad, phi1_rad,
                                                   lam2_rad, phi2_rad);

        assertEquals(expected, distance, 1.0e-11);

        phi1_rad = MathUtils.DTOR * 5.0;
        phi2_rad = MathUtils.DTOR * -5.0;
        distance = MathUtils.sphereDistance(meanEarthRadius_km,
                                            lam1_rad, phi1_rad,
                                            lam2_rad, phi2_rad);

        assertEquals(expected, distance, 1.0e-11);
    }

    public void testSphereDistance_45degreeAcrossTheEquator() {
        final double meanEarthRadius_km = 6371.0;
        final double meanEarthDiameter_km = meanEarthRadius_km * 2;
        final double sphereOutline = meanEarthDiameter_km * Math.PI;
        final double lam1_rad = MathUtils.DTOR * 45.0;
        final double phi1_rad = MathUtils.DTOR * -5.0;
        final double lam2_rad = MathUtils.DTOR * 55.0;
        final double phi2_rad = MathUtils.DTOR * 5.0;

        // expected
        final double x1 = Math.cos(lam1_rad) * Math.cos(phi1_rad);
        final double y1 = Math.sin(lam1_rad) * Math.cos(phi1_rad);
        final double z1 = Math.sin(phi1_rad);
        final double x2 = Math.cos(lam2_rad) * Math.cos(phi2_rad);
        final double y2 = Math.sin(lam2_rad) * Math.cos(phi2_rad);
        final double z2 = Math.sin(phi2_rad);
        final double cosdelta = x1 * x2 + y1 * y2 + z1 * z2;
        final double expected = sphereOutline / 360 * (MathUtils.RTOD * Math.acos(cosdelta));

        // method under test
        final double distance = MathUtils.sphereDistance(meanEarthRadius_km,
                                                         lam1_rad, phi1_rad,
                                                         lam2_rad, phi2_rad);
        assertEquals(expected, distance, 1.0e-11);
    }

    public void testSphereDistance_beliebig() {
        final double meanEarthRadius_km = 6371.0;
        final double meanEarthDiameter_km = meanEarthRadius_km * 2;
        final double sphereOutline = meanEarthDiameter_km * Math.PI;
        final double lam1_rad = MathUtils.DTOR * 77.2;
        final double phi1_rad = MathUtils.DTOR * 25.3;
        final double lam2_rad = MathUtils.DTOR * 52.6;
        final double phi2_rad = MathUtils.DTOR * 12.9;

        // expected
        final double x1 = Math.cos(lam1_rad) * Math.cos(phi1_rad);
        final double y1 = Math.sin(lam1_rad) * Math.cos(phi1_rad);
        final double z1 = Math.sin(phi1_rad);
        final double x2 = Math.cos(lam2_rad) * Math.cos(phi2_rad);
        final double y2 = Math.sin(lam2_rad) * Math.cos(phi2_rad);
        final double z2 = Math.sin(phi2_rad);
        final double cosdelta = x1 * x2 + y1 * y2 + z1 * z2;
        final double expected = sphereOutline / 360 * (MathUtils.RTOD * Math.acos(cosdelta));

        // method under test
        final double distance = MathUtils.sphereDistance(meanEarthRadius_km,
                                                         lam1_rad, phi1_rad,
                                                         lam2_rad, phi2_rad);
        assertEquals(expected, distance, 1.0e-11);
    }
}
