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
import java.util.Calendar;
import java.util.HashMap;

public abstract class BaseSceneHeaderRecord extends BaseRecord {

    private static final int YEAR_OFFSET = 2000;
    private static final HashMap MONTH_TABLE = new HashMap();

    static {
        MONTH_TABLE.put("Jan", 1);
        MONTH_TABLE.put("Feb", 2);
        MONTH_TABLE.put("Mar", 3);
        MONTH_TABLE.put("Apr", 4);
        MONTH_TABLE.put("May", 5);
        MONTH_TABLE.put("Jun", 6);
        MONTH_TABLE.put("Jul", 7);
        MONTH_TABLE.put("Aug", 8);
        MONTH_TABLE.put("Sep", 9);
        MONTH_TABLE.put("Oct", 10);
        MONTH_TABLE.put("Nov", 11);
        MONTH_TABLE.put("Dec", 12);
    }

    private int _headerRecordNumber;
    private String _productId;
    private String _uncorrectedSceneId;
    private double _sceneCenterLat_L1A_L1B1;
    private double _sceneCenterLon_L1A_L1B1;
    private double _sceneCenterLineNum_L1A_L1B1;
    private double _sceneCenterPixelNum_L1A_L1B1;
    private String _sceneCenterTime;
    private long _timeOffsetFromNominalRspCenter;
    private String _rspId;
    private long _orbitsPerCycle;
    private String _sceneID_L1B2;
    private double _sceneCenterLat_L1B2;
    private double _sceneCenterLon_L1B2;
    private double _sceneCenterLineNum_L1B2;
    private double _sceneCenterPixelNum_L1B2;
    private String _orientationAngle;
    private String _incidentAngle;
    private String _missionId;
    private String _sensorId;
    private long _calcOrbitNo;
    private String _orbitDirection;
    private String _dateImageWasTaken;
    private String _sceneCenterLonLat;
    private String _sensorTypeAndSpectrumBandIdentification;
    private String _sceneCenterSunAngle;
    private String _processingCode;
    private String _competentAgentAndProjectIdentification;
    private String _sceneId;
    private long _numEffectiveBands;
    private long _numPixelsPerLineInImage;
    private long _numLinesInImage;
    private long _radiometricResolution;
    private String _level1B2Option;
    private String _resamplingMethod;
    private String _mapProjectionMethod;
    private String _correctionLevel;
    private long _numMapProjAncillaryRecords;
    private long _numRadiometricAncillaryRecords;
    private int[] _effektiveBands;
    private String _imageFormat;
    private double _sceneCornerUpperLeftLat;
    private double _sceneCornerUpperLeftLon;
    private double _sceneCornerUpperRightLat;
    private double _sceneCornerUpperRightLon;
    private double _sceneCornerLowerLeftLat;
    private double _sceneCornerLowerLeftLon;
    private double _sceneCornerLowerRightLat;
    private double _sceneCornerLowerRightLon;
    private String _statusTimeSystem;
    private String _statusAbsoluteNavigation;
    private String _flagAttitudeDetermination;
    private String _accuracyUsedOrbitData;
    private String _accuracyUsedAttitudeData;

    public BaseSceneHeaderRecord(final CeosFileReader reader) throws IOException,
                                                                     IllegalCeosFormatException {
        this(reader, -1);
    }

    public BaseSceneHeaderRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                          IllegalCeosFormatException {
        super(reader, startPos);
        readGeneralFields(reader);
        reader.seek(getAbsolutPosition(getRecordLength()));
    }

