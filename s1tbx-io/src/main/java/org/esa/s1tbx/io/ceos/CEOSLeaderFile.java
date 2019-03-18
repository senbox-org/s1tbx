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
import org.jdom2.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


public class CEOSLeaderFile implements CEOSFile {

    protected BinaryRecord leaderFDR = null;
    protected BinaryRecord sceneHeaderRecord = null;
    protected BinaryRecord platformPositionRecord = null;
    protected BinaryRecord mapProjRecord = null;
    protected BinaryRecord dataQualityRecord = null;
    protected BinaryRecord histogramRecord = null;
    protected BinaryRecord attitudeRecord = null;
    protected BinaryRecord radiometricRecord = null;
    protected BinaryRecord radiometricCompRecord = null;
    protected BinaryRecord detailedProcessingRecord = null;
    protected BinaryRecord facilityRecord = null;

    protected final static String scene_recordDefinitionFile = "scene_record.xml";
    protected final static String platformPosition_recordDefinitionFile = "platform_position_record.xml";
    protected final static String mapproj_recordDefinitionFile = "map_proj_record.xml";
    protected final static String dataQuality_recordDefinitionFile = "data_quality_summary_record.xml";
    protected final static String histogram_recordDefinitionFile = "data_histogram_record.xml";
    protected final static String attitude_recordDefinitionFile = "attitude_record.xml";
    protected final static String radiometric_recordDefinitionFile = "radiometric_record.xml";
    protected final static String radiometric_comp_recordDefinitionFile = "radiometric_compensation_record.xml";
    protected final static String detailedProcessing_recordDefinitionFile = "detailed_processing_record.xml";
    protected final static String facility_recordDefinitionFile = "facility_record.xml";

    private Document sceneXML;
    private Document mapProjXML;
    private Document platformXML;
    private Document attitudeXML;
    private Document radiometricXML;
    private Document radiometricCompXML;
    private Document dataQualityXML;
    private Document histogramXML;
    private Document detailProcXML;
    private Document facilityXML;

    protected CEOSLeaderFile() {
    }

