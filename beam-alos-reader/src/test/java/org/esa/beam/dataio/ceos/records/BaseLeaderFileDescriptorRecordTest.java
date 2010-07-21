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

package org.esa.beam.dataio.ceos.records;

import junit.framework.TestCase;
import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class BaseLeaderFileDescriptorRecordTest extends TestCase {

    private ImageOutputStream _ios;
    private String _prefix;
    private CeosFileReader _reader;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "fdkjglsdkfhierr.m b9b0970w34";
        _ios.writeBytes(_prefix);
        writeRecordData(_ios);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix
        _reader = new CeosFileReader(_ios);
    }

    public void testInit_SimpleConstructor() throws IOException,
                                                    IllegalCeosFormatException {
        _reader.seek(_prefix.length());

        final CeosFileReader reader = _reader;
        final BaseLeaderFileDescriptorRecord record = createLeaderFDR(reader);

        assertRecord(record);
    }


    public void testInit() throws IOException,
                                  IllegalCeosFormatException {

        final CeosFileReader reader = _reader;
        final int startPos = _prefix.length();
        final BaseLeaderFileDescriptorRecord record = createLeaderFDR(reader, startPos);

        assertRecord(record);
    }

    private void assertRecord(final BaseLeaderFileDescriptorRecord record) throws IOException {
        CommonFileDescriptorRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + 4680, _ios.getStreamPosition());

        assertNotNull(record);
        assertEquals(1, record.getNumSceneHeaderRecords());
        assertEquals(4680, record.getSceneHeaderRecordLength());
        assertEquals(3, record.getNumAncillaryRecords());
        assertEquals(4680, record.getAncillaryRecordLength());

        assertEquals(1, record.getSceneIdFieldLocator());
        assertEquals(2, record.getSceneIdFieldDataStart());
        assertEquals(3, record.getSceneIdFieldNumBytes());
        assertEquals("A", record.getSceneIdFieldDataType());

        assertEquals(4, record.getRSPIdLocator());
        assertEquals(5, record.getRSPIdDataStart());
        assertEquals(6, record.getRSPIdNumBytes());
        assertEquals("B", record.getRSPIdDataType());

        assertEquals(7, record.getMissionIdLocator());
        assertEquals(8, record.getMissionIdDataStart());
        assertEquals(9, record.getMissionIdNumBytes());
        assertEquals("C", record.getMissionIdDataType());

        assertEquals(10, record.getSensorIdLocator());
        assertEquals(11, record.getSensorIdDataStart());
        assertEquals(12, record.getSensorIdNumBytes());
        assertEquals("D", record.getSensorIdDataType());

        assertEquals(13, record.getSceneCenterTimeLocator());
        assertEquals(14, record.getSceneCenterTimeDataStart());
        assertEquals(15, record.getSceneCenterTimeNumBytes());
        assertEquals("E", record.getSceneCenterTimeDataType());

        assertEquals(16, record.getSceneCenterLatLonLocator());
        assertEquals(17, record.getSceneCenterLatLonDataStart());
        assertEquals(18, record.getSceneCenterLatLonNumBytes());
        assertEquals("F", record.getSceneCenterLatLonDataType());

        assertEquals(19, record.getProcessingLevelLocator());
        assertEquals(20, record.getProcessingLevelDataStart());
        assertEquals(21, record.getProcessingLevelNumBytes());
        assertEquals("G", record.getProcessingLevelDataType());

        assertEquals(22, record.getImageFormatLocator());
        assertEquals(23, record.getImageFormatDataStart());
        assertEquals(24, record.getImageFormatNumBytes());
        assertEquals("H", record.getImageFormatDataType());

        assertEquals(25, record.getEffektiveBandLocator());
        assertEquals(26, record.getEffektiveBandDataStart());
        assertEquals(27, record.getEffektiveBandNumBytes());
        assertEquals("I", record.getEffektiveBandDataType());

        assertRecords17To21(record);
    }

    private void writeRecordData(final ImageOutputStream ios) throws IOException {
        CommonFileDescriptorRecordTest.writeRecordData(ios);

        ios.writeBytes("     1"); // numSceneHeaderRecords = "bbbbb1" // I6
        ios.writeBytes("  4680"); // sceneHeaderRecordLength = "bb4680" // I6
        ios.writeBytes("     3"); // numAncillaryRecords = "bbbbb3" // I6
        ios.writeBytes("  4680"); // ancillaryRecordLength = "bb4680" // I6
        ios.writeBytes("      "); // 6 x Blank // A6
        ios.writeBytes("      "); // 6 x Blank // A6

        ios.writeBytes("     1"); // sceneIdField LocatorRecordNumber // I6
        ios.writeBytes("     2"); // sceneIdField DataStartByteNumber // I6
        ios.writeBytes("  3");    // sceneIdField NumBytes // I3
        ios.writeBytes("A");      // ceneIdField DataType // A1

        ios.writeBytes("     4"); // RSP ID LocatorRecordNumber // I6
        ios.writeBytes("     5"); // RSP ID DataStartByteNumber // I6
        ios.writeBytes("  6");    // RSP ID NumBytes // I3
        ios.writeBytes("B");      // RSP ID DataType // A1

        ios.writeBytes("     7"); // Mission ID LocatorRecordNumber // I6
        ios.writeBytes("     8"); // Mission ID DataStartByteNumber // I6
        ios.writeBytes("  9");    // Mission ID NumBytes // I3
        ios.writeBytes("C");      // Mission ID DataType // A1

        ios.writeBytes("    10"); // Sensor ID LocatorRecordNumber // I6
        ios.writeBytes("    11"); // Sensor ID DataStartByteNumber // I6
        ios.writeBytes(" 12");    // Sensor ID NumBytes // I3
        ios.writeBytes("D");      // Sensor ID DataType // A1

        ios.writeBytes("    13"); // SceneCenterTime LocatorRecordNumber // I6
        ios.writeBytes("    14"); // SceneCenterTime DataStartByteNumber // I6
        ios.writeBytes(" 15");    // SceneCenterTime NumBytes // I3
        ios.writeBytes("E");      // SceneCenterTime DataType // A1

        ios.writeBytes("    16"); // SceneCenterLatLon LocatorRecordNumber // I6
        ios.writeBytes("    17"); // SceneCenterLatLon DataStartByteNumber // I6
        ios.writeBytes(" 18");    // SceneCenterLatLon NumBytes // I3
        ios.writeBytes("F");      // SceneCenterLatLon DataType // A1

        ios.writeBytes("    19"); // ProcessingLevel LocatorRecordNumber // I6
        ios.writeBytes("    20"); // ProcessingLevel DataStartByteNumber // I6
        ios.writeBytes(" 21");    // ProcessingLevel NumBytes // I3
        ios.writeBytes("G");      // ProcessingLevel DataType // A1

        ios.writeBytes("    22"); // ImageFormat LocatorRecordNumber // I6
        ios.writeBytes("    23"); // ImageFormat DataStartByteNumber // I6
        ios.writeBytes(" 24");    // ImageFormat NumBytes // I3
        ios.writeBytes("H");      // ImageFormat DataType // A1

        ios.writeBytes("    25"); // EffekiveBand LocatorRecordNumber // I6
        ios.writeBytes("    26"); // EffekiveBand DataStartByteNumber // I6
        ios.writeBytes(" 27");    // EffekiveBand NumBytes // I3
        ios.writeBytes("I");      // EffekiveBand DataType // A1

        writeFields17To21(ios);
    }

    protected abstract BaseLeaderFileDescriptorRecord createLeaderFDR(final CeosFileReader reader,
                                                                      final int startPos) throws IOException,
                                                                                                 IllegalCeosFormatException;

    protected abstract BaseLeaderFileDescriptorRecord createLeaderFDR(final CeosFileReader reader) throws IOException,
                                                                                                          IllegalCeosFormatException;

    protected abstract void writeFields17To21(ImageOutputStream ios) throws IOException;

    protected abstract void assertRecords17To21(BaseLeaderFileDescriptorRecord record);
}
