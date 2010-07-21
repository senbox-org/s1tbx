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

public abstract class BaseImageFileDescriptorRecord extends CommonFileDescriptorRecord {

    private int _numImageRecords;
    private int _imageRecordLength;
    private int _numBitsPerPixel;
    private int _numPixelsPerData;
    private int _numBytesPerData;
    private String _bitlistOfPixel;
    private int _numBandsPerFile;
    private int _numLinesPerBand;
    private int _numLeftBorderPixelsPerLine;
    private int _numImagePixelsPerLine;
    private int _numRightBorderPixelsPerLine;
    private int _numTopBorderLines;
    private int _numBottomBorderLines;
    private String _imageFormatID;
    private int _numRecordsPerLineSingleUnit;
    private int _numRecordsPerLine;
    private int _numBytesCoverIdentifierAndHeader;
    private int _numImgDataBytesPerRecAndDummyPix;
    private int _numBytesOfSuffixDataPerRecord;
    private String _flagPrefixDataRepeat;
    private String _locatorLineNumber;
    private String _locatorBandNumber;
    private String _locatorScanStartTime;
    private String _locatorLeftDummyPixel;
    private String _locatorRightDummyPixel;
    private String _dataFormatTypeId;
    private String _dataFormatTypeIdCode;
    private int _numLeftUnusedBitsInPixelData;
    private int _numRightUnusedBitsInPixelData;
    private int _maxPixelDataValue;

    public BaseImageFileDescriptorRecord(final CeosFileReader reader) throws IOException,
                                                                             IllegalCeosFormatException {
        this(reader, -1);
    }

    public BaseImageFileDescriptorRecord(final CeosFileReader reader, final long startPos) throws
                                                                                           IOException,
                                                                                           IllegalCeosFormatException {
        super(reader, startPos);

        readFields(reader);
        reader.seek(getAbsolutPosition(getRecordLength()));
    }

    private void readFields(final CeosFileReader reader) throws IOException,
                                                                IllegalCeosFormatException {
        _numImageRecords = (int) reader.readIn(6);
        _imageRecordLength = (int) reader.readIn(6);
        reader.skipBytes(24); // blanks
        _numBitsPerPixel = reader.readI4();
        _numPixelsPerData = reader.readI4();
        _numBytesPerData = reader.readI4();
        _bitlistOfPixel = reader.readAn(4);
        _numBandsPerFile = reader.readI4();
        _numLinesPerBand = (int) reader.readIn(8);
        _numLeftBorderPixelsPerLine = reader.readI4();
        _numImagePixelsPerLine = (int) reader.readIn(8);
        _numRightBorderPixelsPerLine = reader.readI4();
        _numTopBorderLines = reader.readI4();
        _numBottomBorderLines = reader.readI4();
        _imageFormatID = reader.readAn(4);
        _numRecordsPerLineSingleUnit = reader.readI4();
        _numRecordsPerLine = reader.readI4();
        _numBytesCoverIdentifierAndHeader = reader.readI4();
        _numImgDataBytesPerRecAndDummyPix = (int) reader.readIn(8);
        _numBytesOfSuffixDataPerRecord = reader.readI4();
        _flagPrefixDataRepeat = reader.readAn(4);

        _locatorLineNumber = reader.readAn(8);
        _locatorBandNumber = reader.readAn(8);
        _locatorScanStartTime = reader.readAn(8);
        _locatorLeftDummyPixel = reader.readAn(8);
        _locatorRightDummyPixel = reader.readAn(8);

        reader.skipBytes(52);   // depends on specific product

        _dataFormatTypeId = reader.readAn(36);
        _dataFormatTypeIdCode = reader.readAn(4);
        _numLeftUnusedBitsInPixelData = reader.readI4();
        _numRightUnusedBitsInPixelData = reader.readI4();
        _maxPixelDataValue = reader.readI4();

        readSpecificFields(reader);
    }

    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
    }

    public int getNumImageRecords() {
        return _numImageRecords;
    }

    public int getImageRecordLength() {
        return _imageRecordLength;
    }

    public int getNumBitsPerPixel() {
        return _numBitsPerPixel;
    }

    public int getNumPixelsPerData() {
        return _numPixelsPerData;
    }

    public int getNumBytesPerData() {
        return _numBytesPerData;
    }

    public String getBitlistOfPixel() {
        return _bitlistOfPixel;
    }

    public int getNumBandsPerFile() {
        return _numBandsPerFile;
    }

    public int getNumLinesPerBand() {
        return _numLinesPerBand;
    }

    public int getNumLeftBorderPixelsPerLine() {
        return _numLeftBorderPixelsPerLine;
    }

    public int getNumImagePixelsPerLine() {
        return _numImagePixelsPerLine;
    }

    public int getNumRightBorderPixelsPerLine() {
        return _numRightBorderPixelsPerLine;
    }

    public int getNumTopBorderLines() {
        return _numTopBorderLines;
    }

    public int getNumBottomBorderLines() {
        return _numBottomBorderLines;
    }

    public String getImageFormatID() {
        return _imageFormatID;
    }

    public int getNumRecordsPerLineSingleUnit() {
        return _numRecordsPerLineSingleUnit;
    }

    public int getNumRecordsPerLine() {
        return _numRecordsPerLine;
    }

    public int getNumBytesCoverIdentifierAndHeader() {
        return _numBytesCoverIdentifierAndHeader;
    }

    public int getNumImgDataBytesPerRecAndDummyPix() {
        return _numImgDataBytesPerRecAndDummyPix;
    }

    public int getNumBytesOfSuffixDataPerRecord() {
        return _numBytesOfSuffixDataPerRecord;
    }

    public String getFlagPrefixDataRepeat() {
        return _flagPrefixDataRepeat;
    }

    public String getLocatorBandNumber() {
        return _locatorBandNumber;
    }

    public String getLocatorLeftDummyPixel() {
        return _locatorLeftDummyPixel;
    }

    public String getLocatorLineNumber() {
        return _locatorLineNumber;
    }

    public String getLocatorRightDummyPixel() {
        return _locatorRightDummyPixel;
    }

    public String getLocatorScanStartTime() {
        return _locatorScanStartTime;
    }

    public String getDataFormatTypeId() {
        return _dataFormatTypeId;
    }

    public String getDataFormatTypeIdCode() {
        return _dataFormatTypeIdCode;
    }

    public int getNumLeftUnusedBitsInPixelData() {
        return _numLeftUnusedBitsInPixelData;
    }

    public int getNumRightUnusedBitsInPixelData() {
        return _numRightUnusedBitsInPixelData;
    }

    public int getMaxPixelDataValue() {
        return _maxPixelDataValue;
    }
}
