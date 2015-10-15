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

package org.esa.snap.core.util.math;

import Jama.Matrix;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Helmut Schiller, GKSS
 * @since 4.1
 */
public class LinearSpectralUnmixingTest extends TestCase {

    private Matrix endmembers;
    private Matrix spectra;

    public void testUnconstrainedUnmixing() throws IOException {
        SpectralUnmixing mlm = new UnconstrainedLSU(endmembers.getArray());

        Matrix abundUnconstrBeam = new Matrix(mlm.unmix(spectra.getArray()));

        Matrix abundUnconstrEnvi = Matrix.read(getResourceReader("abundances-unconstr-envi.csv"));
        Matrix abundUnconstrExpected = Matrix.read(getResourceReader("abundances-unconstr-expected.csv"));
        assertEquals("Difference of abundances (BEAM minus ENVI, unconstrained)",
                0.0,
                maxAbs(abundUnconstrBeam.minus(abundUnconstrEnvi)),
                1e-4);
        assertEquals("Difference of abundances (BEAM minus EXPECTED, unconstrained)",
                0.0,
                maxAbs(abundUnconstrBeam.minus(abundUnconstrExpected)),
                1e-7);
    }

    public void testConstrainedUnmixing() throws IOException {
        SpectralUnmixing mlmC = new ConstrainedLSU(endmembers.getArray());
        Matrix abundConstrBeam = new Matrix(mlmC.unmix(spectra.getArray()));
        
        Matrix abundConstrEnvi = Matrix.read(getResourceReader("abundances-constr-envi.csv"));
        Matrix abundConstrExpected = Matrix.read(getResourceReader("abundances-constr-expected.csv"));
        assertEquals("Difference of abundances (BEAM minus ENVI, constrained)",
                0.0,
                maxAbs(abundConstrBeam.minus(abundConstrEnvi)),
                1e-2);
        assertEquals("Difference of abundances (BEAM minus EXPECTED, constrained)",
                0.0,
                maxAbs(abundConstrBeam.minus(abundConstrExpected)),
                1e-7);

        assertEquals("Sum of abundances must be 1 (constrained)",
                0.0,
                maxAbsDeltaRowSumFromOne(abundConstrBeam),
                1e-15);
    }

    private static double maxAbs(Matrix matrix) {
        int nrows = matrix.getRowDimension();
        int ncols = matrix.getColumnDimension();
        double[][] array = matrix.getArray();
        double max = 0.;
        for (int ir = 0; ir < nrows; ir++) {
            for (int ic = 0; ic < ncols; ic++) {
                if (max < Math.abs(array[ir][ic])) {
                    max = Math.abs(array[ir][ic]);
                }
            }
        }
        return max;
    }

    private static double maxAbsDeltaRowSumFromOne(Matrix matrix) {
        int nrow = matrix.getRowDimension();
        int ncol = matrix.getColumnDimension();
        double[][] array = matrix.getArray();
        double max = -1.0;
        for (int ic = 0; ic < ncol; ic++) {
            double sum = 0.0;
            for (int ir = 0; ir < nrow; ir++) {
                sum += array[ir][ic];
            }
            double ad = Math.abs(sum - 1.);
            if (ad > max) {
                max = ad;
            }
        }
        return max;
    }

    private static BufferedReader getResourceReader(String name) {
        String resourceName = "lsu/" + name;
        InputStream stream = LinearSpectralUnmixingTest.class.getResourceAsStream(resourceName);
        if (stream == null) {
            fail("resource not found: " + resourceName);
        }
        return new BufferedReader(new InputStreamReader(stream));
    }

    @Override
    protected void setUp() throws Exception {
        endmembers = Matrix.read(getResourceReader("endmember-spectra.csv"));
        spectra = Matrix.read(getResourceReader("pixel-spectra.csv"));
    }
}
