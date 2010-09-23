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

package org.esa.beam.preprocessor.equalization;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EqualizationLUT {

    private List<Map<Integer, double[]>> coefficientsMapList;

    EqualizationLUT(Reader[] bandCoefficientReaders) throws IOException {
        coefficientsMapList = new ArrayList<Map<Integer, double[]>>(bandCoefficientReaders.length);
        for (Reader bandCoefficientReader : bandCoefficientReaders) {
            final HashMap<Integer, double[]> coefMap = new HashMap<Integer, double[]>();
            final CsvReader csvReader = new CsvReader(bandCoefficientReader, new char[]{' '});
            try {
                double[] coefs = csvReader.readDoubleRecord();
                while (coefs != null) {
                    coefMap.put(csvReader.getLineNumber() - 1, coefs);
                    coefs = csvReader.readDoubleRecord();
                }
            } finally {
                csvReader.close();
            }
            coefficientsMapList.add(coefMap);

        }
    }

    // Returns 3 coefficients from the coefficient Look-Up-Table for the given band index at
    // the given detectorIndex.
    // bandIndex and detectorIndex are zero-based
    double[] getCoefficients(int bandIndex, int detectorIndex) {
        final Map<Integer, double[]> coefMap = coefficientsMapList.get(bandIndex);
        return coefMap.get(detectorIndex);
    }
}
