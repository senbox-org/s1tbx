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

public class UTMTest extends TestCase {

    public void testGetProjectionParams() {
        double[] p;

        p = UTM.getProjectionParams(0, false);
        assertEquals(6378137.0, p[0], 1e-10);
        assertEquals(6356752.3, p[1], 1e-10);
        assertEquals(0.0, p[2], 1e-10);
        assertEquals(-177.0, p[3], 1e-10);
        assertEquals(0.9996, p[4], 1e-10);
        assertEquals(500000.0, p[5], 1e-10);
        assertEquals(0.0, p[6], 1e-10);

        p = UTM.getProjectionParams(32, false);
        assertEquals(6378137.0, p[0], 1e-10);
        assertEquals(6356752.3, p[1], 1e-10);
        assertEquals(0.0, p[2], 1e-10);
        assertEquals(15.0, p[3], 1e-10);
        assertEquals(0.9996, p[4], 1e-10);
        assertEquals(500000.0, p[5], 1e-10);
        assertEquals(0.0, p[6], 1e-10);

        p = UTM.getProjectionParams(59, true);
        assertEquals(6378137.0, p[0], 1e-10);
        assertEquals(6356752.3, p[1], 1e-10);
        assertEquals(0.0, p[2], 1e-10);
        assertEquals(177.0, p[3], 1e-10);
        assertEquals(0.9996, p[4], 1e-10);
        assertEquals(500000.0, p[5], 1e-10);
        assertEquals(10000000.0, p[6], 1e-10);

        p = UTM.getProjectionParams(-1, false);
        assertEquals(-177.0, p[3], 1e-10);

        p = UTM.getProjectionParams(60, false);
        assertEquals(+177.0, p[3], 1e-10);
    }

    public void testGetZoneIndexAndCentralMeridian() {
        assertEquals(0, UTM.getZoneIndex(-190f));
        assertEquals(0, UTM.getZoneIndex(-180f));
        assertEquals(1, UTM.getZoneIndex(-172f));
        assertEquals(15, UTM.getZoneIndex(-90f));
        assertEquals(23, UTM.getZoneIndex(-40f));
        assertEquals(30, UTM.getZoneIndex(0f));
        assertEquals(36, UTM.getZoneIndex(+40f));
        assertEquals(45, UTM.getZoneIndex(+90f));
        assertEquals(58, UTM.getZoneIndex(+172f));
        assertEquals(59, UTM.getZoneIndex(+180f));
        assertEquals(59, UTM.getZoneIndex(+190f));

        assertEquals(-177.0, UTM.getCentralMeridian(-1), 1e-10);
        assertEquals(-177.0, UTM.getCentralMeridian(0), 1e-10);
        assertEquals(-87.0, UTM.getCentralMeridian(15), 1e-10);
        assertEquals(-39.0, UTM.getCentralMeridian(23), 1e-10);
        assertEquals(3.0, UTM.getCentralMeridian(30), 1e-10);
        assertEquals(39.0, UTM.getCentralMeridian(36), 1e-10);
        assertEquals(93.0, UTM.getCentralMeridian(45), 1e-10);
        assertEquals(171.0, UTM.getCentralMeridian(58), 1e-10);
        assertEquals(177.0, UTM.getCentralMeridian(59), 1e-10);
        assertEquals(177.0, UTM.getCentralMeridian(60), 1e-10);
    }
}
