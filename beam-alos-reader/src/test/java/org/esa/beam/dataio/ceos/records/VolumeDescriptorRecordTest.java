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
import org.esa.beam.framework.datamodel.MetadataElement;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VolumeDescriptorRecordTest extends TestCase {

    private MemoryCacheImageOutputStream _ios;
    private String _prefix;
    private CeosFileReader _reader;

    @Override
    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "VolumeDescriptorRecordTest_prefix";
        _ios.writeBytes(_prefix);
        writeRecordData(_ios);
        _ios.writeBytes("VolumeDescriptorRecordTest_suffix"); // suffix
        _reader = new CeosFileReader(_ios);
    }

    public void testInit_SimpleConstructor() throws IOException,
                                                    IllegalCeosFormatException {
        _reader.seek(_prefix.length());
        final VolumeDescriptorRecord record = new VolumeDescriptorRecord(_reader);

        assertRecord(record);
    }

    public void testInit() throws IOException,
                                  IllegalCeosFormatException {
        final VolumeDescriptorRecord record = new VolumeDescriptorRecord(_reader, _prefix.length());

        assertRecord(record);
    }

    public void testAssignMetadata() throws IOException,
                                            IllegalCeosFormatException {
        final VolumeDescriptorRecord record = new VolumeDescriptorRecord(_reader, _prefix.length());
        final MetadataElement volumeMetadata = new MetadataElement("VOLUME_DESCRIPTOR");

        record.assignMetadataTo(volumeMetadata, "suffix");

        assertEquals(23, volumeMetadata.getNumAttributes());
        assertEquals(0, volumeMetadata.getNumElements());
        assertMetadata(volumeMetadata);
    }

    private void assertMetadata(final MetadataElement elem) {
        BaseRecordTest.assertMetadata(elem);

        BaseRecordTest.assertStringAttribute(elem, "Ascii code character", "AB");
        BaseRecordTest.assertStringAttribute(elem, "Specification number", "abcdefghijkl");
        BaseRecordTest.assertStringAttribute(elem, "Specification revision number", "CD");
        BaseRecordTest.assertStringAttribute(elem, "Record format revision number", "EF");
        BaseRecordTest.assertStringAttribute(elem, "Software version number", "bcdefghijklm");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume ID", "cdefghijklmnopqr");
        BaseRecordTest.assertStringAttribute(elem, "Volume set ID", "defghijklmnopqrs");
        BaseRecordTest.assertIntAttribute(elem, "Volume number of this volume descriptor record", 12);
        BaseRecordTest.assertIntAttribute(elem, "Number of first file following the volume directory file", 2345);
        BaseRecordTest.assertIntAttribute(elem, "Logical volume number in volume set", 3456);
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparation date", "efghijkl");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparation time", "fghijklm");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparation country", "ghijklmnopqr");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparing agent", "hijklmno");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparing facility", "ijklmnopqrst");
        BaseRecordTest.assertIntAttribute(elem, "Number of filepointer records", 4567);
        BaseRecordTest.assertIntAttribute(elem, "Number of records", 5678);
    }

    private void writeRecordData(final MemoryCacheImageOutputStream ios) throws IOException {
        BaseRecordTest.writeRecordData(ios);

        ios.writeBytes("AB"); // asciiCodeCharacter // A2
        CeosTestHelper.writeBlanks(ios, 2); // blank
        ios.writeBytes("abcdefghijkl"); // specificationNumber //A12
        ios.writeBytes("CD"); // specificationRevisionNumer // A2
        ios.writeBytes("EF"); // recordFormatRevisionNumer // A2
        ios.writeBytes("bcdefghijklm"); // softwareVersionNumber // A12
        CeosTestHelper.writeBlanks(ios, 16); // blank
        ios.writeBytes("cdefghijklmnopqr"); // logicalVolumeID // A16
        ios.writeBytes("defghijklmnopqrs"); // volumeSetID // A16
        CeosTestHelper.writeBlanks(ios, 6); // blank
        ios.writeBytes("12"); // volumeNuberOfThisVolumeDescritorRecord // I2
        ios.writeBytes("2345"); // nuberOfFirstFileFollowingTheVolumeDirectoryFile // I4
        ios.writeBytes("3456"); // logicalVolumeNumberInVolumeSet // I4
        CeosTestHelper.writeBlanks(ios, 4); // blank
        ios.writeBytes("efghijkl"); // logicalVolumePreparationDate // A8
        ios.writeBytes("fghijklm"); // logicalVolumePreparationTime // A8
        ios.writeBytes("ghijklmnopqr"); // logicalVolumePreparationCountry // A12
        ios.writeBytes("hijklmno"); // logicalVolumePreparingAgent // A8
        ios.writeBytes("ijklmnopqrst"); // logicalVolumePreparingFacility // A12
        ios.writeBytes("4567"); // numberOfFilepointerRecords // I4
        ios.writeBytes("5678"); // nuberOfRecords // I4
        CeosTestHelper.writeBlanks(ios, 192); // blank
    }

    private void assertRecord(final VolumeDescriptorRecord record) throws IOException {
        BaseRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + 360, _ios.getStreamPosition());

        assertEquals("AB", record.getAsciiCodeCharacter());
        assertEquals("abcdefghijkl", record.getSpecificationNumber());
        assertEquals("CD", record.getSpecificationRevisionNumber());
        assertEquals("EF", record.getRecordFormatRevisionNumer());
        assertEquals("bcdefghijklm", record.getSoftwareVersionNumber());
        assertEquals("cdefghijklmnopqr", record.getLogicalVolumeID());
        assertEquals("defghijklmnopqrs", record.getVolumeSetID());
        assertEquals(12, record.getVolumeNumberOfThisVolumeDescritorRecord());
        assertEquals(2345, record.getNumberOfFirstFileFollowingTheVolumeDirectoryFile());
        assertEquals(3456, record.getLogicalVolumeNumberInVolumeSet());
        assertEquals("efghijkl", record.getLogicalVolumePreparationDate());
        assertEquals("fghijklm", record.getLogicalVolumePreparationTime());
        assertEquals("ghijklmnopqr", record.getLogicalVolumePreparationCountry());
        assertEquals("hijklmno", record.getLogicalVolumePreparingAgent());
        assertEquals("ijklmnopqrst", record.getLogicalVolumePreparingFacility());
        assertEquals(4567, record.getNumberOfFilepointerRecords());
        assertEquals(5678, record.getNumberOfRecords());
    }
}
