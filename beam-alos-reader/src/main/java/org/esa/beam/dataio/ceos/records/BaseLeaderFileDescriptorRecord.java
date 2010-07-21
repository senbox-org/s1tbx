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

public class BaseLeaderFileDescriptorRecord extends CommonFileDescriptorRecord {

    private int _numSceneHeaderRecords;
    private int _sceneHeaderRecordLength;
    private int _numAncillaryRecords;
    private int _ancillaryRecordLength;

    private int _sceneIdFieldLocator;
    private int _sceneIdFieldDataStart;
    private int _sceneIdFieldNumBytes;
    private String _sceneIdFieldDataType;

    private int _RSPIdLocator;
    private int _RSPIdDataStart;
    private int _RSPIdNumBytes;
    private String _RSPIdDataType;

    private int _missionIdLocator;
    private int _missionIdDataStart;
    private int _missionIdNumBytes;
    private String _missionIdDataType;

    private int _sensorIdLocator;
    private int _sensorIdDataStart;
    private int _sensorIdNumBytes;
    private String _sensorIdDataType;

    private int _sceneCenterTimeLocator;
    private int _sceneCenterTimeDataStart;
    private int _sceneCenterTimeNumBytes;
    private String _sceneCenterTimeDataType;

    private int _sceneCenterLatLonLocator;
    private int _sceneCenterLatLonDataStart;
    private int _sceneCenterLatLonNumBytes;
    private String _sceneCenterLatLonDataType;

    private int _processingLevelLocator;
    private int _processingLevelDataStart;
    private int _processingLevelNumBytes;
    private String _processingLevelDataType;

    private int _imageFormatLocator;
    private int _imageFormatDataStart;
    private int _imageFormatNumBytes;
    private String _imageFormatDataType;

    private int _effektiveBandLocator;
    private int _effektiveBandDataStart;
    private int _effektiveBandNumBytes;
    private String _effektiveBandDataType;

    public BaseLeaderFileDescriptorRecord(final CeosFileReader reader) throws IOException,
                                                                              IllegalCeosFormatException {
        this(reader, -1);
    }

    public BaseLeaderFileDescriptorRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                                   IllegalCeosFormatException {
        super(reader, startPos);
        readGeneralFields(reader);
        readSpecificFields(reader);
        reader.seek(getAbsolutPosition(getRecordLength()));
    }

