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

public class FilePointerRecord extends BaseRecord {

    public static final int RECORD_LENGTH = 360;

    private static final String IMAGE_FILE_CLASS_CODE = "IMGY";

    private final String _codeCharacter;
    private final int _filePointerNumber;
    private final String _fileID;
    private final String _fileClass;
    private final String _fileClassCode;
    private final String _fileDataType;
    private final String _fileDataTypeCode;
    private final int _numberOfRecords;
    private final int _firstRecordLength;
    private final int _maxRecordLength;
    private final String _recordLengthType;
    private final String _recordLengthTypeCode;
    private final int _firstRecordVolumeNumber;
    private final int _finalRecordVolumeNumber;
    private final int _firstRecordNumberOfReferencedFile;

    public FilePointerRecord(final CeosFileReader reader) throws IOException, IllegalCeosFormatException {
        this(reader, -1);
    }

    public FilePointerRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                      IllegalCeosFormatException {
        super(reader, startPos);
        _codeCharacter = reader.readAn(2);
        reader.skipBytes(2);    // blank
        _filePointerNumber = reader.readI4();
        _fileID = reader.readAn(16);
        _fileClass = reader.readAn(28);
        _fileClassCode = reader.readAn(4);
        _fileDataType = reader.readAn(28);
        _fileDataTypeCode = reader.readAn(4);
        _numberOfRecords = (int) reader.readIn(8);
        _firstRecordLength = (int) reader.readIn(8);
        _maxRecordLength = (int) reader.readIn(8);
        _recordLengthType = reader.readAn(12);
        _recordLengthTypeCode = reader.readAn(4);
        _firstRecordVolumeNumber = (int) reader.readIn(2);
        _finalRecordVolumeNumber = (int) reader.readIn(2);
        _firstRecordNumberOfReferencedFile = (int) reader.readIn(8);
        // skip the last 208 blanks
        reader.skipBytes(208);
    }

    public String getCodeCharacter() {
        return _codeCharacter;
    }

    public String getFileClass() {
        return _fileClass;
    }

    public String getFileClassCode() {
        return _fileClassCode;
    }

    public String getFileDataType() {
        return _fileDataType;
    }

    public String getFileDataTypeCode() {
        return _fileDataTypeCode;
    }

    public String getFileID() {
        return _fileID;
    }

    public int getFilePointerNumber() {
        return _filePointerNumber;
    }

    public int getFirstRecordLength() {
        return _firstRecordLength;
    }

    public int getMaxRecordLength() {
        return _maxRecordLength;
    }

    public int getNumberOfRecords() {
        return _numberOfRecords;
    }

    public String getRecordLengthType() {
        return _recordLengthType;
    }

    public String getRecordLengthTypeCode() {
        return _recordLengthTypeCode;
    }

    public boolean isImageFileRecord() {
        return FilePointerRecord.IMAGE_FILE_CLASS_CODE.equalsIgnoreCase(_fileClassCode);
    }

    public int getFirstRecordVolumeNumber() {
        return _firstRecordVolumeNumber;
    }

    public int getFinalRecordVolumeNumber() {
        return _finalRecordVolumeNumber;
    }

    public int getFirstRecordNumberOfReferencedFile() {
        return _firstRecordNumberOfReferencedFile;
    }

    @Override
    public void assignMetadataTo(final MetadataElement root, final String suffix) {
        final MetadataElement elem = createMetadataElement("FilePointerRecord", suffix);
        root.addElement(elem);

        super.assignMetadataTo(elem, null);

        elem.setAttributeString("Code character", _codeCharacter);
        elem.setAttributeInt("File pointer number", _filePointerNumber);
        elem.setAttributeString("File ID", _fileID);
        elem.setAttributeString("File class", _fileClass);
        elem.setAttributeString("File class code", _fileClassCode);
        elem.setAttributeString("File datatype", _fileDataType);
        elem.setAttributeString("File datatype code", _fileDataTypeCode);
        elem.setAttributeInt("Number of records", _numberOfRecords);
        elem.setAttributeInt("First record length", _firstRecordLength);
        elem.setAttributeInt("Max record length", _maxRecordLength);
        elem.setAttributeString("Record lengthtype", _recordLengthType);
        elem.setAttributeString("Record lengthtype code", _recordLengthTypeCode);
        elem.setAttributeInt("First record volume numer", _firstRecordVolumeNumber);
        elem.setAttributeInt("Final record volume number", _finalRecordVolumeNumber);
        elem.setAttributeInt("First record number of referenced file", _firstRecordNumberOfReferencedFile);
    }
}
