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
import org.esa.beam.dataio.ceos.CeosTestHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.Ancillary2Record;
import org.esa.beam.dataio.ceos.records.Ancillary2RecordTest;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.IOException;

public class Avnir2Ancillary2RecordTest extends Ancillary2RecordTest {

    @Override
    protected Ancillary2Record createAncillary2Record(final CeosFileReader reader) throws IOException,
                                                                                          IllegalCeosFormatException {
        return new Avnir2Ancillary2Record(reader);
    }

    @Override
    protected Ancillary2Record createAncillary2Record(final CeosFileReader reader, final int startPos) throws
                                                                                                       IOException,
                                                                                                       IllegalCeosFormatException {
        return new Avnir2Ancillary2Record(reader, startPos);
    }

    @Override
    protected void writeSpecificRecordData(final MemoryCacheImageOutputStream ios) throws IOException {
        ios.seek(_prefix.length() + 24);
        ios.writeBytes("12345");    // exposure coefficient band1   // I5
        ios.writeBytes("56789");    // exposure coefficient band2   // I5
        ios.writeBytes("12345");    // exposure coefficient band3   // I5
        ios.writeBytes("56789");    // exposure coefficient band4   // I5
        ios.seek(_prefix.length() + 78);
        ios.writeBytes("2345.432"); // detector temp. band1 // F8.3
        ios.writeBytes("5678.123"); // detector temp. band2 // F8.3
        ios.writeBytes("9876.321"); // detector temp. band3 // F8.3
        ios.writeBytes("1234.567"); // detector temp. band4 // F8.3
        ios.writeBytes("2345.432"); // detector assembly temp. band1 // F8.3
        ios.writeBytes("5678.123"); // detector assembly temp. band2 // F8.3
        ios.writeBytes("9876.321"); // detector assembly temp. band3 // F8.3
        ios.writeBytes("1234.567"); // detector assembly temp. band4 // F8.3

        ios.writeBytes("1111.999"); // signal processing unit temp.  // F8.3
        CeosTestHelper.writeBlanks(ios, 2552);

        ios.writeBytes("234.5432"); // absoluteCalibrationGain band1    // F8.4
        ios.writeBytes("543.2345"); // absoluteCalibrationOffset band1  // F8.4
        ios.writeBytes("987.6321"); // absoluteCalibrationGain band2    // F8.4
        ios.writeBytes("123.4567"); // absoluteCalibrationOffset band2  // F8.4
        ios.writeBytes("234.5432"); // absoluteCalibrationGain band3    // F8.4
        ios.writeBytes("543.2345"); // absoluteCalibrationOffset band3  // F8.4
        ios.writeBytes("987.6321"); // absoluteCalibrationGain band4    // F8.4
        ios.writeBytes("123.4567"); // absoluteCalibrationOffset band4  // F8.4
        CeosTestHelper.writeBlanks(ios, 1914);
    }

    @Override
    protected void assertSpecificRecordData(final Ancillary2Record record) {
        final Avnir2Ancillary2Record avnir2Record = (Avnir2Ancillary2Record) record;

        assertEquals(12345, avnir2Record.getExposureCoefficient(1));
        assertEquals(56789, avnir2Record.getExposureCoefficient(2));
        assertEquals(12345, avnir2Record.getExposureCoefficient(3));
        assertEquals(56789, avnir2Record.getExposureCoefficient(4));

        assertEquals(2345.432, avnir2Record.getDetectorTemperature(1), 1e-6);
        assertEquals(5678.123, avnir2Record.getDetectorTemperature(2), 1e-6);
        assertEquals(9876.321, avnir2Record.getDetectorTemperature(3), 1e-6);
        assertEquals(1234.567, avnir2Record.getDetectorTemperature(4), 1e-6);
        assertEquals(2345.432, avnir2Record.getDetectorAssemblyTemperature(1), 1e-6);
        assertEquals(5678.123, avnir2Record.getDetectorAssemblyTemperature(2), 1e-6);
        assertEquals(9876.321, avnir2Record.getDetectorAssemblyTemperature(3), 1e-6);
        assertEquals(1234.567, avnir2Record.getDetectorAssemblyTemperature(4), 1e-6);

        assertEquals(1111.999, avnir2Record.getSignalProcessingUnitTemperature(), 1e-6);

        assertEquals(234.5432, avnir2Record.getAbsoluteCalibrationGain(1), 1e-6);
        assertEquals(543.2345, avnir2Record.getAbsoluteCalibrationOffset(1), 1e-6);
        assertEquals(987.6321, avnir2Record.getAbsoluteCalibrationGain(2), 1e-6);
        assertEquals(123.4567, avnir2Record.getAbsoluteCalibrationOffset(2), 1e-6);
        assertEquals(234.5432, avnir2Record.getAbsoluteCalibrationGain(3), 1e-6);
        assertEquals(543.2345, avnir2Record.getAbsoluteCalibrationOffset(3), 1e-6);
        assertEquals(987.6321, avnir2Record.getAbsoluteCalibrationGain(4), 1e-6);
        assertEquals(123.4567, avnir2Record.getAbsoluteCalibrationOffset(4), 1e-6);

    }
}
