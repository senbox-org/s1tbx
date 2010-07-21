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
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.Ancillary1Record;
import org.esa.beam.dataio.ceos.records.Ancillary1RecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public class Avnir2Ancillary1RecordTest extends Ancillary1RecordTest {

    @Override
    protected Ancillary1Record createAncillary1Record(final CeosFileReader reader) throws IOException,
                                                                                          IllegalCeosFormatException {
        return new Avnir2Ancillary1Record(reader);
    }

    @Override
    protected Ancillary1Record createAncillary1Record(final CeosFileReader reader, final int startPos) throws
                                                                                                       IOException,
                                                                                                       IllegalCeosFormatException {
        return new Avnir2Ancillary1Record(reader, startPos);
    }

    @Override
    protected void writeCoefficients(final ImageOutputStream ios) throws IOException {
        // write 4 * 10 coefficients for band1
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for band2
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for band3
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for band4
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);


    }

    @Override
    protected void assertCoefficients(final Ancillary1Record record) throws IOException,
                                                                            IllegalCeosFormatException {
        final double[][] coeffsForBand1 = record.getTransformationCoefficientsFor(1);
        final double[] latCoeffsBand1 = coeffsForBand1[0];
        final double[] lonCoeffsBand1 = coeffsForBand1[1];
        final double[] xCoeffsBand1 = coeffsForBand1[2];
        final double[] yCoeffsBand1 = coeffsForBand1[3];
        assertNotNull(latCoeffsBand1);
        assertEquals(10, latCoeffsBand1.length);
        assertEquals(123.4567 + 0, latCoeffsBand1[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffsBand1[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffsBand1[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffsBand1[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffsBand1[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffsBand1[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffsBand1[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffsBand1[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffsBand1[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffsBand1[9], 1e-6);
        assertNotNull(lonCoeffsBand1);
        assertEquals(10, lonCoeffsBand1.length);
        assertEquals(123.9876 + 0, lonCoeffsBand1[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffsBand1[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffsBand1[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffsBand1[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffsBand1[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffsBand1[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffsBand1[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffsBand1[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffsBand1[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffsBand1[9], 1e-6);
        assertNotNull(xCoeffsBand1);
        assertEquals(10, xCoeffsBand1.length);
        assertEquals(123.1234 + 0, xCoeffsBand1[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffsBand1[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffsBand1[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffsBand1[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffsBand1[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffsBand1[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffsBand1[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffsBand1[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffsBand1[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffsBand1[9], 1e-6);
        assertNotNull(yCoeffsBand1);
        assertEquals(10, yCoeffsBand1.length);
        assertEquals(123.5678 + 0, yCoeffsBand1[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffsBand1[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffsBand1[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffsBand1[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffsBand1[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffsBand1[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffsBand1[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffsBand1[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffsBand1[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffsBand1[9], 1e-6);

        final double[][] coeffsForBand2 = record.getTransformationCoefficientsFor(2);
        final double[] latCoeffsBand2 = coeffsForBand2[0];
        final double[] lonCoeffsBand2 = coeffsForBand2[1];
        final double[] xCoeffsBand2 = coeffsForBand2[2];
        final double[] yCoeffsBand2 = coeffsForBand2[3];
        assertNotNull(latCoeffsBand2);
        assertEquals(10, latCoeffsBand2.length);
        assertEquals(123.4567 + 0, latCoeffsBand2[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffsBand2[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffsBand2[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffsBand2[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffsBand2[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffsBand2[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffsBand2[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffsBand2[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffsBand2[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffsBand2[9], 1e-6);
        assertNotNull(lonCoeffsBand2);
        assertEquals(10, lonCoeffsBand2.length);
        assertEquals(123.9876 + 0, lonCoeffsBand2[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffsBand2[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffsBand2[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffsBand2[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffsBand2[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffsBand2[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffsBand2[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffsBand2[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffsBand2[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffsBand2[9], 1e-6);
        assertNotNull(xCoeffsBand2);
        assertEquals(10, xCoeffsBand2.length);
        assertEquals(123.1234 + 0, xCoeffsBand2[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffsBand2[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffsBand2[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffsBand2[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffsBand2[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffsBand2[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffsBand2[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffsBand2[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffsBand2[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffsBand2[9], 1e-6);
        assertNotNull(yCoeffsBand2);
        assertEquals(10, yCoeffsBand2.length);
        assertEquals(123.5678 + 0, yCoeffsBand2[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffsBand2[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffsBand2[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffsBand2[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffsBand2[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffsBand2[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffsBand2[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffsBand2[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffsBand2[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffsBand2[9], 1e-6);

        final double[][] coeffsForBand3 = record.getTransformationCoefficientsFor(3);
        final double[] latCoeffsBand3 = coeffsForBand3[0];
        final double[] lonCoeffsBand3 = coeffsForBand3[1];
        final double[] xCoeffsBand3 = coeffsForBand3[2];
        final double[] yCoeffsBand3 = coeffsForBand3[3];
        assertNotNull(latCoeffsBand3);
        assertEquals(10, latCoeffsBand3.length);
        assertEquals(123.4567 + 0, latCoeffsBand3[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffsBand3[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffsBand3[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffsBand3[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffsBand3[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffsBand3[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffsBand3[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffsBand3[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffsBand3[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffsBand3[9], 1e-6);
        assertNotNull(lonCoeffsBand3);
        assertEquals(10, lonCoeffsBand3.length);
        assertEquals(123.9876 + 0, lonCoeffsBand3[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffsBand3[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffsBand3[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffsBand3[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffsBand3[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffsBand3[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffsBand3[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffsBand3[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffsBand3[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffsBand3[9], 1e-6);
        assertNotNull(xCoeffsBand3);
        assertEquals(10, xCoeffsBand3.length);
        assertEquals(123.1234 + 0, xCoeffsBand3[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffsBand3[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffsBand3[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffsBand3[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffsBand3[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffsBand3[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffsBand3[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffsBand3[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffsBand3[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffsBand3[9], 1e-6);
        assertNotNull(yCoeffsBand3);
        assertEquals(10, yCoeffsBand3.length);
        assertEquals(123.5678 + 0, yCoeffsBand3[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffsBand3[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffsBand3[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffsBand3[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffsBand3[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffsBand3[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffsBand3[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffsBand3[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffsBand3[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffsBand3[9], 1e-6);

        final double[][] coeffsForBand4 = record.getTransformationCoefficientsFor(4);
        final double[] latCoeffsBand4 = coeffsForBand4[0];
        final double[] lonCoeffsBand4 = coeffsForBand4[1];
        final double[] xCoeffsBand4 = coeffsForBand4[2];
        final double[] yCoeffsBand4 = coeffsForBand4[3];
        assertNotNull(latCoeffsBand4);
        assertEquals(10, latCoeffsBand4.length);
        assertEquals(123.4567 + 0, latCoeffsBand4[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffsBand4[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffsBand4[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffsBand4[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffsBand4[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffsBand4[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffsBand4[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffsBand4[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffsBand4[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffsBand4[9], 1e-6);
        assertNotNull(lonCoeffsBand4);
        assertEquals(10, lonCoeffsBand4.length);
        assertEquals(123.9876 + 0, lonCoeffsBand4[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffsBand4[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffsBand4[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffsBand4[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffsBand4[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffsBand4[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffsBand4[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffsBand4[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffsBand4[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffsBand4[9], 1e-6);
        assertNotNull(xCoeffsBand4);
        assertEquals(10, xCoeffsBand4.length);
        assertEquals(123.1234 + 0, xCoeffsBand4[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffsBand4[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffsBand4[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffsBand4[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffsBand4[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffsBand4[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffsBand4[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffsBand4[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffsBand4[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffsBand4[9], 1e-6);
        assertNotNull(yCoeffsBand4);
        assertEquals(10, yCoeffsBand4.length);
        assertEquals(123.5678 + 0, yCoeffsBand4[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffsBand4[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffsBand4[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffsBand4[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffsBand4[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffsBand4[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffsBand4[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffsBand4[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffsBand4[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffsBand4[9], 1e-6);
    }

}