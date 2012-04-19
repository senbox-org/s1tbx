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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.modis.hdf.IHDFAdapterForMocking;
import org.esa.beam.dataio.modis.hdf.lib.HDFTestCase;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.Range;

import java.io.IOException;

public class ModisUint16BandReaderTest extends HDFTestCase {

    public void testRead() throws IOException {
        setHdfMock(new IHDFAdapterForMocking() {
            short value = -7;

            @Override
            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws IOException {
                final short[] shorts = (short[]) buffer;
                for (int i = 0; i < shorts.length; i++) {
                    shorts[i] = value;
                    value += 2;
                }
            }
        });

        final ProductData buffer = new ProductData.UShort(12);
        final ModisUint16BandReader reader = new ModisUint16BandReader(3, 2, false);
        final int fill = 999;
        reader.setFillValue(fill);
        reader.setValidRange(new Range(4, Short.MAX_VALUE * 2 - 3));

        // Method under test
        reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);

        final int[] expected = {65529, 65531, fill, fill, fill, fill, 5, 7, 9, 11, 13, 15};
        for (int i = 0; i < expected.length; i++) {
            assertEquals("false at index: " + i + "  ", expected[i], buffer.getElemIntAt(i));
        }
    }

    public void testHDFException() throws ProductIOException {
        setHdfMock(new IHDFAdapterForMocking() {

            @Override
            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer) throws IOException {
                throw new IOException("TestMessage");
            }
        });

        final int sdsId = 3;
        final ProductData buffer = new ProductData.UShort(12);
        final ModisUint16BandReader reader = new ModisUint16BandReader(sdsId, 2, false);

        try {
            // Method under test
            reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);
            fail();
        } catch (IOException e) {
            assertEquals("TestMessage", e.getMessage());
        }
    }
}
