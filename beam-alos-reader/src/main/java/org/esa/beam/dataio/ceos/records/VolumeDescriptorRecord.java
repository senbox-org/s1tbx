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

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.framework.datamodel.MetadataElement;

import java.io.IOException;

public class VolumeDescriptorRecord extends BaseRecord {

    private final String _asciiCodeCharacter;
    private final String _specificationNumber;
    private final String _specificationRevisionNumber;
    private final String _recordFormatRevisionNumer;
    private final String _softwareVersionNumber;
    private final String _logicalVolumeID;
    private final String _volumeSetID;
    private final int _volumeNumberOfThisVolumeDescritorRecord;
    private final int _numberOfFirstFileFollowingTheVolumeDirectoryFile;
    private final int _logicalVolumeNumberInVolumeSet;
    private final String _logicalVolumePreparationDate;
    private final String _logicalVolumePreparationTime;
    private String _logicalVolumePreparationCountry;
    private String _logicalVolumePreparingAgent;
    private String _logicalVolumePreparingFacility;
    private int _numberOfFilepointerRecords;
    private int _numberOfRecords;

    public VolumeDescriptorRecord(CeosFileReader reader) throws IOException,
                                                                IllegalCeosFormatException {
        this(reader, -1);
    }

    public VolumeDescriptorRecord(CeosFileReader reader, long startPos) throws IOException,
                                                                               IllegalCeosFormatException {
        super(reader, startPos);

        _asciiCodeCharacter = reader.readAn(2);
        reader.skipBytes(2); // blank
        _specificationNumber = reader.readAn(12);
        _specificationRevisionNumber = reader.readAn(2);
        _recordFormatRevisionNumer = reader.readAn(2);
        _softwareVersionNumber = reader.readAn(12);
        reader.skipBytes(16); // blank
        _logicalVolumeID = reader.readAn(16);
        _volumeSetID = reader.readAn(16);
        reader.skipBytes(6); // blank
        _volumeNumberOfThisVolumeDescritorRecord = (int) reader.readIn(2);
        _numberOfFirstFileFollowingTheVolumeDirectoryFile = reader.readI4();
        _logicalVolumeNumberInVolumeSet = reader.readI4();
        reader.skipBytes(4); // blank
        _logicalVolumePreparationDate = reader.readAn(8);
        _logicalVolumePreparationTime = reader.readAn(8);
        _logicalVolumePreparationCountry = reader.readAn(12);
        _logicalVolumePreparingAgent = reader.readAn(8);
        _logicalVolumePreparingFacility = reader.readAn(12);
        _numberOfFilepointerRecords = reader.readI4();
        _numberOfRecords = reader.readI4();
        reader.skipBytes(192);
    }

    public String getAsciiCodeCharacter() {
        return _asciiCodeCharacter;
    }

    public String getSpecificationNumber() {
        return _specificationNumber;
    }

    public String getSpecificationRevisionNumber() {
        return _specificationRevisionNumber;
    }

    public String getRecordFormatRevisionNumer() {
        return _recordFormatRevisionNumer;
    }

    public String getSoftwareVersionNumber() {
        return _softwareVersionNumber;
    }

    public String getLogicalVolumeID() {
        return _logicalVolumeID;
    }

    public String getVolumeSetID() {
        return _volumeSetID;
    }

    public int getVolumeNumberOfThisVolumeDescritorRecord() {
        return _volumeNumberOfThisVolumeDescritorRecord;
    }

    public int getNumberOfFirstFileFollowingTheVolumeDirectoryFile() {
        return _numberOfFirstFileFollowingTheVolumeDirectoryFile;
    }

    public int getLogicalVolumeNumberInVolumeSet() {
        return _logicalVolumeNumberInVolumeSet;
    }

    public String getLogicalVolumePreparationDate() {
        return _logicalVolumePreparationDate;
    }

    public String getLogicalVolumePreparationTime() {
        return _logicalVolumePreparationTime;
    }

    public String getLogicalVolumePreparationCountry() {
        return _logicalVolumePreparationCountry;
    }

    public String getLogicalVolumePreparingAgent() {
        return _logicalVolumePreparingAgent;
    }

    public String getLogicalVolumePreparingFacility() {
        return _logicalVolumePreparingFacility;
    }

    public int getNumberOfFilepointerRecords() {
        return _numberOfFilepointerRecords;
    }

    public int getNumberOfRecords() {
        return _numberOfRecords;
    }

    @Override
    public void assignMetadataTo(final MetadataElement elem, final String suffix) {
//        final MetadataElement elem = createMetadataElement("VolumeDescriptorRecord", suffix);
//        root.addElement(elem);

        super.assignMetadataTo(elem, suffix);

        elem.setAttributeString("Ascii code character", _asciiCodeCharacter);
        elem.setAttributeString("Specification number", _specificationNumber);
        elem.setAttributeString("Specification revision number", _specificationRevisionNumber);
        elem.setAttributeString("Record format revision number", _recordFormatRevisionNumer);
        elem.setAttributeString("Software version number", _softwareVersionNumber);
        elem.setAttributeString("Logical volume ID", _logicalVolumeID);
        elem.setAttributeString("Volume set ID", _volumeSetID);
        elem.setAttributeInt("Volume number of this volume descriptor record",
                             _volumeNumberOfThisVolumeDescritorRecord);
        elem.setAttributeInt("Number of first file following the volume directory file",
                             _numberOfFirstFileFollowingTheVolumeDirectoryFile);
        elem.setAttributeInt("Logical volume number in volume set", _logicalVolumeNumberInVolumeSet);
        elem.setAttributeString("Logical volume preparation date", _logicalVolumePreparationDate);
        elem.setAttributeString("Logical volume preparation time", _logicalVolumePreparationTime);
        elem.setAttributeString("Logical volume preparation country", _logicalVolumePreparationCountry);
        elem.setAttributeString("Logical volume preparing agent", _logicalVolumePreparingAgent);
        elem.setAttributeString("Logical volume preparing facility", _logicalVolumePreparingFacility);
        elem.setAttributeInt("Number of filepointer records", _numberOfFilepointerRecords);
        elem.setAttributeInt("Number of records", _numberOfRecords);
    }
}