    private void readGeneralFields(final CeosFileReader reader) throws IOException,
                                                                       IllegalCeosFormatException {
        _numSceneHeaderRecords = (int) reader.readIn(6);
        _sceneHeaderRecordLength = (int) reader.readIn(6);
        _numAncillaryRecords = (int) reader.readIn(6);
        _ancillaryRecordLength = (int) reader.readIn(6);
        reader.skipBytes(12); // 2 * A6 Blank

        _sceneIdFieldLocator = (int) reader.readIn(6);
        _sceneIdFieldDataStart = (int) reader.readIn(6);
        _sceneIdFieldNumBytes = (int) reader.readIn(3);
        _sceneIdFieldDataType = reader.readAn(1);

        _RSPIdLocator = (int) reader.readIn(6);
        _RSPIdDataStart = (int) reader.readIn(6);
        _RSPIdNumBytes = (int) reader.readIn(3);
        _RSPIdDataType = reader.readAn(1);

        _missionIdLocator = (int) reader.readIn(6);
        _missionIdDataStart = (int) reader.readIn(6);
        _missionIdNumBytes = (int) reader.readIn(3);
        _missionIdDataType = reader.readAn(1);

        _sensorIdLocator = (int) reader.readIn(6);
        _sensorIdDataStart = (int) reader.readIn(6);
        _sensorIdNumBytes = (int) reader.readIn(3);
        _sensorIdDataType = reader.readAn(1);

        _sceneCenterTimeLocator = (int) reader.readIn(6);
        _sceneCenterTimeDataStart = (int) reader.readIn(6);
        _sceneCenterTimeNumBytes = (int) reader.readIn(3);
        _sceneCenterTimeDataType = reader.readAn(1);

        _sceneCenterLatLonLocator = (int) reader.readIn(6);
        _sceneCenterLatLonDataStart = (int) reader.readIn(6);
        _sceneCenterLatLonNumBytes = (int) reader.readIn(3);
        _sceneCenterLatLonDataType = reader.readAn(1);

        _processingLevelLocator = (int) reader.readIn(6);
        _processingLevelDataStart = (int) reader.readIn(6);
        _processingLevelNumBytes = (int) reader.readIn(3);
        _processingLevelDataType = reader.readAn(1);

        _imageFormatLocator = (int) reader.readIn(6);
        _imageFormatDataStart = (int) reader.readIn(6);
        _imageFormatNumBytes = (int) reader.readIn(3);
        _imageFormatDataType = reader.readAn(1);

        _effektiveBandLocator = (int) reader.readIn(6);
        _effektiveBandDataStart = (int) reader.readIn(6);
        _effektiveBandNumBytes = (int) reader.readIn(3);
        _effektiveBandDataType = reader.readAn(1);
    }

    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
    }

    public int getNumSceneHeaderRecords() {
        return _numSceneHeaderRecords;
    }

    public int getSceneHeaderRecordLength() {
        return _sceneHeaderRecordLength;
    }

    public int getNumAncillaryRecords() {
        return _numAncillaryRecords;
    }

    public int getAncillaryRecordLength() {
        return _ancillaryRecordLength;
    }

    public int getSceneIdFieldLocator() {
        return _sceneIdFieldLocator;
    }

    public int getSceneIdFieldDataStart() {
        return _sceneIdFieldDataStart;
    }

    public int getSceneIdFieldNumBytes() {
        return _sceneIdFieldNumBytes;
    }

    public String getSceneIdFieldDataType() {
        return _sceneIdFieldDataType;
    }

    public int getRSPIdLocator() {
        return _RSPIdLocator;
    }

    public int getRSPIdDataStart() {
        return _RSPIdDataStart;
    }

    public int getRSPIdNumBytes() {
        return _RSPIdNumBytes;
    }

    public String getRSPIdDataType() {
        return _RSPIdDataType;
    }

    public int getMissionIdLocator() {
        return _missionIdLocator;
    }

    public int getMissionIdDataStart() {
        return _missionIdDataStart;
    }

    public int getMissionIdNumBytes() {
        return _missionIdNumBytes;
    }

    public String getMissionIdDataType() {
        return _missionIdDataType;
    }

    public int getSensorIdLocator() {
        return _sensorIdLocator;
    }

    public int getSensorIdDataStart() {
        return _sensorIdDataStart;
    }

    public int getSensorIdNumBytes() {
        return _sensorIdNumBytes;
    }

    public String getSensorIdDataType() {
        return _sensorIdDataType;
    }

    public int getSceneCenterTimeLocator() {
        return _sceneCenterTimeLocator;
    }

    public int getSceneCenterTimeDataStart() {
        return _sceneCenterTimeDataStart;
    }

    public int getSceneCenterTimeNumBytes() {
        return _sceneCenterTimeNumBytes;
    }

    public String getSceneCenterTimeDataType() {
        return _sceneCenterTimeDataType;
    }

    public int getSceneCenterLatLonLocator() {
        return _sceneCenterLatLonLocator;
    }

    public int getSceneCenterLatLonDataStart() {
        return _sceneCenterLatLonDataStart;
    }

    public int getSceneCenterLatLonNumBytes() {
        return _sceneCenterLatLonNumBytes;
    }

    public String getSceneCenterLatLonDataType() {
        return _sceneCenterLatLonDataType;
    }

    public int getProcessingLevelLocator() {
        return _processingLevelLocator;
    }

    public int getProcessingLevelDataStart() {
        return _processingLevelDataStart;
    }

    public int getProcessingLevelNumBytes() {
        return _processingLevelNumBytes;
    }

    public String getProcessingLevelDataType() {
        return _processingLevelDataType;
    }

    public int getImageFormatLocator() {
        return _imageFormatLocator;
    }

    public int getImageFormatDataStart() {
        return _imageFormatDataStart;
    }

    public int getImageFormatNumBytes() {
        return _imageFormatNumBytes;
    }

    public String getImageFormatDataType() {
        return _imageFormatDataType;
    }

    public int getEffektiveBandLocator() {
        return _effektiveBandLocator;
    }

    public int getEffektiveBandDataStart() {
        return _effektiveBandDataStart;
    }

    public int getEffektiveBandNumBytes() {
        return _effektiveBandNumBytes;
    }

    public String getEffektiveBandDataType() {
        return _effektiveBandDataType;
    }
}
