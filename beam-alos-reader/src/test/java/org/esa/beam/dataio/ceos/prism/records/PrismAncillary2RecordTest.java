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
import org.esa.beam.dataio.ceos.CeosTestHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.Ancillary2Record;
import org.esa.beam.dataio.ceos.records.Ancillary2RecordTest;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.IOException;

public class PrismAncillary2RecordTest extends Ancillary2RecordTest {

    @Override
    protected Ancillary2Record createAncillary2Record(final CeosFileReader reader) throws IOException,
                                                                                          IllegalCeosFormatException {
        return new PrismAncillary2Record(reader);
    }

    @Override
    protected Ancillary2Record createAncillary2Record(final CeosFileReader reader, final int startPos) throws
                                                                                                       IOException,
                                                                                                       IllegalCeosFormatException {
        return new PrismAncillary2Record(reader, startPos);
    }

    @Override
    protected void writeSpecificRecordData(final MemoryCacheImageOutputStream ios) throws IOException {
        ios.writeBytes("C"); // compressionMode // A1
        CeosTestHelper.writeBlanks(ios, 15);
        ios.writeBytes("2345.432"); // ccdTemperature // F8.3
        ios.writeBytes("5432.345"); // signalProcessingSectionTemperature // F8.3
        CeosTestHelper.writeBlanks(ios, 2608);
        ios.writeBytes("234.5432"); // absoluteCalibrationGain // F8.4
        ios.writeBytes("543.2345"); // absoluteCalibrationOffset // F8.4
        CeosTestHelper.writeBlanks(ios, 1962);
    }

    @Override
    protected void assertSpecificRecordData(final Ancillary2Record record) {
        final PrismAncillary2Record prismRecord = (PrismAncillary2Record) record;
        assertEquals("C", prismRecord.getCompressionMode());
        assertEquals(2345.432, prismRecord.getCcdTemperature(), 1e-6);
        assertEquals(5432.345, prismRecord.getSignalProcessingSectionTemperature(), 1e-6);
        assertEquals(234.5432, prismRecord.getAbsoluteCalibrationGain(), 1e-6);
        assertEquals(543.2345, prismRecord.getAbsoluteCalibrationOffset(), 1e-6);

    }
}
