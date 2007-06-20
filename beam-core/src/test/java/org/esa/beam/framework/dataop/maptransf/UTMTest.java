/*
 * $Id: UTMTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.maptransf;

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