    private void readGeneralFields(final CeosFileReader reader) throws IOException,
                                                                       IllegalCeosFormatException {
        _headerRecordNumber = reader.readI4();
        reader.skipBytes(4);    // blank
        _productId = reader.readAn(16);
        _uncorrectedSceneId = reader.readAn(16);
        _sceneCenterLat_L1A_L1B1 = reader.readFn(16);
        _sceneCenterLon_L1A_L1B1 = reader.readFn(16);
        _sceneCenterLineNum_L1A_L1B1 = reader.readFn(16);
        _sceneCenterPixelNum_L1A_L1B1 = reader.readFn(16);
        _sceneCenterTime = reader.readAn(32);
        _timeOffsetFromNominalRspCenter = reader.readIn(16);
        _rspId = reader.readAn(16);
        _orbitsPerCycle = reader.readIn(16);

        _sceneID_L1B2 = reader.readAn(16);
        _sceneCenterLat_L1B2 = reader.readFn(16);
        _sceneCenterLon_L1B2 = reader.readFn(16);
        _sceneCenterLineNum_L1B2 = reader.readFn(16);
        _sceneCenterPixelNum_L1B2 = reader.readFn(16);
        _orientationAngle = reader.readAn(16);
        _incidentAngle = reader.readAn(16);
        _missionId = reader.readAn(16);
        _sensorId = reader.readAn(16);
        _calcOrbitNo = reader.readIn(16);
        _orbitDirection = reader.readAn(16);

//        readFields30To31(reader);
        reader.skipBytes(28);

        _dateImageWasTaken = reader.readAn(8);
        _sceneCenterLonLat = reader.readAn(17);
        reader.skipBytes(17); // 17 x blank
        _sensorTypeAndSpectrumBandIdentification = reader.readAn(10);
        _sceneCenterSunAngle = reader.readAn(14);

        _processingCode = reader.readAn(12);
        _competentAgentAndProjectIdentification = reader.readAn(12);
        _sceneId = reader.readAn(16);
        reader.skipBytes(906); // 10 + 880 + 16 blanks

        _numEffectiveBands = reader.readIn(16);
        _numPixelsPerLineInImage = reader.readIn(16);
        _numLinesInImage = reader.readIn(16);
        reader.skipBytes(32); // 16 + 16 blanks
        _radiometricResolution = reader.readIn(16);
        reader.skipBytes(16); // 16 x blank
        _level1B2Option = reader.readAn(16);
        _resamplingMethod = reader.readAn(16);
        _mapProjectionMethod = reader.readAn(16);
        _correctionLevel = reader.readAn(16);
        _numMapProjAncillaryRecords = reader.readIn(16);
        _numRadiometricAncillaryRecords = reader.readIn(16);
        reader.skipBytes(32); // 32 x blank
        _effektiveBands = reader.readInArray(64, 1);

        _imageFormat = reader.readAn(16);
        _sceneCornerUpperLeftLat = reader.readFn(16);
        _sceneCornerUpperLeftLon = reader.readFn(16);
        _sceneCornerUpperRightLat = reader.readFn(16);
        _sceneCornerUpperRightLon = reader.readFn(16);
        _sceneCornerLowerLeftLat = reader.readFn(16);
        _sceneCornerLowerLeftLon = reader.readFn(16);
        _sceneCornerLowerRightLat = reader.readFn(16);
        _sceneCornerLowerRightLon = reader.readFn(16);
        _statusTimeSystem = reader.readAn(2);
        _statusAbsoluteNavigation = reader.readAn(2);
        _flagAttitudeDetermination = reader.readAn(2);
        _accuracyUsedOrbitData = reader.readAn(2);
        _accuracyUsedAttitudeData = reader.readAn(2);

//        readField73ToEnd(reader);

        readSpecificFields(reader);
    }

    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {

    }

    public int getHeaderRecordNumber() {
        return _headerRecordNumber;
    }

    public String getProductId() {
        return _productId;
    }

    public String getProductLevel() {
        String levelString = _productId.substring(1, 4);
        levelString = levelString.replace('_', ' ');
        return levelString.trim();
    }

    public String getUncorrectedSceneId() {
        return _uncorrectedSceneId;
    }

    public double getSceneCenterLat_L1A_L1B1() {
        return _sceneCenterLat_L1A_L1B1;
    }

    public double getSceneCenterLon_L1A_L1B1() {
        return _sceneCenterLon_L1A_L1B1;
    }

    public double getSceneCenterLineNum_L1A_L1B1() {
        return _sceneCenterLineNum_L1A_L1B1;
    }

    public double getSceneCenterPixelNum_L1A_L1B1() {
        return _sceneCenterPixelNum_L1A_L1B1;
    }

    public String getSceneCenterTime() {
        return _sceneCenterTime;
    }

    public long getTimeOffsetFromNominalRspCenter() {
        return _timeOffsetFromNominalRspCenter;
    }

    public String getRspId() {
        return _rspId;
    }

    public long getOrbitsPerCycle() {
        return _orbitsPerCycle;
    }

    public String getSceneID_L1B2() {
        return _sceneID_L1B2;
    }

    public double getSceneCenterLat_L1B2() {
        return _sceneCenterLat_L1B2;
    }

