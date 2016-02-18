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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.util.BeamConstants;


public class RsMathUtilTest extends TestCase {

    public static final double EPS = 1e-5F;

    public RsMathUtilTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(RsMathUtilTest.class);
    }

    /**
     * Tests the functionality of radianceToReflectance():
     */
    public void testRadianceToReflectance() {
        float rad = 80.f;
        float sza = 30.f;

        // check for correct values
        assertEquals(0.1692265f, RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[0]),
                     EPS);
        assertEquals(0.1757149f, RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[5]),
                     EPS);
        assertEquals(0.23116349f, RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[10]),
                     EPS);
        assertEquals(0.3287255f, RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[14]),
                     EPS);
        testVectorVersion();
    }

    /**
     * Tests the functionality of reflectanceToRadiance():
     */
    public void testReflectanceToRadiance() {
        float rad = 80.f;
        float sza = 30.f;
        float solarFlux = 1714.9084f;

        float refl = RsMathUtils.radianceToReflectance(rad, sza, solarFlux);   // this is tested above
        float backToRad = RsMathUtils.reflectanceToRadiance(refl, sza, solarFlux);
        assertEquals(rad, backToRad);
    }

    /**
     * Tests the functionality of radianceToReflectance() vector version
     */
    public void testVectorVersion() {

        float[] rad = {80.f, 80.f, 80.f, 80.f};
        float[] sza = {30.f, 30.f, 30.f, 30.f};

        // check for correct results
        assertEquals(0.1692265f,
                     RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[0], null)[0],
                     EPS);
        assertEquals(0.1757149f,
                     RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[5], null)[0],
                     EPS);
        assertEquals(0.23116349f,
                     RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[10], null)[0],
                     EPS);
        assertEquals(0.3287255f,
                     RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[14], null)[0],
                     EPS);

        // check that if we set a recycle array that it is actually used
        float[] recycle = new float[4];

        assertEquals(recycle,
                     RsMathUtils.radianceToReflectance(rad, sza, BeamConstants.MERIS_SOLAR_FLUXES[0], recycle));
    }

    /**
     * Tests the functionality of zenithToElevation
     */
    public void testZenithToElevation() {
        // function must scale correctly
        assertEquals(45.f, RsMathUtils.zenithToElevation(45.f), 1e-7);

        // vector function must not accept null as input
        float[] returndata;
        try {
            returndata = RsMathUtils.zenithToElevation(null, null);
            fail("exception expected");
        } catch (RuntimeException e) {
        }

        // vector function must scale correctly
        assertEquals(45.f, (RsMathUtils.zenithToElevation(new float[]{45.f, 45.f, 45.f}, null))[0], 1e-7);

        // vector function must use the recycle argument if set
        returndata = new float[3];
        assertEquals(returndata, RsMathUtils.zenithToElevation(new float[]{45.f, 45.f, 45.f}, returndata));
    }

    /**
     * Tests the functionality of elevationToZenith
     */
    public void testElevationToElevation() {
        // function must scale correctly
        assertEquals(45.f, RsMathUtils.elevationToZenith(45.f), 1e-7);

        // vector function must not accept null as input
        float[] returndata;
        try {
            returndata = RsMathUtils.elevationToZenith(null, null);
            fail("exception expected");
        } catch (RuntimeException e) {
        }

        // vector function must scale correctly
        assertEquals(45.f, (RsMathUtils.elevationToZenith(new float[]{45.f, 45.f, 45.f}, null))[0], 1e-7);

        // vector function must use the recycle argument if set
        returndata = new float[3];
        assertEquals(returndata, RsMathUtils.elevationToZenith(new float[]{45.f, 45.f, 45.f}, returndata));
    }

    /**
     * Tests the functionality of simpleBarometric()
     */
    public void testSimpleBarometric() {
        float fRet;

        // at approx 5500 m the pressure must be half the sea pressure
        fRet = RsMathUtils.simpleBarometric(1013.f, 5500.f);
        assertEquals(515.4429931640625f, fRet, EPS);

        // at elevation 0 m the pressures must be identical
        fRet = RsMathUtils.simpleBarometric(1013.f, 0.f);
        assertEquals(1013.f, fRet, EPS);

        // att approx 8000m the pressure must have dropped to 1/e * seaPress
        fRet = RsMathUtils.simpleBarometric(1013.f, 8000.f);
        assertEquals(379.14377f, fRet, EPS);

        // now the tests for the vector version
        // ------------------------------------
        float[] fRetArray;
        float[] pressure = {1013.f, 1013.f, 1013.f, 1013.f, 1013.f};
        float[] halfHeight = {5500.f, 5500.f, 5500.f, 5500.f, 5500.f};

        // at approx 5500 m the pressure must be half the sea pressure
        fRetArray = RsMathUtils.simpleBarometric(pressure, halfHeight, null);
        assertEquals(515.4429931640625f, fRetArray[3], EPS);

        // at elevation 0 m the pressures must be identical
        float[] seaHeight = {0.f, 0.f, 0.f, 0.f, 0.f};
        fRetArray = RsMathUtils.simpleBarometric(pressure, seaHeight, null);
        assertEquals(1013.f, fRetArray[1], EPS);

        // att approx 8000m the pressure must have dropped to 1/e * seaPress
        float[] eHeight = {8000.f, 8000.f, 8000.f, 8000.f, 8000.f};
        fRetArray = RsMathUtils.simpleBarometric(pressure, eHeight, null);
        assertEquals(379.14377f, fRetArray[4], EPS);

        // assert that an exception occurs when arrays do not have the same size
        try {
            float[] wrongHeight = {120.f, 120.f, 120.f};

            fRetArray = RsMathUtils.simpleBarometric(pressure, wrongHeight, null);
            fail("exception expected");
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        // check that the recycle argument is used
        float[] recycle = {0.f, 0.f, 0.f, 0.f, 0.f};
        fRetArray = RsMathUtils.simpleBarometric(pressure, eHeight, recycle);
        assertSame(fRetArray, recycle);

        // check that the recycle argument is not used when it has the wrong size
        float[] recycleShort = {0.f, 0.f, 0.f, 0.f};
        fRetArray = RsMathUtils.simpleBarometric(pressure, eHeight, recycleShort);
        assertTrue(fRetArray != recycle);
    }

    public void testKoschmider() {
        assertEquals(3.92f, RsMathUtils.koschmieder(2.f), EPS);
        assertEquals(0.46117648482f, RsMathUtils.koschmieder(17.f), EPS);
    }

    public void testKoschmider_exception() {
        try {
            RsMathUtils.koschmieder(0.f);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testKoschmiderInv() {
        assertEquals(3.92f, RsMathUtils.koschmiederInv(2.f), EPS);
        assertEquals(39.2f, RsMathUtils.koschmiederInv(0.2f), EPS);
    }

    public void testKoschmiderInv_exception() {
        try {
            RsMathUtils.koschmiederInv(0.f);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }


    public void testApplyGeodeticCorrection() {
        final GeoPos gp = new GeoPos();
        final float re = (float) RsMathUtils.MEAN_EARTH_RADIUS;
        final float dpr = (float) RsMathUtils.DEG_PER_RAD;
        final float h = 0.02f * re / dpr; // meter
        float expLat, expLon;

        gp.lon = 0;
        gp.lat = 0;
        expLon = 0;
        expLat = 0;
        RsMathUtils.applyGeodeticCorrection(gp, h, 0, 0);
        assertEquals(expLon, gp.lon, EPS);
        assertEquals(expLat, gp.lat, EPS);

        gp.lon = 0;
        gp.lat = 0;
        expLon = 0;
        expLat = 0 + 0.02f;
        RsMathUtils.applyGeodeticCorrection(gp, h, 45, 0);
        assertEquals(expLon, gp.lon, EPS);
        assertEquals(expLat, gp.lat, EPS);

        gp.lon = 0;
        gp.lat = 0;
        expLon = 0 + 0.02f;
        expLat = 0;
        RsMathUtils.applyGeodeticCorrection(gp, h, 45, 90);
        assertEquals(expLon, gp.lon, EPS);
        assertEquals(expLat, gp.lat, EPS);

        gp.lon = 0;
        gp.lat = 0;
        expLon = 0;
        expLat = 0 - 0.02f;
        RsMathUtils.applyGeodeticCorrection(gp, h, 45, 180);
        assertEquals(expLon, gp.lon, EPS);
        assertEquals(expLat, gp.lat, EPS);

        gp.lon = 0;
        gp.lat = 0;
        expLon = 0 - 0.02f;
        expLat = 0;
        RsMathUtils.applyGeodeticCorrection(gp, h, 45, 270);
        assertEquals(expLon, gp.lon, EPS);
        assertEquals(expLat, gp.lat, EPS);

        gp.lon = 0;
        gp.lat = 0;
        expLon = 0 - 0.02f;
        expLat = 0;
        RsMathUtils.applyGeodeticCorrection(gp, h, 45, -90);
        assertEquals(expLon, gp.lon, EPS);
        assertEquals(expLat, gp.lat, EPS);

//        gp.lon = 0;
//        gp.lat = 45;
//        expLon = 0;
//        expLat = 45 + dpr * h / re;
//        RsMathUtils.applyGeodeticCorrection(gp, h, 45, 0);
//        assertEquals(expLon, gp.lon, EPS);
//        assertEquals(expLat, gp.lat, EPS);
//
//        gp.lon = 0;
//        gp.lat = 45;
//        expLon = 0;
//        expLat = 45 - dpr * h / re;
//        RsMathUtils.applyGeodeticCorrection(gp, h, 45, 180);
//        assertEquals(expLon, gp.lon, EPS);
//        assertEquals(expLat, gp.lat, EPS);
    }

}
