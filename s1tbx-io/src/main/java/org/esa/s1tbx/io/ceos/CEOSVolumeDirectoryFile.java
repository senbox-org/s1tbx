/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.ceos;

import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.StringUtils;
import org.jdom2.Document;

import java.io.IOException;


/**
 * This class represents a volume directory file of a product.
 */
public class CEOSVolumeDirectoryFile implements CEOSFile {

    private BinaryRecord volumeDescriptorRecord;
    private FilePointerRecord[] filePointerRecords;
    private BinaryRecord textRecord;

    private final static String volume_desc_recordDefinitionFile = "volume_descriptor.xml";
    private final static String filePointerDefinitionFile = "file_pointer_record.xml";
    private final static String text_recordDefinitionFile = "text_record.xml";

    private Document volDescXML;
    private Document filePointerXML;
    private Document textRecXML;

    public CEOSVolumeDirectoryFile(final BinaryFileReader binaryReader, final String mission) throws IOException {
        if (volDescXML == null)
            volDescXML = loadDefinitionFile(mission, volume_desc_recordDefinitionFile);
        volumeDescriptorRecord = new BinaryRecord(binaryReader, -1, volDescXML, volume_desc_recordDefinitionFile);
    }

    public void readFilePointersAndTextRecords(final BinaryFileReader binaryReader, final String mission) throws IOException {
        try {
            if (filePointerXML == null)
                filePointerXML = loadDefinitionFile(mission, filePointerDefinitionFile);
            filePointerRecords = readFilePointers(volumeDescriptorRecord, filePointerXML, filePointerDefinitionFile);
        } catch (Exception e) {
            System.out.println("Error reading file pointer record: " + e.getMessage());
        }

        try {
            if (textRecXML == null)
                textRecXML = loadDefinitionFile(mission, text_recordDefinitionFile);
            textRecord = new BinaryRecord(binaryReader, -1, textRecXML, text_recordDefinitionFile);
        } catch (Exception e) {
            System.out.println("Error reading text record: " + e.getMessage());
        }
    }

    public BinaryRecord getTextRecord() {
        return textRecord;
    }

    public BinaryRecord getVolumeDescriptorRecord() {
        return volumeDescriptorRecord;
    }

    public String getProductName() {
        return getProductName(textRecord);
    }

    public String getProductType() {
        return getProductType(textRecord);
    }

    public String getProductOrigin() {
	return getProductOrigin(volumeDescriptorRecord);
    }

    public void assignMetadataTo(final MetadataElement rootElem) {
        CeosHelper.addMetadata(rootElem, volumeDescriptorRecord, "Volume Descriptor");
        CeosHelper.addMetadata(rootElem, textRecord, "Text Record");

        int i = 1;
        for (FilePointerRecord fp : filePointerRecords) {
            CeosHelper.addMetadata(rootElem, fp, "File Pointer Record " + i++);
        }
    }

    public static FilePointerRecord[] readFilePointers(final BinaryRecord vdr, final Document filePointerXML,
                                                       final String recName) throws IOException {
        final int numFilePointers = vdr.getAttributeInt("Number of filepointer records");
        final BinaryFileReader reader = vdr.getReader();
        reader.seek(vdr.getRecordLength());
        final FilePointerRecord[] filePointers = new FilePointerRecord[numFilePointers];
        for (int i = 0; i < numFilePointers; i++) {
            filePointers[i] = new FilePointerRecord(reader, filePointerXML, recName);
        }
        return filePointers;
    }

    public static String getProductName(final BinaryRecord textRecord) {
        if (textRecord == null) return "unknown";
        final String name = textRecord.getAttributeString("Product type specifier").trim().replace("PRODUCT:", "")
                + '-' + textRecord.getAttributeString("Scene identification").trim();
        return StringUtils.createValidName(name.trim(), new char[]{'_', '-'}, '_');
    }

    public static String getProductType(final BinaryRecord textRecord) {
        if (textRecord == null) return "unknown";
        String type = textRecord.getAttributeString("Product type specifier").trim();
        type = type.replace("PRODUCT:", "");
        type = type.replace("JERS-1", "JERS1");
        type = type.replace("JERS_1", "JERS1");
        type = type.replace("ERS-1", "ERS1");
        type = type.replace("ERS_1", "ERS1");
        type = type.replace("ERS-2", "ERS2");
        type = type.replace("ERS_2", "ERS2");
        return type.trim();
    }

    public static String getProductOrigin(final BinaryRecord vdr) {
	return vdr.getAttributeString("Specification number");
    }
}
