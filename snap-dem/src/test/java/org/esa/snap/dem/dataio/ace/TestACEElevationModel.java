/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.ace;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 25, 2008
 * To change this template use File | Settings | File Templates.
 */
public class TestACEElevationModel {

    private static double[] expectedValues = {
            1100.0,
            1149.0,
            1080.0,
            1037.0
    };

    @Ignore
    public void testElevationModel() throws Exception {

        final ACEElevationModel dem = getElevationModel();
        int height = 2;
        int width = 2;
        final double[] demValues = new double[width * height];
        int count = 0;

        final GeoPos geoPos = new GeoPos(-18, 20);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                geoPos.setLocation(geoPos.getLat() + x, geoPos.getLon() + y);
                try {
                    demValues[count++] = dem.getElevation(geoPos);
                } catch (Exception e) {
                    assertFalse("Get Elevation threw: " + e.getMessage(), true);
                }
            }
        }

        assertArrayEquals(expectedValues, demValues, 1.0e-6);
    }

    @Test
    public void testFilenameCreation() throws Exception {
        final ACEElevationModel dem = getElevationModel();

        assertEquals("45S004W.ACE", dem.createTileFilename(-45, -4));
        assertEquals("45S004E.ACE", dem.createTileFilename(-45, +4));
        assertEquals("45N004W.ACE", dem.createTileFilename(+45, -4));
        assertEquals("45N004E.ACE", dem.createTileFilename(+45, +4));

        assertEquals("05S045W.ACE", dem.createTileFilename(-5, -45));
        assertEquals("05S045E.ACE", dem.createTileFilename(-5, +45));
        assertEquals("05N045W.ACE", dem.createTileFilename(+5, -45));
        assertEquals("05N045E.ACE", dem.createTileFilename(+5, +45));

        assertEquals("90S180W.ACE", dem.createTileFilename(-90, -180));
        assertEquals("90S180E.ACE", dem.createTileFilename(-90, +180));
        assertEquals("90N180W.ACE", dem.createTileFilename(+90, -180));
        assertEquals("90N180E.ACE", dem.createTileFilename(+90, +180));
    }

    private ACEElevationModel getElevationModel() throws IOException {
        return new ACEElevationModel(new ACEElevationModelDescriptor(), Resampling.BILINEAR_INTERPOLATION);
    }

}
