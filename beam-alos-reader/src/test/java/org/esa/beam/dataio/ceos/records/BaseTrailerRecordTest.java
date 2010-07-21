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

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class BaseTrailerRecordTest extends TestCase {

    private String _prefix;
    private CeosFileReader _reader;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(os);
        _prefix = "TrailerRecordTest_prefix";
        ios.writeBytes(_prefix);
        writeRecordData(ios);
        ios.writeBytes("TrailerRecordTest_suffix"); // as suffix
        _reader = new CeosFileReader(ios);
    }

    public void testInit_SimpleConstructor() throws IOException,
                                                    IllegalCeosFormatException {
        _reader.seek(_prefix.length());

        final BaseTrailerRecord record = createTrailerRecord(_reader);

        assertRecord(record);
    }


    public void testInit() throws IOException,
                                  IllegalCeosFormatException {
        final BaseTrailerRecord record = createTrailerRecord(_reader, _prefix.length());

        assertRecord(record);
    }

    private void writeRecordData(final ImageOutputStream ios) throws IOException {
        BaseRecordTest.writeRecordData(ios);

        ios.writeBytes("   1"); // number of trailer records // I4
        ios.writeBytes("   1"); // number of trailer in one CCD // I4

        writeHistograms(ios);

        CeosTestHelper.writeBlanks(ios, 248);
    }

    private void assertRecord(final BaseTrailerRecord record) throws IOException {
        BaseRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
//        assertEquals(_prefix.length() + 8460, _ios.getStreamPosition());

        assertEquals(1, record.getNumTrailerRecords());
        assertEquals(1, record.getNumTrailerRecordsInOneCCDUnit());
        assertHistograms(record);
    }

    protected abstract BaseTrailerRecord createTrailerRecord(CeosFileReader reader) throws IOException,
                                                                                           IllegalCeosFormatException;

    protected abstract BaseTrailerRecord createTrailerRecord(final CeosFileReader reader, final int startPos) throws
                                                                                                              IOException,
                                                                                                              IllegalCeosFormatException;

    protected abstract void writeHistograms(final ImageOutputStream ios) throws IOException;

    protected abstract void assertHistograms(final BaseTrailerRecord record);

}