    public double getSceneCenterLon_L1B2() {
        return _sceneCenterLon_L1B2;
    }

    public double getSceneCenterLineNum_L1B2() {
        return _sceneCenterLineNum_L1B2;
    }

    public double getSceneCenterPixelNum_L1B2() {
        return _sceneCenterPixelNum_L1B2;
    }

    public String getOrientationAngle() {
        return _orientationAngle;
    }

    public String getIncidentAngle() {
        return _incidentAngle;
    }

    public String getMissionId() {
        return _missionId;
    }

    public String getSensorId() {
        return _sensorId;
    }

    public long getCalcOrbitNo() {
        return _calcOrbitNo;
    }

    public String getOrbitDirection() {
        return _orbitDirection;
    }

    public Calendar getDateImageWasTaken() {
        final Calendar calendar = Calendar.getInstance();
        final int days = Integer.parseInt(_dateImageWasTaken.substring(0, 2));
        final int month = (Integer) MONTH_TABLE.get(_dateImageWasTaken.substring(2, 5));
        final int year = Integer.parseInt(_dateImageWasTaken.substring(5, 7)) + YEAR_OFFSET;

        calendar.set(year, month - 1, days, 0, 0, 0);
        return calendar;
    }

    public String getSceneCenterLonLat() {
        return _sceneCenterLonLat;
    }

    public String getSensorTypeAndSpectrumBandIdentification() {
        return _sensorTypeAndSpectrumBandIdentification;
    }

    public String getSceneCenterSunAngle() {
        return _sceneCenterSunAngle;
    }

    public String getProcessingCode() {
        return _processingCode;
    }

    public String getCompetentAgentAndProjectIdentification() {
        return _competentAgentAndProjectIdentification;
    }

    public String getSceneId() {
        return _sceneId;
    }

    public long getNumEffectiveBands() {
        return _numEffectiveBands;
    }

    public long getNumPixelsPerLineInImage() {
        return _numPixelsPerLineInImage;
    }

    public long getNumLinesInImage() {
        return _numLinesInImage;
    }

    public long getRadiometricResolution() {
        return _radiometricResolution;
    }

    public String getLevel1B2Option() {
        return _level1B2Option;
    }

    public String getResamplingMethod() {
        return _resamplingMethod;
    }

    public String getMapProjectionMethod() {
        return _mapProjectionMethod;
    }

    public String getCorrectionLevel() {
        return _correctionLevel;
    }

    public long getNumMapProjAncillaryRecords() {
        return _numMapProjAncillaryRecords;
    }

    public long getNumRadiometricAncillaryRecords() {
        return _numRadiometricAncillaryRecords;
    }

    public int[] getEffektiveBands() {
        return _effektiveBands;
    }

    public String getImageFormat() {
        return _imageFormat;
    }

    public double getSceneCornerUpperLeftLat() {
        return _sceneCornerUpperLeftLat;
    }

    public double getSceneCornerUpperLeftLon() {
        return _sceneCornerUpperLeftLon;
    }

    public double getSceneCornerUpperRightLat() {
        return _sceneCornerUpperRightLat;
    }

    public double getSceneCornerUpperRightLon() {
        return _sceneCornerUpperRightLon;
    }

    public double getSceneCornerLowerLeftLat() {
        return _sceneCornerLowerLeftLat;
    }

    public double getSceneCornerLowerLeftLon() {
        return _sceneCornerLowerLeftLon;
    }

    public double getSceneCornerLowerRightLat() {
        return _sceneCornerLowerRightLat;
    }

    public double getSceneCornerLowerRightLon() {
        return _sceneCornerLowerRightLon;
    }

    public String getStatusTimeSystem() {
        return _statusTimeSystem;
    }

    public String getStatusAbsoluteNavigation() {
        return _statusAbsoluteNavigation;
    }

    public String getFlagAttitudeDetermination() {
        return _flagAttitudeDetermination;
    }

    public String getAccuracyUsedOrbitData() {
        return _accuracyUsedOrbitData;
    }

    public String getAccuracyUsedAttitudeData() {
        return _accuracyUsedAttitudeData;
    }

//    protected abstract void readFields30To31(final CeosFileReader reader) throws IOException,
//                                                                                 IllegalCeosFormatException;
//
//    protected abstract void readField73ToEnd(final CeosFileReader reader) throws IOException,
//                                                                                 IllegalCeosFormatException;

    public abstract String getYawSteeringFlag();
}
