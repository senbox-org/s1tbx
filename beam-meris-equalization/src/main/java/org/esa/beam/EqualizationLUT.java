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

package org.esa.beam;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

class EqualizationLUT {

    private static final String COEF_FILE_PATTERN = "Equalization_coefficient_band_%02d_reprocessing_r%d_%s.txt";
    private static final String FR = "FR";
    private static final String RR = "RR";
    private static final int BAND_COUNT = 15;
    private Map<Integer, Map<Integer, double[]>> bandMap;


    EqualizationLUT(int reprocessingVersion, boolean isFullResolution) throws IOException {
        bandMap = new HashMap<Integer, Map<Integer, double[]>>(BAND_COUNT);
        for (int i = 1; i <= BAND_COUNT; i++) {
            final HashMap<Integer, double[]> coefMap = new HashMap<Integer, double[]>();
            final InputStream stream = getClass().getResourceAsStream(
                    String.format(COEF_FILE_PATTERN, i, reprocessingVersion, isFullResolution ? FR : RR));
            final CsvReader reader = new CsvReader(new InputStreamReader(stream), new char[]{' '});
            try {
                double[] coefs = reader.readDoubleRecord();
                while (coefs != null) {
                    coefMap.put(reader.getLineNumber() - 1, coefs);
                    coefs = reader.readDoubleRecord();
                }
            } finally {
                reader.close();
            }
            bandMap.put(i, coefMap);
        }
    }

    // Returns 3 coefficients from the coefficient Look-Up-Table for the given band index at
    // the given detectorIndex.
    // bandIndex and detectorIndex are zero-based
    double[] getCoefficients(int bandIndex, int detectorIndex) {
        final Map<Integer, double[]> coefMap = bandMap.get(bandIndex + 1);
        return coefMap.get(detectorIndex);
    }
}
