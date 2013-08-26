/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos.records;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.nest.dataio.binary.BinaryDBReader;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.IllegalBinaryFormatException;
import org.esa.nest.dataio.ceos.FilePointerRecord;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FilePointerRecordTest extends TestCase {

    private ImageOutputStream _ios;
    private String _prefix;
    private BinaryFileReader _reader;

    private final static String mission = "ers";
    private final static String filePointerDefinitionFile = "file_pointer_record.xml";
    private final static org.jdom.Document filePointerXML = BinaryDBReader.loadDefinitionFile(mission, filePointerDefinitionFile);

    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "fdkjglsdkfhierr.m b9b0970w34";
        _ios.writeBytes(_prefix);
        writeRecordData(_ios);
        _ios.writeBytes("nq3tf9ß8nvnvpdi er 0 324p3f"); // as suffix
        _reader = new BinaryFileReader(_ios);
    }

    public void testInit_SimpleConstructor() throws IOException, IllegalBinaryFormatException {
        _ios.seek(_prefix.length());
        final FilePointerRecord record = new FilePointerRecord(_reader, filePointerXML, filePointerDefinitionFile);

        assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + 360, _ios.getStreamPosition());
    }

    public void testInit() throws IOException, IllegalBinaryFormatException {
        final FilePointerRecord record = new FilePointerRecord(_reader, filePointerXML, _prefix.length(), filePointerDefinitionFile);

        assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + 360, _ios.getStreamPosition());
    }

    public void testAssignMetadataTo() throws IOException, IllegalBinaryFormatException {
        final FilePointerRecord record = new FilePointerRecord(_reader, filePointerXML, _prefix.length(), filePointerDefinitionFile);
        final MetadataElement elem = new MetadataElement("elem");

        record.assignMetadataTo(elem, "suffix");

        assertEquals(0, elem.getNumAttributes());
        assertEquals(1, elem.getNumElements());
        final MetadataElement recordRoot = elem.getElement("FilePointerRecord suffix");
        assertNotNull(recordRoot);
        assertMetadata(recordRoot);
        assertEquals(0, recordRoot.getNumElements());
        assertEquals(17, recordRoot.getNumAttributes());
    }

    public static void assertMetadata(final MetadataElement elem) {
        BaseRecordTest.assertMetadata(elem);

        BaseRecordTest.assertStringAttribute(elem, "Ascii code character", "A ");
        BaseRecordTest.assertIntAttribute(elem, "File pointer number", 2);
        BaseRecordTest.assertStringAttribute(elem, "File ID", "AL PSMB2IMGYBSQ ");
        BaseRecordTest.assertStringAttribute(elem, "File class", "IMAGERY                     ");
        BaseRecordTest.assertStringAttribute(elem, "File class code", "IMGY");
        BaseRecordTest.assertStringAttribute(elem, "File datatype", "BINARY ONLY                 ");
        BaseRecordTest.assertStringAttribute(elem, "File datatype code", "BINO");
        BaseRecordTest.assertIntAttribute(elem, "Number of records", 14001);
        BaseRecordTest.assertIntAttribute(elem, "FirstRecordLength", 897623);
        BaseRecordTest.assertIntAttribute(elem, "MaxRecordLength", 8634264);
        BaseRecordTest.assertStringAttribute(elem, "RecordLengthType", "FIXED LENGTH");
        BaseRecordTest.assertStringAttribute(elem, "RecordLengthTypeCode", "FIXD");
        BaseRecordTest.assertIntAttribute(elem, "FirstRecordVolumeNumber", 1);
        BaseRecordTest.assertIntAttribute(elem, "FinalRecordVolumeNumber", 2);
        BaseRecordTest.assertIntAttribute(elem, "ReferencedFilePortionStart", 3);
        BaseRecordTest.assertIntAttribute(elem, "ReferencedFilePortionEnd", 17);
    }

    private static void assertRecord(final FilePointerRecord record) {
        BaseRecordTest.assertRecord(record);

        assertNotNull(record);
        assertEquals("A ", record.getAttributeString("Ascii code character"));
        assertEquals(2, (int)record.getAttributeInt("File Pointer Number"));
        assertEquals("AL PSMB2IMGYBSQ ", record.getAttributeString("File ID"));
        assertEquals("IMAGERY                     ", record.getAttributeString("File class"));
        assertEquals("IMGY", record.getAttributeString("File class code"));
        assertEquals("BINARY ONLY                 ", record.getAttributeString("File datatype"));
        assertEquals("BINO", record.getAttributeString("File datatype Code"));
        assertEquals(14001, (int)record.getAttributeInt("Number of records"));
        assertEquals(897623, (int)record.getAttributeInt("FirstRecordLength"));
        assertEquals(8634264, (int)record.getAttributeInt("MaxRecordLength"));
        assertEquals("FIXED LENGTH", record.getAttributeString("RecordLengthType"));
        assertEquals("FIXD", record.getAttributeString("RecordLengthTypeCode"));
        assertEquals(1, (int)record.getAttributeInt("FirstRecordVolumeNumber"));
        assertEquals(2, (int)record.getAttributeInt("FinalRecordVolumeNumber"));
        assertEquals(3, (int)record.getAttributeInt("ReferencedFilePortionStart"));
        assertEquals(17, (int)record.getAttributeInt("ReferencedFilePortionEnd"));
    }

    private static void writeRecordData(ImageOutputStream ios) throws IOException {
        BaseRecordTest.writeRecordData(ios);
        ios.writeBytes("A "); // codeCharacter = "A" + 1 blank // A2
        ios.skipBytes(2); // reader.skipBytes(2);  // blank
        ios.writeBytes("   2"); // filePointerNumber = bbb1 - bbb9 // I4
        ios.writeBytes("AL PSMB2IMGYBSQ "); // fileID = "LL SSSCTFFFFXXXB"reader.readAn(16);
        ios.writeBytes("IMAGERY                     "); // fileClass // A28
        ios.writeBytes("IMGY"); // fileClassCode = "LEAD", "IMGY", "TRAI" or "SPPL"  // A4
        // fileDataType = "BINARY ONLY                 " or "MIXED BINARY AND ASCII      "
        ios.writeBytes("BINARY ONLY                 "); // A28
        ios.writeBytes("BINO"); // fileDataTypeCode = "MBAA" or "BINO" // A4
        ios.writeBytes("   14001"); // numberOfRecords = 2 - n+1 // I8
        ios.writeBytes("  897623"); // _firstRecordLength // I8
        ios.writeBytes(" 8634264"); // _maxRecordLength  // I8
        ios.writeBytes("FIXED LENGTH"); // _recordLengthType = "FIXED LENGTH" // A12
        ios.writeBytes("FIXD"); // _recordLengthTypeCode = "FIXD" // A4
        ios.writeBytes(" 1"); // _firstRecordVolumeNumer = 1 // I2
        ios.writeBytes(" 2"); // _finalRecordVolumeNumber = 1 (2 only for test)// I2
        ios.writeBytes("       3"); // ReferencedFilePortionStart = 1 (3 only for test) // I8
        ios.writeBytes("      17"); // ReferencedFilePortionEnd = 1 (3 only for test) // I8

        // Blank = 208 blanks
        ios.writeBytes("                                                  " +
                       "                                                  " +
                       "                                                  " +
                       "                                                  "); // A208
    }
}
