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

package org.esa.beam.processor.cloud;

import junit.framework.TestCase;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.util.math.MathUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloudPNTest extends TestCase {

    private CloudPN cloudPn;

    @Override
	public void setUp() throws IOException, ProcessorException {
        new CloudProcessor().installAuxdata(); // just to extract auxdata
        Map<String,String> cloudConfig = new HashMap<String, String>();
        cloudConfig.put(CloudPN.CONFIG_FILE_NAME, "cloud_config.txt");
        cloudPn = new CloudPN();
        cloudPn.setUp(cloudConfig);
    }

    public void testAltitudeCorrectedPressure() {
        double pressure = 1000;
        double altitude = 100;
        double correctedPressure = cloudPn.altitudeCorrectedPressure(pressure, altitude, true);
        assertEquals("corrected pressure", 988.08, correctedPressure, 0.01);
        correctedPressure = cloudPn.altitudeCorrectedPressure(pressure, altitude, false);
        assertEquals("corrected pressure", 1000, correctedPressure, 0.0001);
    }

    public void testCalculateI() {
        double radiance = 50;
        float sunSpectralFlux = 10;
        double sunZenith = 45;
        double i = cloudPn.calculateI(radiance, sunSpectralFlux, sunZenith);
        assertEquals("calculated i", (radiance / (sunSpectralFlux * Math.cos(sunZenith * MathUtils.DTOR))), i, 0.00001);
    }
}