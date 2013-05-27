/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.dem.ace;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.nest.util.TestUtils;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 25, 2008
 * To change this template use File | Settings | File Templates.
 */
public class TestACEElevationModel extends TestCase {


    final ACEElevationModelDescriptor demDescriptor = new ACEElevationModelDescriptor();

    float[] expectedValues = {
            1098.5f, 1148.25f, 1232.0f, 1311.0f, 999.75f, 731.75f, 766.5f, 582.75f, 461.25f, 485.0f, 452.0f, 833.25f,
            614.25f, 393.0f, 525.75f, 2107.25f, 11.5f, 1529.75f, 655.25f, 185.5f, 96.25f, 820.0f, -500.0f, 1979.25f, 842.97363f
    };

    public void testElevationModel() throws Exception {

        if(!demDescriptor.isDemInstalled()) {
            TestUtils.skipTest(this);
            return;
        }

        final ElevationModel dem = demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        final double[] demValues = new double[expectedValues.length];
        int count = 0;

        final GeoPos geoPos = new GeoPos(-18, 20);
        int height = 5;
        int width = 5;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                geoPos.setLocation(geoPos.getLat() + x, geoPos.getLon() + y);
                try {
                    demValues[count++] = dem.getElevation(geoPos);
                } catch (Exception e) {
                    assertFalse("Get Elevation threw", true);
                }
            }
        }
        
        //assertTrue(Arrays.equals(expectedValues, demValues));
    }
}
