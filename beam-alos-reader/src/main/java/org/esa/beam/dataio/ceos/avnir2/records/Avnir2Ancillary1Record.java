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

package org.esa.beam.dataio.ceos.avnir2.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.Ancillary1Record;

import java.io.IOException;

public class Avnir2Ancillary1Record extends Ancillary1Record {

    private double[][][] _bandCoeffs;

    public Avnir2Ancillary1Record(final CeosFileReader reader) throws IOException,
                                                                      IllegalCeosFormatException {
        this(reader, -1);
    }

    public Avnir2Ancillary1Record(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                           IllegalCeosFormatException {
        super(reader, startPos);
    }

    @Override
    public double[][] getTransformationCoefficientsFor(final int bandIndex) throws IOException,
                                                                                   IllegalCeosFormatException {
        if (bandIndex < 1 || bandIndex > 4) {
            throw new IllegalArgumentException("The band index must be between 1 and 4");
        }

        if (_bandCoeffs == null) {
            _bandCoeffs = new double[4][4][10];   // 4 bands ; 4 transformations ; 10 coeffs
            final long[] tempLongs = new long[_bandCoeffs[0][0].length];
            getReader().seek(getAbsolutPosition(1964));
            for (int i = 0; i < _bandCoeffs.length; i++) {
                final double[][] bandCoeff = _bandCoeffs[i];
                for (int j = 0; j < bandCoeff.length; j++) {
                    getReader().readB8(tempLongs);
                    bandCoeff[j] = CeosHelper.convertLongToDouble(tempLongs);
                }
            }
        }

        return _bandCoeffs[bandIndex - 1];
    }
}
