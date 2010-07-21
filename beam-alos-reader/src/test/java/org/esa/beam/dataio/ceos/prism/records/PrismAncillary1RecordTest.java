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

package org.esa.beam.dataio.ceos.prism.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.Ancillary1Record;
import org.esa.beam.dataio.ceos.records.Ancillary1RecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public class PrismAncillary1RecordTest extends Ancillary1RecordTest {


    @Override
    protected Ancillary1Record createAncillary1Record(final CeosFileReader reader) throws IOException,
                                                                                          IllegalCeosFormatException {
        return new PrismAncillary1Record(reader);
    }

    @Override
    protected Ancillary1Record createAncillary1Record(final CeosFileReader reader, final int startPos) throws
                                                                                                       IOException,
                                                                                                       IllegalCeosFormatException {
        return new PrismAncillary1Record(reader, startPos);
    }

    @Override
    protected void writeCoefficients(final ImageOutputStream ios) throws IOException {
        // write 4 * 10 coefficients for CCD1
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for CCD2
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for CCD3
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for CCD4
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for CCD5
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for CCD6
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for CCD7
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

        // write 4 * 10 coefficients for CCD8
        writeIncrementingDoubles(123.4567, 10, ios);
        writeIncrementingDoubles(123.9876, 10, ios);
        writeIncrementingDoubles(123.1234, 10, ios);
        writeIncrementingDoubles(123.5678, 10, ios);

    }


    @Override
    protected void assertCoefficients(final Ancillary1Record record) throws IOException,
                                                                            IllegalCeosFormatException {
        final double[][] coeffsForCCD1 = record.getTransformationCoefficientsFor(1);
        final double[] latCoeffs_ccd1 = coeffsForCCD1[0];
        final double[] lonCoeffs_ccd1 = coeffsForCCD1[1];
        final double[] xCoeffs_ccd1 = coeffsForCCD1[2];
        final double[] yCoeffs_ccd1 = coeffsForCCD1[3];
        assertNotNull(latCoeffs_ccd1);
        assertEquals(10, latCoeffs_ccd1.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd1[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd1[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd1[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd1[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd1[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd1[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd1[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd1[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd1[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd1[9], 1e-6);
        assertNotNull(lonCoeffs_ccd1);
        assertEquals(10, lonCoeffs_ccd1.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd1[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd1[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd1[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd1[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd1[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd1[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd1[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd1[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd1[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd1[9], 1e-6);
        assertNotNull(xCoeffs_ccd1);
        assertEquals(10, xCoeffs_ccd1.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd1[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd1[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd1[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd1[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd1[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd1[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd1[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd1[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd1[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd1[9], 1e-6);
        assertNotNull(yCoeffs_ccd1);
        assertEquals(10, yCoeffs_ccd1.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd1[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd1[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd1[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd1[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd1[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd1[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd1[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd1[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd1[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd1[9], 1e-6);

        final double[][] coeffsForCCD2 = record.getTransformationCoefficientsFor(2);
        final double[] latCoeffs_ccd2 = coeffsForCCD2[0];
        final double[] lonCoeffs_ccd2 = coeffsForCCD2[1];
        final double[] xCoeffs_ccd2 = coeffsForCCD2[2];
        final double[] yCoeffs_ccd2 = coeffsForCCD2[3];
        assertNotNull(latCoeffs_ccd2);
        assertEquals(10, latCoeffs_ccd2.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd2[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd2[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd2[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd2[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd2[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd2[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd2[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd2[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd2[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd2[9], 1e-6);
        assertNotNull(lonCoeffs_ccd2);
        assertEquals(10, lonCoeffs_ccd2.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd2[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd2[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd2[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd2[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd2[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd2[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd2[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd2[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd2[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd2[9], 1e-6);
        assertNotNull(xCoeffs_ccd2);
        assertEquals(10, xCoeffs_ccd2.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd2[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd2[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd2[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd2[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd2[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd2[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd2[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd2[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd2[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd2[9], 1e-6);
        assertNotNull(yCoeffs_ccd2);
        assertEquals(10, yCoeffs_ccd2.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd2[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd2[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd2[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd2[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd2[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd2[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd2[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd2[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd2[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd2[9], 1e-6);

        final double[][] coeffsForCCD3 = record.getTransformationCoefficientsFor(3);
        final double[] latCoeffs_ccd3 = coeffsForCCD3[0];
        final double[] lonCoeffs_ccd3 = coeffsForCCD3[1];
        final double[] xCoeffs_ccd3 = coeffsForCCD3[2];
        final double[] yCoeffs_ccd3 = coeffsForCCD3[3];
        assertNotNull(latCoeffs_ccd3);
        assertEquals(10, latCoeffs_ccd3.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd3[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd3[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd3[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd3[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd3[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd3[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd3[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd3[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd3[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd3[9], 1e-6);
        assertNotNull(lonCoeffs_ccd3);
        assertEquals(10, lonCoeffs_ccd3.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd3[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd3[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd3[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd3[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd3[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd3[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd3[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd3[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd3[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd3[9], 1e-6);
        assertNotNull(xCoeffs_ccd3);
        assertEquals(10, xCoeffs_ccd3.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd3[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd3[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd3[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd3[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd3[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd3[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd3[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd3[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd3[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd3[9], 1e-6);
        assertNotNull(yCoeffs_ccd3);
        assertEquals(10, yCoeffs_ccd3.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd3[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd3[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd3[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd3[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd3[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd3[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd3[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd3[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd3[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd3[9], 1e-6);

        final double[][] coeffsForCCD4 = record.getTransformationCoefficientsFor(4);
        final double[] latCoeffs_ccd4 = coeffsForCCD4[0];
        final double[] lonCoeffs_ccd4 = coeffsForCCD4[1];
        final double[] xCoeffs_ccd4 = coeffsForCCD4[2];
        final double[] yCoeffs_ccd4 = coeffsForCCD4[3];
        assertNotNull(latCoeffs_ccd4);
        assertEquals(10, latCoeffs_ccd4.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd4[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd4[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd4[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd4[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd4[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd4[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd4[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd4[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd4[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd4[9], 1e-6);
        assertNotNull(lonCoeffs_ccd4);
        assertEquals(10, lonCoeffs_ccd4.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd4[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd4[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd4[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd4[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd4[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd4[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd4[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd4[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd4[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd4[9], 1e-6);
        assertNotNull(xCoeffs_ccd4);
        assertEquals(10, xCoeffs_ccd4.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd4[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd4[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd4[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd4[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd4[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd4[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd4[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd4[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd4[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd4[9], 1e-6);
        assertNotNull(yCoeffs_ccd4);
        assertEquals(10, yCoeffs_ccd4.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd4[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd4[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd4[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd4[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd4[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd4[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd4[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd4[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd4[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd4[9], 1e-6);

        final double[][] coeffsForCCD5 = record.getTransformationCoefficientsFor(5);
        final double[] latCoeffs_ccd5 = coeffsForCCD5[0];
        final double[] lonCoeffs_ccd5 = coeffsForCCD5[1];
        final double[] xCoeffs_ccd5 = coeffsForCCD5[2];
        final double[] yCoeffs_ccd5 = coeffsForCCD5[3];
        assertNotNull(latCoeffs_ccd5);
        assertEquals(10, latCoeffs_ccd5.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd5[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd5[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd5[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd5[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd5[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd5[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd5[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd5[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd5[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd5[9], 1e-6);
        assertNotNull(lonCoeffs_ccd5);
        assertEquals(10, lonCoeffs_ccd5.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd5[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd5[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd5[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd5[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd5[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd5[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd5[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd5[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd5[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd5[9], 1e-6);
        assertNotNull(xCoeffs_ccd5);
        assertEquals(10, xCoeffs_ccd5.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd5[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd5[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd5[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd5[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd5[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd5[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd5[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd5[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd5[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd5[9], 1e-6);
        assertNotNull(yCoeffs_ccd5);
        assertEquals(10, yCoeffs_ccd5.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd5[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd5[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd5[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd5[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd5[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd5[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd5[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd5[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd5[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd5[9], 1e-6);

        final double[][] coeffsForCCD6 = record.getTransformationCoefficientsFor(6);
        final double[] latCoeffs_ccd6 = coeffsForCCD6[0];
        final double[] lonCoeffs_ccd6 = coeffsForCCD6[1];
        final double[] xCoeffs_ccd6 = coeffsForCCD6[2];
        final double[] yCoeffs_ccd6 = coeffsForCCD6[3];
        assertNotNull(latCoeffs_ccd6);
        assertEquals(10, latCoeffs_ccd6.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd6[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd6[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd6[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd6[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd6[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd6[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd6[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd6[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd6[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd6[9], 1e-6);
        assertNotNull(lonCoeffs_ccd6);
        assertEquals(10, lonCoeffs_ccd6.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd6[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd6[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd6[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd6[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd6[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd6[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd6[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd6[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd6[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd6[9], 1e-6);
        assertNotNull(xCoeffs_ccd6);
        assertEquals(10, xCoeffs_ccd6.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd6[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd6[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd6[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd6[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd6[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd6[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd6[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd6[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd6[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd6[9], 1e-6);
        assertNotNull(yCoeffs_ccd6);
        assertEquals(10, yCoeffs_ccd6.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd6[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd6[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd6[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd6[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd6[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd6[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd6[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd6[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd6[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd6[9], 1e-6);

        final double[][] coeffsForCCD7 = record.getTransformationCoefficientsFor(7);
        final double[] latCoeffs_ccd7 = coeffsForCCD7[0];
        final double[] lonCoeffs_ccd7 = coeffsForCCD7[1];
        final double[] xCoeffs_ccd7 = coeffsForCCD7[2];
        final double[] yCoeffs_ccd7 = coeffsForCCD7[3];
        assertNotNull(latCoeffs_ccd7);
        assertEquals(10, latCoeffs_ccd7.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd7[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd7[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd7[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd7[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd7[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd7[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd7[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd7[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd7[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd7[9], 1e-6);
        assertNotNull(lonCoeffs_ccd7);
        assertEquals(10, lonCoeffs_ccd7.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd7[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd7[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd7[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd7[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd7[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd7[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd7[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd7[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd7[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd7[9], 1e-6);
        assertNotNull(xCoeffs_ccd7);
        assertEquals(10, xCoeffs_ccd7.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd7[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd7[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd7[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd7[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd7[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd7[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd7[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd7[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd7[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd7[9], 1e-6);
        assertNotNull(yCoeffs_ccd7);
        assertEquals(10, yCoeffs_ccd7.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd7[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd7[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd7[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd7[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd7[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd7[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd7[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd7[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd7[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd7[9], 1e-6);

        final double[][] coeffsForCCD8 = record.getTransformationCoefficientsFor(8);
        final double[] latCoeffs_ccd8 = coeffsForCCD8[0];
        final double[] lonCoeffs_ccd8 = coeffsForCCD8[1];
        final double[] xCoeffs_ccd8 = coeffsForCCD8[2];
        final double[] yCoeffs_ccd8 = coeffsForCCD8[3];
        assertNotNull(latCoeffs_ccd8);
        assertEquals(10, latCoeffs_ccd8.length);
        assertEquals(123.4567 + 0, latCoeffs_ccd8[0], 1e-6);
        assertEquals(123.4567 + 1, latCoeffs_ccd8[1], 1e-6);
        assertEquals(123.4567 + 2, latCoeffs_ccd8[2], 1e-6);
        assertEquals(123.4567 + 3, latCoeffs_ccd8[3], 1e-6);
        assertEquals(123.4567 + 4, latCoeffs_ccd8[4], 1e-6);
        assertEquals(123.4567 + 5, latCoeffs_ccd8[5], 1e-6);
        assertEquals(123.4567 + 6, latCoeffs_ccd8[6], 1e-6);
        assertEquals(123.4567 + 7, latCoeffs_ccd8[7], 1e-6);
        assertEquals(123.4567 + 8, latCoeffs_ccd8[8], 1e-6);
        assertEquals(123.4567 + 9, latCoeffs_ccd8[9], 1e-6);
        assertNotNull(lonCoeffs_ccd8);
        assertEquals(10, lonCoeffs_ccd8.length);
        assertEquals(123.9876 + 0, lonCoeffs_ccd8[0], 1e-6);
        assertEquals(123.9876 + 1, lonCoeffs_ccd8[1], 1e-6);
        assertEquals(123.9876 + 2, lonCoeffs_ccd8[2], 1e-6);
        assertEquals(123.9876 + 3, lonCoeffs_ccd8[3], 1e-6);
        assertEquals(123.9876 + 4, lonCoeffs_ccd8[4], 1e-6);
        assertEquals(123.9876 + 5, lonCoeffs_ccd8[5], 1e-6);
        assertEquals(123.9876 + 6, lonCoeffs_ccd8[6], 1e-6);
        assertEquals(123.9876 + 7, lonCoeffs_ccd8[7], 1e-6);
        assertEquals(123.9876 + 8, lonCoeffs_ccd8[8], 1e-6);
        assertEquals(123.9876 + 9, lonCoeffs_ccd8[9], 1e-6);
        assertNotNull(xCoeffs_ccd8);
        assertEquals(10, xCoeffs_ccd8.length);
        assertEquals(123.1234 + 0, xCoeffs_ccd8[0], 1e-6);
        assertEquals(123.1234 + 1, xCoeffs_ccd8[1], 1e-6);
        assertEquals(123.1234 + 2, xCoeffs_ccd8[2], 1e-6);
        assertEquals(123.1234 + 3, xCoeffs_ccd8[3], 1e-6);
        assertEquals(123.1234 + 4, xCoeffs_ccd8[4], 1e-6);
        assertEquals(123.1234 + 5, xCoeffs_ccd8[5], 1e-6);
        assertEquals(123.1234 + 6, xCoeffs_ccd8[6], 1e-6);
        assertEquals(123.1234 + 7, xCoeffs_ccd8[7], 1e-6);
        assertEquals(123.1234 + 8, xCoeffs_ccd8[8], 1e-6);
        assertEquals(123.1234 + 9, xCoeffs_ccd8[9], 1e-6);
        assertNotNull(yCoeffs_ccd8);
        assertEquals(10, yCoeffs_ccd8.length);
        assertEquals(123.5678 + 0, yCoeffs_ccd8[0], 1e-6);
        assertEquals(123.5678 + 1, yCoeffs_ccd8[1], 1e-6);
        assertEquals(123.5678 + 2, yCoeffs_ccd8[2], 1e-6);
        assertEquals(123.5678 + 3, yCoeffs_ccd8[3], 1e-6);
        assertEquals(123.5678 + 4, yCoeffs_ccd8[4], 1e-6);
        assertEquals(123.5678 + 5, yCoeffs_ccd8[5], 1e-6);
        assertEquals(123.5678 + 6, yCoeffs_ccd8[6], 1e-6);
        assertEquals(123.5678 + 7, yCoeffs_ccd8[7], 1e-6);
        assertEquals(123.5678 + 8, yCoeffs_ccd8[8], 1e-6);
        assertEquals(123.5678 + 9, yCoeffs_ccd8[9], 1e-6);
    }


}