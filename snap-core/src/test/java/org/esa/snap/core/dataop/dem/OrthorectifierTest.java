/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataop.dem;

import junit.framework.Assert;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Pointing;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrthorectifierTest {

    static final int SCENE_WIDTH = 100;
    static final int SCENE_HEIGHT = 100;
    static final int MAX_ITERATION_COUNT = 50;


    @Test
    public void testThatOrthorectifierIsWellFormed() {

        final Orthorectifier gcDEM = createOrthorectifier();
        final GeoCoding gcWGS = gcDEM.getGeoCoding();
        final Pointing pointing = gcDEM.getPointing();

        assertSame(gcWGS, gcDEM.getGeoCoding());
        assertNull(gcDEM.getElevationModel());
        assertEquals(50, gcDEM.getMaxIterationCount());
        assertTrue(gcDEM.canGetGeoPos());
        assertTrue(gcDEM.canGetPixelPos());

        assertEquals(0.0f, pointing.getElevation(new PixelPos(0, 0)), 1.0e-5f);
        assertEquals(3000.0, pointing.getElevation(new PixelPos(0, 50)), 1.0e-5f);
        assertEquals(6000.0, pointing.getElevation(new PixelPos(0, 100)), 1.0e-5f);
        assertEquals(3000.0, pointing.getElevation(new PixelPos(100, 50)), 1.0e-5f);

        Assert.assertEquals(new GeoPos(10, 0), gcWGS.getGeoPos(new PixelPos(0, 0), null));
        assertEquals(new GeoPos(0, 0), gcWGS.getGeoPos(new PixelPos(0, 100), null));
        assertEquals(new GeoPos(10, 10), gcWGS.getGeoPos(new PixelPos(100, 0), null));
        assertEquals(new GeoPos(0, 10), gcWGS.getGeoPos(new PixelPos(100, 100), null));

        assertEquals(new GeoPos(5, 0), gcWGS.getGeoPos(new PixelPos(0, 50), null));
        assertEquals(new GeoPos(5, 5), gcWGS.getGeoPos(new PixelPos(50, 50), null));
        assertEquals(new GeoPos(10, 5), gcWGS.getGeoPos(new PixelPos(50, 0), null));
    }

    @Test
    public void testThatPredictionCorrectionScopeIsAchieved() {
        final Orthorectifier o = createOrthorectifier();
        final PixelPos pixelPos = o.getPixelPos(new GeoPos(5.0f, 5.0f), null);
        assertEquals(50.0f, pixelPos.x, 1.0e-6);
        assertTrue(pixelPos.y > 50.0f);
        GeoPos geoPosFalse = o.getGeoCoding().getGeoPos(pixelPos, null);
        assertEquals(5.0f, geoPosFalse.lon, 1.0e-6);
        assertTrue(geoPosFalse.lat < 5.0f);
        GeoPos geoPosTrue = o.getGeoPos(pixelPos, null);
        assertEquals(5.0f, geoPosTrue.lon, 1.0e-6);
        assertTrue(geoPosTrue.lat > geoPosFalse.lat);
    }

    Orthorectifier createOrthorectifier() {
        return new Orthorectifier(SCENE_WIDTH,
                                  SCENE_HEIGHT,
                                  new PointingMock(new GeoCodingMock()),
                                  null,
                                  MAX_ITERATION_COUNT);
    }

}
