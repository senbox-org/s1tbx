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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GeoPosTest extends TestCase {

    public GeoPosTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(GeoPosTest.class);
    }

    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    public void testConstructor() {
        GeoPos geoPos1 = new GeoPos();
        assertEquals(0.0F, geoPos1.lat, 1.e-10F);
        assertEquals(0.0F, geoPos1.lon, 1.e-10F);

        GeoPos geoPos2 = new GeoPos(-1.4F, 180.3F);
        assertEquals(-1.4F, geoPos2.lat, 1.e-10F);
        assertEquals(180.3F, geoPos2.lon, 1.e-10F);
        assertEquals(geoPos2.lat, geoPos2.getLat(), 1.e-10F);
        assertEquals(geoPos2.lon, geoPos2.getLon(), 1.e-10F);

        GeoPos geoPos3 = new GeoPos(geoPos2);
        assertEquals(geoPos3.lat, geoPos3.lat, 1.e-10F);
        assertEquals(geoPos3.lon, geoPos3.lon, 1.e-10F);
    }

    public void testIsInvalid() {
        assertTrue(new GeoPos(+90, +180).isValid());
        assertTrue(new GeoPos(+90, 0).isValid());
        assertTrue(new GeoPos(+90, -180).isValid());
        assertTrue(new GeoPos(0, +180).isValid());
        assertTrue(new GeoPos(0, 0).isValid());
        assertTrue(new GeoPos(0, -180).isValid());
        assertTrue(new GeoPos(-90, +180).isValid());
        assertTrue(new GeoPos(-90, 0).isValid());
        assertTrue(new GeoPos(-90, -180).isValid());

        assertTrue(new GeoPos(0, -181).isValid());
        assertTrue(new GeoPos(0, 181).isValid());
        assertFalse(new GeoPos(-91, 0).isValid());
        assertFalse(new GeoPos(91, 0).isValid());

        assertFalse(new GeoPos(Float.NaN, Float.NaN).isValid());
        assertFalse(new GeoPos(Float.NaN, 0).isValid());
        assertFalse(new GeoPos(0, Float.NaN).isValid());
    }

    public void testSetInvalid() {
        GeoPos geoPos = new GeoPos();
        assertTrue(geoPos.isValid());
        geoPos.setInvalid();
        assertFalse(geoPos.isValid());
        assertEquals("Inv N (NaN)", geoPos.getLatString());
        assertEquals("Inv E (NaN)", geoPos.getLonString());
    }

    public void testStringConversion() {
        GeoPos geoPos;

        geoPos = new GeoPos(0.0F, 0.0F);
        assertEquals("0\260", geoPos.getLatString());
        assertEquals("0\260", geoPos.getLonString());

        geoPos = new GeoPos(10.0F, 20.0F);
        assertEquals("10\260 N", geoPos.getLatString());
        assertEquals("20\260 E", geoPos.getLonString());

        geoPos = new GeoPos(-10.0F, -20.0F);
        assertEquals("10\260 S", geoPos.getLatString());
        assertEquals("20\260 W", geoPos.getLonString());

        geoPos = new GeoPos(10.5F, 20.5F);
        assertEquals("10\26030' N", geoPos.getLatString());
        assertEquals("20\26030' E", geoPos.getLonString());

        geoPos = new GeoPos(-10.5F, -20.5F);
        assertEquals("10\26030' S", geoPos.getLatString());
        assertEquals("20\26030' W", geoPos.getLonString());

        geoPos = new GeoPos(10.5083333F, 20.5083333F);
        assertEquals("10\26030'30\" N", geoPos.getLatString());
        assertEquals("20\26030'30\" E", geoPos.getLonString());

        geoPos = new GeoPos(-10.5083333F, -20.5083333F);
        assertEquals("10\26030'30\" S", geoPos.getLatString());
        assertEquals("20\26030'30\" W", geoPos.getLonString());

        geoPos = new GeoPos(10.516555F, 20.516555F);
        assertEquals("10\26031' N", geoPos.getLatString());
        assertEquals("20\26031' E", geoPos.getLonString());

        geoPos = new GeoPos(10.99988F, 20.99988F);
        assertEquals("11\260 N", geoPos.getLatString());
        assertEquals("21\260 E", geoPos.getLonString());
    }


    public void testNormalizeLon() {
        assertEquals(0, GeoPos.normalizeLon(0), 0);
        assertEquals(-180, GeoPos.normalizeLon(-180), 0);
        assertEquals(+180, GeoPos.normalizeLon(+180), 0);

        assertEquals(33, GeoPos.normalizeLon(33), 0);
        assertEquals(45, GeoPos.normalizeLon(45), 0);
        assertEquals(90, GeoPos.normalizeLon(90), 0);
        assertEquals(112.4f, GeoPos.normalizeLon(112.4f), 0);
        assertEquals(180, GeoPos.normalizeLon(180), 0);
        assertEquals(-179, GeoPos.normalizeLon(181), 0);
        assertEquals(-90, GeoPos.normalizeLon(270), 0);
        assertEquals(-45, GeoPos.normalizeLon(315), 0);
        assertEquals(0, GeoPos.normalizeLon(360), 0);

        assertEquals(33, GeoPos.normalizeLon(4 * 360 + 33), 0);
        assertEquals(45, GeoPos.normalizeLon(4 * 360 + 45), 0);
        assertEquals(90, GeoPos.normalizeLon(4 * 360 + 90), 0);
        assertEquals(112.4f, GeoPos.normalizeLon(4 * 360 + 112.4f), 1e-4);
        assertEquals(180, GeoPos.normalizeLon(4 * 360 + 180), 0);
        assertEquals(-179, GeoPos.normalizeLon(4 * 360 + 181), 0);
        assertEquals(-90, GeoPos.normalizeLon(4 * 360 + 270), 0);
        assertEquals(-45, GeoPos.normalizeLon(4 * 360 + 315), 0);
        assertEquals(0, GeoPos.normalizeLon(4 * 360 + 360), 0);

        assertEquals(-33, GeoPos.normalizeLon(-33), 0);
        assertEquals(-45, GeoPos.normalizeLon(-45), 0);
        assertEquals(-90, GeoPos.normalizeLon(-90), 0);
        assertEquals(-112.4f, GeoPos.normalizeLon(-112.4f), 0);
        assertEquals(-180, GeoPos.normalizeLon(-180), 0);
        assertEquals(+179, GeoPos.normalizeLon(-181), 0);
        assertEquals(+90, GeoPos.normalizeLon(-270), 0);
        assertEquals(+45, GeoPos.normalizeLon(-315), 0);
        assertEquals(0, GeoPos.normalizeLon(-360), 0);

        assertEquals(-33, GeoPos.normalizeLon(-4 * 360 - 33), 0);
        assertEquals(-45, GeoPos.normalizeLon(-4 * 360 - 45), 0);
        assertEquals(-90, GeoPos.normalizeLon(-4 * 360 - 90), 0);
        assertEquals(-112.4f, GeoPos.normalizeLon(-4 * 360 - 112.4f), 1e-4);
        assertEquals(-180, GeoPos.normalizeLon(-4 * 360 - 180), 0);
        assertEquals(+179, GeoPos.normalizeLon(-4 * 360 - 181), 0);
        assertEquals(+90, GeoPos.normalizeLon(-4 * 360 - 270), 0);
        assertEquals(+45, GeoPos.normalizeLon(-4 * 360 - 315), 0);
        assertEquals(0, GeoPos.normalizeLon(-4 * 360 - 360), 0);

        assertEquals(33, GeoPos.normalizeLon(-4 * 360 + 33), 0);
        assertEquals(45, GeoPos.normalizeLon(-4 * 360 + 45), 0);
        assertEquals(90, GeoPos.normalizeLon(-4 * 360 + 90), 0);
        assertEquals(112.4f, GeoPos.normalizeLon(-4 * 360 + 112.4f), 1e-4);
        assertEquals(-180, GeoPos.normalizeLon(-4 * 360 + 180), 0); // !!!
        assertEquals(-179, GeoPos.normalizeLon(-4 * 360 + 181), 0);
        assertEquals(-90, GeoPos.normalizeLon(-4 * 360 + 270), 0);
        assertEquals(-45, GeoPos.normalizeLon(-4 * 360 + 315), 0);
        assertEquals(0, GeoPos.normalizeLon(-4 * 360 + 360), 0);

    }
}