    protected CEOSLeaderFile(final ImageInputStream stream, final String mission, final String defnFile)
            throws IOException {
        final BinaryFileReader reader = new BinaryFileReader(stream);

        final Document leaderXML = loadDefinitionFile(mission, defnFile);
        leaderFDR = new BinaryRecord(reader, -1, leaderXML, defnFile);
        reader.seek(leaderFDR.getRecordEndPosition());

        int num = leaderFDR.getAttributeInt("Number of data set summary records");
        if (sceneXML == null && num > 0) {
            sceneXML = loadDefinitionFile(mission, scene_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                sceneHeaderRecord = new BinaryRecord(reader, -1, sceneXML, scene_recordDefinitionFile);
                reader.seek(sceneHeaderRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of map projection data records");
        if (mapProjXML == null && num > 0) {
            mapProjXML = loadDefinitionFile(mission, mapproj_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                mapProjRecord = new BinaryRecord(reader, -1, mapProjXML, mapproj_recordDefinitionFile);
                reader.seek(mapProjRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of platform pos. data records");
        if (platformXML == null && num > 0) {
            platformXML = loadDefinitionFile(mission, platformPosition_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                platformPositionRecord = new BinaryRecord(reader, -1, platformXML, platformPosition_recordDefinitionFile);
                reader.seek(platformPositionRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of attitude data records");
        if (attitudeXML == null && num > 0) {
            attitudeXML = loadDefinitionFile(mission, attitude_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                attitudeRecord = new BinaryRecord(reader, -1, attitudeXML, attitude_recordDefinitionFile);
                reader.seek(attitudeRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of radiometric data records");
        if (radiometricXML == null && num > 0) {
            radiometricXML = loadDefinitionFile(mission, radiometric_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                radiometricRecord = new BinaryRecord(reader, -1, radiometricXML, radiometric_recordDefinitionFile);
                reader.seek(radiometricRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of rad. compensation records");
        if (radiometricCompXML == null && num > 0) {
            radiometricCompXML = loadDefinitionFile(mission, radiometric_comp_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                radiometricCompRecord = new BinaryRecord(reader, -1, radiometricCompXML, radiometric_comp_recordDefinitionFile);
                reader.seek(radiometricCompRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of data quality summary records");
        if (dataQualityXML == null && num > 0) {
            dataQualityXML = loadDefinitionFile(mission, dataQuality_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                dataQualityRecord = new BinaryRecord(reader, -1, dataQualityXML, dataQuality_recordDefinitionFile);
                reader.seek(dataQualityRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of data histograms records");
        if (histogramXML == null && num > 0) {
            histogramXML = loadDefinitionFile(mission, histogram_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                histogramRecord = new BinaryRecord(reader, -1, histogramXML, histogram_recordDefinitionFile);
                reader.seek(histogramRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of det. processing records");
        if (detailProcXML == null && num > 0) {
            detailProcXML = loadDefinitionFile(mission, detailedProcessing_recordDefinitionFile);

            for (int i = 0; i < num; ++i) {
                detailedProcessingRecord = new BinaryRecord(reader, -1, detailProcXML, detailedProcessing_recordDefinitionFile);
                reader.seek(detailedProcessingRecord.getRecordEndPosition());
            }
        }

        num = leaderFDR.getAttributeInt("Number of facility data records");
        if (facilityXML == null && num > 0) {
            facilityXML = loadDefinitionFile(mission, facility_recordDefinitionFile);
            for (int i = 0; i < num; ++i) {
                facilityRecord = new BinaryRecord(reader, -1, facilityXML, facility_recordDefinitionFile);
                reader.seek(facilityRecord.getRecordEndPosition());
            }
        }

        reader.close();
    }

    public final BinaryRecord getSceneRecord() {
        return sceneHeaderRecord;
    }

    public BinaryRecord getMapProjRecord() {
        return mapProjRecord;
    }

    public BinaryRecord getPlatformPositionRecord() {
        return platformPositionRecord;
    }

    public BinaryRecord getHistogramRecord() {
        return histogramRecord;
    }

    public BinaryRecord getRadiometricRecord() {
        return radiometricRecord;
    }

    public BinaryRecord getFacilityRecord() {
        return facilityRecord;
    }

    public BinaryRecord getDetailedProcessingRecord() {
        return detailedProcessingRecord;
    }

    public static float[] getLatCorners(final BinaryRecord mapProjRec) {
        if (mapProjRec == null) return null;

        final Double latUL = mapProjRec.getAttributeDouble("1st line 1st pixel geodetic latitude");
        final Double latUR = mapProjRec.getAttributeDouble("1st line last valid pixel geodetic latitude");
        final Double latLL = mapProjRec.getAttributeDouble("Last line 1st pixel geodetic latitude");
        final Double latLR = mapProjRec.getAttributeDouble("Last line last valid pixel geodetic latitude");
        if (latUL == null || latUR == null || latLL == null || latLR == null)
            return null;
        return new float[]{latUL.floatValue(), latUR.floatValue(), latLL.floatValue(), latLR.floatValue()};
    }

    public static float[] getLonCorners(final BinaryRecord mapProjRec) {
        if (mapProjRec == null) return null;

        final Double lonUL = mapProjRec.getAttributeDouble("1st line 1st pixel geodetic longitude");
        final Double lonUR = mapProjRec.getAttributeDouble("1st line last valid pixel geodetic longitude");
        final Double lonLL = mapProjRec.getAttributeDouble("Last line 1st pixel geodetic longitude");
        final Double lonLR = mapProjRec.getAttributeDouble("Last line last valid pixel geodetic longitude");
        if (lonUL == null || lonUR == null || lonLL == null || lonLR == null)
            return null;
        return new float[]{lonUL.floatValue(), lonUR.floatValue(), lonLL.floatValue(), lonLR.floatValue()};
    }

    public void addMetadata(MetadataElement sphElem) {

        CeosHelper.addMetadata(sphElem, leaderFDR, "File Descriptor");
        CeosHelper.addMetadata(sphElem, sceneHeaderRecord, "Scene Parameters");
        CeosHelper.addMetadata(sphElem, mapProjRecord, "Map Projection");
        CeosHelper.addMetadata(sphElem, platformPositionRecord, "Platform Position");
        CeosHelper.addMetadata(sphElem, dataQualityRecord, "Data Quality");
        CeosHelper.addMetadata(sphElem, histogramRecord, "Histogram");
        CeosHelper.addMetadata(sphElem, attitudeRecord, "Attitude");
        CeosHelper.addMetadata(sphElem, radiometricRecord, "Radiometric");
        CeosHelper.addMetadata(sphElem, radiometricCompRecord, "Radiometric Compensation");
        CeosHelper.addMetadata(sphElem, detailedProcessingRecord, "Detailed Processing");
        CeosHelper.addMetadata(sphElem, facilityRecord, "Facility Related");
    }
}
