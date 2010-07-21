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

public abstract class BaseImageFileDescriptorRecordTest extends TestCase {

    private MemoryCacheImageOutputStream _ios;
    private String _prefix;
    private CeosFileReader _reader;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "AbstractImageFileDescriptorRecordTest_prefix";
        _ios.writeBytes(_prefix);
        writeRecordData(_ios);
        _ios.writeBytes("AbstractImageFileDescriptorRecordTest_suffix"); // as suffix
        _reader = new CeosFileReader(_ios);
    }

    public void testInit_SimpleConstructor() throws IOException,
                                                    IllegalCeosFormatException {
        _reader.seek(_prefix.length());

        final BaseImageFileDescriptorRecord record = createImageFileDescriptorRecord(_reader);

        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + BaseRecordTest.RECORD_LENGTH, _ios.getStreamPosition());
        assertRecord(record);
    }

    public void testInit() throws IOException,
                                  IllegalCeosFormatException {
        final BaseImageFileDescriptorRecord record = createImageFileDescriptor(_reader, _prefix.length());

        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + BaseRecordTest.RECORD_LENGTH, _ios.getStreamPosition());
        assertRecord(record);
    }

    protected void writeRecordData(final ImageOutputStream ios) throws IOException {
        CommonFileDescriptorRecordTest.writeRecordData(ios);

        ios.writeBytes(" 12546"); // numImageRecords // I6
        ios.writeBytes(" 12487"); // imageRecordLength // I6
        CeosTestHelper.writeBlanks(ios, 24);
        ios.writeBytes(" 123"); // numBitsPerPixel // I4
        ios.writeBytes(" 234"); // numPixelsPerData // I4
        ios.writeBytes(" 345"); // numBytesPerData // I4
        ios.writeBytes("abcd"); // bitlistOfPixel // A4
        ios.writeBytes(" 567"); // numBandsPerFile // I4
        ios.writeBytes("14587962"); // numLinesPerBand // I8
        ios.writeBytes("1245"); // numLeftBorderPixelsPerLine // I4
        ios.writeBytes("24568954"); // numImagePixelsPerLine // I8
        ios.writeBytes("6542"); // numRightBorderPixelsPerLine // I4
        ios.writeBytes("5432"); // numTopBorderLines // I4
        ios.writeBytes("4321"); // numBottomBorderLines // I4
        ios.writeBytes("bcde"); // imageFormatID // A4
        ios.writeBytes(" 852"); // numRecordsPerLineSingleUnit // I4
        ios.writeBytes(" 963"); // numRecordsPerLine // I4
        ios.writeBytes(" 741"); // numBytesCoverIdentifierAndHeader // I4
        ios.writeBytes("24562583"); // numImgDataBytesPerRecAndDummyPix // I8
        ios.writeBytes(" 987"); // numBytesOfSuffixDataPerRecord // I4
        ios.writeBytes("sdef"); // flagPrefixDataRepeat // A4
        ios.writeBytes("   1 4PB"); // locatorLineNumber
        ios.writeBytes("   5 4PB"); // locatorBandNumber
        ios.writeBytes("   9 6PB"); // locatorScanStartTime
        ios.writeBytes("  15 4PB"); // locatorLeftDummyPixel
        ios.writeBytes("  19 4PB"); // locatorRightDummyPixel

        writeBytes341To392(ios);

        ios.writeBytes("oiklfd4klsgjopesirmfdlknaoiawef5lkdd"); // dataFormatTypeId // A36
        ios.writeBytes("BVFR"); // dataFormatTypeIdCode // A4
        ios.writeBytes(" 753"); // numLeftUnusedBitsInPixelData // I4
        ios.writeBytes(" 357"); // numRightUnusedBitsInPixelData // I4
        ios.writeBytes(" 242"); // maxPixelDataValue // I4
        CeosTestHelper.writeBlanks(ios, 4);
        CeosTestHelper.writeBlanks(ios, 8);
        CeosTestHelper.writeBlanks(ios, 8);
        CeosTestHelper.writeBlanks(ios, BaseRecordTest.RECORD_LENGTH - 464);
    }

    protected void assertRecord(final BaseImageFileDescriptorRecord record) {
        CommonFileDescriptorRecordTest.assertRecord(record);

        assertEquals(12546, record.getNumImageRecords());
        assertEquals(12487, record.getImageRecordLength());
        assertEquals(123, record.getNumBitsPerPixel());
        assertEquals(234, record.getNumPixelsPerData());
        assertEquals(345, record.getNumBytesPerData());
        assertEquals("abcd", record.getBitlistOfPixel());
        assertEquals(567, record.getNumBandsPerFile());
        assertEquals(14587962, record.getNumLinesPerBand());
        assertEquals(1245, record.getNumLeftBorderPixelsPerLine());
        assertEquals(24568954, record.getNumImagePixelsPerLine());
        assertEquals(6542, record.getNumRightBorderPixelsPerLine());
        assertEquals(5432, record.getNumTopBorderLines());
        assertEquals(4321, record.getNumBottomBorderLines());
        assertEquals("bcde", record.getImageFormatID());
        assertEquals(852, record.getNumRecordsPerLineSingleUnit());
        assertEquals(963, record.getNumRecordsPerLine());
        assertEquals(741, record.getNumBytesCoverIdentifierAndHeader());
        assertEquals(24562583, record.getNumImgDataBytesPerRecAndDummyPix());
        assertEquals(987, record.getNumBytesOfSuffixDataPerRecord());
        assertEquals("sdef", record.getFlagPrefixDataRepeat());
        assertEquals("   1 4PB", record.getLocatorLineNumber());
        assertEquals("   5 4PB", record.getLocatorBandNumber());
        assertEquals("   9 6PB", record.getLocatorScanStartTime());
        assertEquals("  15 4PB", record.getLocatorLeftDummyPixel());
        assertEquals("  19 4PB", record.getLocatorRightDummyPixel());

        assertBytes341To392(record);

        assertEquals("oiklfd4klsgjopesirmfdlknaoiawef5lkdd", record.getDataFormatTypeId());
        assertEquals("BVFR", record.getDataFormatTypeIdCode());
        assertEquals(753, record.getNumLeftUnusedBitsInPixelData());
        assertEquals(357, record.getNumRightUnusedBitsInPixelData());
        assertEquals(242, record.getMaxPixelDataValue());
    }


    protected abstract BaseImageFileDescriptorRecord createImageFileDescriptorRecord(final CeosFileReader reader) throws
                                                                                                                  IOException,
                                                                                                                  IllegalCeosFormatException;

    protected abstract BaseImageFileDescriptorRecord createImageFileDescriptor(final CeosFileReader reader,
                                                                               final int startPos) throws IOException,
                                                                                                          IllegalCeosFormatException;

    protected abstract void writeBytes341To392(ImageOutputStream ios) throws IOException;

    protected abstract void assertBytes341To392(final BaseImageFileDescriptorRecord record);
}
