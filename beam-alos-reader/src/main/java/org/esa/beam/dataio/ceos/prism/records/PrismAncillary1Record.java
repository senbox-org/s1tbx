package org.esa.beam.dataio.ceos.prism.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.Ancillary1Record;

import java.io.IOException;

/*
 * $Id: PrismAncillary1Record.java,v 1.1 2006/09/13 09:12:34 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

/**
 * Created by Marco Peters.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class PrismAncillary1Record extends Ancillary1Record {

    private double[][][] _ccdCoeffs;

    public PrismAncillary1Record(final CeosFileReader reader) throws IOException,
                                                                     IllegalCeosFormatException {
        this(reader, -1);
    }

    public PrismAncillary1Record(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                          IllegalCeosFormatException {
        super(reader, startPos);
    }

    @Override
    public double[][] getTransformationCoefficientsFor(final int ccdIndex) throws IOException,
                                                                                  IllegalCeosFormatException {
        if (ccdIndex < 1 || ccdIndex > 8) {
            throw new IllegalArgumentException("The CCD number must be between 1 and 8");
        }

        if (_ccdCoeffs == null) {
            _ccdCoeffs = new double[8][4][10];    // 8 CCDs ; 4 transformations ; 10 coeffs
            getReader().seek(getAbsolutPosition(1964));
            for (int i = 0; i < _ccdCoeffs.length; i++) {
                final double[][] ccdCoeff = _ccdCoeffs[i];
                for (int j = 0; j < ccdCoeff.length; j++) {
                    final long[] longs = new long[_ccdCoeffs[0][0].length];
                    getReader().readB8(longs);
                    ccdCoeff[j] = CeosHelper.convertLongToDouble(longs);
                }
            }
        }

        return _ccdCoeffs[ccdIndex - 1];
    }

}
