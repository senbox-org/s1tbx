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
import org.esa.beam.dataio.ceos.CeosTestHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Ancillary3RecordTest extends TestCase {

    private MemoryCacheImageOutputStream _ios;
    private String _prefix;
    private CeosFileReader _reader;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "Ancillery3RecordTest_prefix";
        _ios.writeBytes(_prefix);
        writeRecordData(_ios);
        _ios.writeBytes("Ancillery3RecordTest_suffix"); // suffix
        _reader = new CeosFileReader(_ios);
    }

    public void testInit_SimpleConstructor() throws IOException,
                                                    IllegalCeosFormatException {
        _reader.seek(_prefix.length());

        final Ancillary3Record record = new Ancillary3Record(_reader);

        assertRecord(record);
    }

    public void testInit() throws IOException,
                                  IllegalCeosFormatException {
        final Ancillary3Record record = new Ancillary3Record(_reader, _prefix.length());

        assertRecord(record);
    }

    private void writeRecordData(final MemoryCacheImageOutputStream ios) throws IOException {
        BaseRecordTest.writeRecordData(ios);

        ios.writeBytes("ljoiauiaerakb voieiruhnjdsnoirsd"); // orbitalElementsType // A32
        ios.writeBytes("    8001.1234567"); // orbitalElement1 // F16.7
        ios.writeBytes("    8002.1234567"); // orbitalElement2 // F16.7
        ios.writeBytes("    8003.1234567"); // orbitalElement3 // F16.7
        ios.writeBytes("    8004.1234567"); // orbitalElement4 // F16.7
        ios.writeBytes("    8005.1234567"); // orbitalElement5 // F16.7
        ios.writeBytes("    8006.1234567"); // orbitalElement6 // F16.7
        ios.writeBytes("4568"); // numDataPoints // I4
        ios.writeBytes("6578"); // firstPointYear // I4
        ios.writeBytes("1234"); // firstPointMonth // I4
        ios.writeBytes("4321"); // firstPointDay // I4
        ios.writeBytes("5643"); // firstPointTotalDays // I4
        ios.writeBytes(" 4.920000000000000E+03"); // firstPointTotalSeconds // F22.15
        ios.writeBytes(" 6.000000000000000E+01"); // intervalTimeBetweenPoints // F22.15
        // referenceCoordinateSystem // A64
        ios.writeBytes("alksdfjpawendbslkdslpovamreoifmclskdasdkfjaoeifmdlkdsfedlsfjiooe");
        CeosTestHelper.writeBlanks(ios, 22); // greenwichMeanHourAngle // F22.15
        ios.writeBytes("       2.0000000"); // positionalErrorFlightDirection // F16.7
        ios.writeBytes("       2.1111111"); // positionalErrorFlightVerticalDirection // F16.7
        ios.writeBytes("       2.2222222"); // positionalErrorRadiusDirection // F16.7
        ios.writeBytes("       2.3333333"); // velocityErrorFlightDirection // F16.7
        ios.writeBytes("       2.4444444"); // velocityErrorFlightVerticalDirection // F16.7
        ios.writeBytes("       2.5555555"); // velocityErrorRadiusDirection // F16.7

        for (int i = 0; i < 28; i++) {
            for (int j = 1; j < 7; j++) {
                ios.writeBytes(" " + j + "." + (i + 10) + "0000000000000E-03");
            }
        }

        CeosTestHelper.writeBlanks(ios, 18);
        ios.writeBytes("1"); // flagLeapSecond // I1
        CeosTestHelper.writeBlanks(ios, 579);
    }

    private void assertRecord(final Ancillary3Record record) throws IOException {
        BaseRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + 4680, _ios.getStreamPosition());

        assertEquals("ljoiauiaerakb voieiruhnjdsnoirsd", record.getOrbitalElementsType());
//        assertEquals(8001.1234567, record.getOrbitalElement1(), 1e-10);
//        assertEquals(8002.1234567, record.getOrbitalElement2(), 1e-10);
//        assertEquals(8003.1234567, record.getOrbitalElement3(), 1e-10);
//        assertEquals(8004.1234567, record.getOrbitalElement4(), 1e-10);
//        assertEquals(8005.1234567, record.getOrbitalElement5(), 1e-10);
//        assertEquals(8006.1234567, record.getOrbitalElement6(), 1e-10);
        assertEquals(4568, record.getNumDataPoints());
        assertEquals(6578, record.getFirstPointYear());
        assertEquals(1234, record.getFirstPointMonth());
        assertEquals(4321, record.getFirstPointDay());
        assertEquals(5643, record.getFirstPointTotalDays());
        assertEquals(4.92E+03, record.getFirstPointTotalSeconds(), 1e-10);
        assertEquals(6.0E+01, record.getIntervalTimeBetweenPoints(), 1e-10);
        assertEquals("alksdfjpawendbslkdslpovamreoifmclskdasdkfjaoeifmdlkdsfedlsfjiooe",
                     record.getReferenceCoordinateSystem());
        assertEquals(2.0000000, record.getPositionalErrorFlightDirection(), 1e-10);
        assertEquals(2.1111111, record.getPositionalErrorFlightVerticalDirection(), 1e-10);
        assertEquals(2.2222222, record.getPositionalErrorRadiusDirection(), 1e-10);
        assertEquals(2.3333333, record.getVelocityErrorFlightDirection(), 1e-10);
        assertEquals(2.4444444, record.getVelocityErrorFlightVerticalDirection(), 1e-10);
        assertEquals(2.5555555, record.getVelocityErrorRadiusDirection(), 1e-10);
        final Ancillary3Record.DataPoint[] dataPoints = record.getDataPoints();
        for (int i = 0; i < dataPoints.length; i++) {
            final Ancillary3Record.DataPoint dataPoint = dataPoints[i];
            assertEquals((1 + 0.1 + 0.01 * i) * 0.001, dataPoint.getPositionalVectorDataPointX(), 1e-10);
            assertEquals((2 + 0.1 + 0.01 * i) * 0.001, dataPoint.getPositionalVectorDataPointY(), 1e-10);
            assertEquals((3 + 0.1 + 0.01 * i) * 0.001, dataPoint.getPositionalVectorDataPointZ(), 1e-10);
            assertEquals((4 + 0.1 + 0.01 * i) * 0.001, dataPoint.getVelocityVectorDataPointX(), 1e-10);
            assertEquals((5 + 0.1 + 0.01 * i) * 0.001, dataPoint.getVelocityVectorDataPointY(), 1e-10);
            assertEquals((6 + 0.1 + 0.01 * i) * 0.001, dataPoint.getVelocityVectorDataPointZ(), 1e-10);
        }
        assertEquals(1, record.getFlagLeapSecond());

    }
}
