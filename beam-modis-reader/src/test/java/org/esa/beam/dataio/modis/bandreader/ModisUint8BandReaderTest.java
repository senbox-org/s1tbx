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

package org.esa.beam.dataio.modis.bandreader;

import org.esa.beam.dataio.modis.hdf.lib.HDFTestCase;

public class ModisUint8BandReaderTest extends HDFTestCase {

    public void testDummy() {
        // to prevent failures due to empty testclass
    }

    // @todo tb/** rewrite for NetCDF
//    public void testRead() throws IOException, HDFException, InvalidRangeException {
//        setHdfMock(new IHDFAdapterForMocking() {
//            byte value = -7;
//
//            @Override
//            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer)
//                    throws HDFException {
//                final byte[] bytes = (byte[]) buffer;
//                for (int i = 0; i < bytes.length; i++) {
//                    bytes[i] = value;
//                    value += 2;
//                }
//            }
//        });
//
//        final ProductData buffer = new ProductData.UByte(12);
//        final ModisUint8BandReader reader = new ModisUint8BandReader(3, 2, false);
//        reader.setFillValue(0);
//        reader.setValidRange(new Range(4, Byte.MAX_VALUE * 2 - 3));
//
//        // Method under test
//        reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);
//
//        final int[] expected = {249, 251, 0, 0, 0, 0, 5, 7, 9, 11, 13, 15};
//        for (int i = 0; i < expected.length; i++) {
//            assertEquals("false at index: " + i + "  ", expected[i], buffer.getElemIntAt(i));
//        }
//    }

    // @todo tb/** rewrite for NetCDF
//    public void testHDFException() throws ProductIOException, HDFException {
//        setHdfMock(new IHDFAdapterForMocking() {
//            @Override
//            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer)
//                    throws HDFException {
//                throw new HDFException("TestMessage");
//            }
//        });
//
//        final int sdsId = 3;
//        final ProductData buffer = new ProductData.UByte(12);
//        final ModisUint8BandReader reader = new ModisUint8BandReader(sdsId, 2, false);
//
//        try {
//            // Method under test
//            reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);
//            fail();
//        } catch (HDFException e) {
//            assertEquals("TestMessage", e.getMessage());
//        } catch (Exception e) {
//            fail();
//        }
//    }
}