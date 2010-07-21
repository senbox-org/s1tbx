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

import java.io.IOException;

public class CommonFileDescriptorRecord extends BaseRecord {

    private String _codeCharacter;
    private String _fileDocumentNumber;
    private String _fileDokumentRevisionNumber;
    private String _fileDesignRevisionLetter;
    private String _logicalVolPrepSysRelNum;
    private int _fileNumber;
    private String _fileID;
    private String _flagRecordComposition;
    private int _recordNumberPositionOfEachFile;
    private int _fieldLengthForRecordData;
    private String _flagOfRecordTypeCode;
    private int _recordTypeCodeBytePosition;
    private int _recordTypeCodeFieldLength;
    private String _flagRecordLength;
    private int _bytePosOfRecLength;
    private int _numOfBytesOfRecLength;
    private String _flagDataConvInfFileDescRec;
    private String _flagDataConvInOtherRecords;
    private String _flagDataDispFileDescRecord;
    private String _flagDataDispInOtherRecords;

    public CommonFileDescriptorRecord(final CeosFileReader reader, final long startPos) throws
                                                                                        IOException,
                                                                                        IllegalCeosFormatException {
        super(reader, startPos);
        _codeCharacter = reader.readAn(2);
        reader.skipBytes(2);    // blank
        _fileDocumentNumber = reader.readAn(12);
        _fileDokumentRevisionNumber = reader.readAn(2);
        _fileDesignRevisionLetter = reader.readAn(2);
        _logicalVolPrepSysRelNum = reader.readAn(12);
        _fileNumber = reader.readI4();
        _fileID = reader.readAn(16);
        _flagRecordComposition = reader.readAn(4);
        _recordNumberPositionOfEachFile = (int) reader.readIn(8);
        _fieldLengthForRecordData = reader.readI4();
        _flagOfRecordTypeCode = reader.readAn(4);
        _recordTypeCodeBytePosition = (int) reader.readIn(8);
        _recordTypeCodeFieldLength = reader.readI4();
        _flagRecordLength = reader.readAn(4);
        _bytePosOfRecLength = (int) reader.readIn(8);
        _numOfBytesOfRecLength = reader.readI4();
        _flagDataConvInfFileDescRec = reader.readAn(1);
        _flagDataConvInOtherRecords = reader.readAn(1);
        _flagDataDispFileDescRecord = reader.readAn(1);
        _flagDataDispInOtherRecords = reader.readAn(1);
        reader.skipBytes(64);   // blank
    }

    public String getCodeCharacter() {
        return _codeCharacter;
    }

    public String getFileDocumentNumber() {
        return _fileDocumentNumber;
    }

    public String getFileDokumentRevisionNumber() {
        return _fileDokumentRevisionNumber;
    }

    public String getFileDesignRevisionLetter() {
        return _fileDesignRevisionLetter;
    }

    public String getLogicalVolPrepSysRelNum() {
        return _logicalVolPrepSysRelNum;
    }

    public int getFileNumber() {
        return _fileNumber;
    }

    public String getFileID() {
        return _fileID;
    }

    public String getFlagRecordComposition() {
        return _flagRecordComposition;
    }

    public int getRecordNumberPositionOfEachFile() {
        return _recordNumberPositionOfEachFile;
    }

    public int getFieldLengthForRecordData() {
        return _fieldLengthForRecordData;
    }

    public String getFlagOfRecordTypeCode() {
        return _flagOfRecordTypeCode;
    }

    public int getRecordTypeCodeBytePosition() {
        return _recordTypeCodeBytePosition;
    }

    public int getRecordTypeCodeFieldLength() {
        return _recordTypeCodeFieldLength;
    }

    public String getFlagRecordLength() {
        return _flagRecordLength;
    }

    public int getBytePosOfRecLength() {
        return _bytePosOfRecLength;
    }

    public int getNumOfBytesOfRecLength() {
        return _numOfBytesOfRecLength;
    }

    public String getFlagDataConvInfFileDescRec() {
        return _flagDataConvInfFileDescRec;
    }

    public String getFlagDataConvInOtherRecords() {
        return _flagDataConvInOtherRecords;
    }

    public String getFlagDataDispFileDescRecord() {
        return _flagDataDispFileDescRecord;
    }

    public String getFlagDataDispInOtherRecords() {
        return _flagDataDispInOtherRecords;
    }
}
