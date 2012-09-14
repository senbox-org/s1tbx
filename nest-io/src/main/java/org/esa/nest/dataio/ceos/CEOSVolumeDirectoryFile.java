/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.nest.dataio.binary.BinaryDBReader;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;

import java.io.IOException;


/**
 * This class represents a volume directory file of a product.
 *
 */
public class CEOSVolumeDirectoryFile {

    private BinaryRecord volumeDescriptorRecord;
    private FilePointerRecord[] filePointerRecords;
    private BinaryRecord textRecord;

    private final static String volume_desc_recordDefinitionFile = "volume_descriptor.xml";
    private final static String filePointerDefinitionFile = "file_pointer_record.xml";
    private final static String text_recordDefinitionFile = "text_record.xml";

    private static org.jdom.Document volDescXML;
    private static org.jdom.Document filePointerXML;
    private static org.jdom.Document textRecXML;

    public CEOSVolumeDirectoryFile(final BinaryFileReader binaryReader, final String mission) throws IOException {
        if(volDescXML == null)
            volDescXML = BinaryDBReader.loadDefinitionFile(mission, volume_desc_recordDefinitionFile);
        volumeDescriptorRecord = new BinaryRecord(binaryReader, -1, volDescXML, volume_desc_recordDefinitionFile);
    }

    public void readFilePointersAndTextRecords(final BinaryFileReader binaryReader, final String mission) throws IOException {
        try {
            if(filePointerXML == null)
                filePointerXML = BinaryDBReader.loadDefinitionFile(mission, filePointerDefinitionFile);
            filePointerRecords = CeosHelper.readFilePointers(volumeDescriptorRecord, filePointerXML, filePointerDefinitionFile);
        } catch(Exception e) {
            System.out.println("Error reading file pointer record: "+e.getMessage());
        }

        try {
            if(textRecXML == null)
                textRecXML = BinaryDBReader.loadDefinitionFile(mission, text_recordDefinitionFile);
            textRecord = new BinaryRecord(binaryReader, -1, textRecXML, text_recordDefinitionFile);
        } catch(Exception e) {
            System.out.println("Error reading text record: "+e.getMessage());
        }
    }

    public BinaryRecord getTextRecord() {
        return textRecord;
    }

    public BinaryRecord getVolumeDescriptorRecord() {
        return volumeDescriptorRecord;
    }

    public String getProductName() {
        return CeosHelper.getProductName(textRecord);
    }

    public String getProductType() {
        return CeosHelper.getProductType(textRecord);
    }

    public void assignMetadataTo(final MetadataElement rootElem) {
        CeosHelper.addMetadata(rootElem, volumeDescriptorRecord, "Volume Descriptor");
        CeosHelper.addMetadata(rootElem, textRecord, "Text Record");

        int i = 1;
        for(FilePointerRecord fp : filePointerRecords) {
            CeosHelper.addMetadata(rootElem, fp, "File Pointer Record " + i++);
        }
    }

}