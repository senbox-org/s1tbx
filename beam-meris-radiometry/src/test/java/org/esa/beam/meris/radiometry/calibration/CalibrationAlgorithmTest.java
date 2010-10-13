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

package org.esa.beam.meris.radiometry.calibration;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class CalibrationAlgorithmTest {

    @Test
    public void calibration() throws IOException, URISyntaxException {
        final String oldRacResource = "MER_RAC_AXVIEC20050708_135553_20021224_121445_20041213_220000";
        final String newRacResource = "MER_RAC_AXVACR20091016_154511_20021224_121445_20041213_220000";
        final InputStream oldRacStream = getClass().getResourceAsStream(oldRacResource);
        final InputStream newRacStream = getClass().getResourceAsStream(newRacResource);
        final CalibrationAlgorithm calibrationAlgorithm = new CalibrationAlgorithm(Resolution.RR, 1247.4,
                                                                                   oldRacStream, newRacStream);
        // from exported pixel in validation product
        final double sourceRadiance = 119.61037;
        final double targetRadiance = 120.02557;
        assertEquals(targetRadiance, calibrationAlgorithm.calibrate(0, 759, sourceRadiance), 1.0e-4);
    }

}
