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
package org.csa.rstb.io.radarsat1;

import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.s1tbx.io.ceos.CEOSLeaderFile;
import org.esa.s1tbx.io.ceos.CeosRecordHeader;
import org.jdom2.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


class RadarsatLeaderFile extends CEOSLeaderFile {

    private final static String mission = "radarsat";
    private final static String leader_recordDefinitionFile = "leader_file.xml";
    private final static String resourcePath = "org/csa/rstb/io/ceos_db/";

    private final static Document leaderXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, leader_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document sceneXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, scene_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document mapProjXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, mapproj_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document platformXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, platformPosition_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document attitudeXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, attitude_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document radiometricXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, radiometric_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document dataQualityXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, dataQuality_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document facilityXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, facility_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document histogramXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, histogram_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document detailProcXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, detailedProcessing_recordDefinitionFile, RadarsatLeaderFile.class);
    private final static Document radiometricCompXML = BinaryDBReader.loadDefinitionFile(resourcePath, mission, radiometric_comp_recordDefinitionFile, RadarsatLeaderFile.class);

    public RadarsatLeaderFile(final ImageInputStream stream) throws IOException {
        final BinaryFileReader reader = new BinaryFileReader(stream);

        CeosRecordHeader header = new CeosRecordHeader(reader);
        final Document leaderXML = loadDefinitionFile(mission, leader_recordDefinitionFile);
        leaderFDR = new BinaryRecord(reader, -1, leaderXML, leader_recordDefinitionFile);
        header.seekToEnd();


        while(header.getRecordLength() > 0) {
            header = new CeosRecordHeader(reader);
            switch(header.getRecordTypeCode()) {
                case 10:
                    sceneHeaderRecord = new BinaryRecord(reader, -1, sceneXML, scene_recordDefinitionFile);
                    break;
                case 60:
                    dataQualityRecord = new BinaryRecord(reader, -1, dataQualityXML, dataQuality_recordDefinitionFile);
                    break;
                case 70:
                    histogramRecord = new BinaryRecord(reader, -1, histogramXML, histogram_recordDefinitionFile);
                    break;
                case 120:
                    detailedProcessingRecord = new BinaryRecord(reader, -1, detailProcXML, detailedProcessing_recordDefinitionFile);
                    break;
                case 20:
                    mapProjRecord = new BinaryRecord(reader, -1, mapProjXML, mapproj_recordDefinitionFile);
                    break;
                case 30:
                    platformPositionRecord = new BinaryRecord(reader, -1, platformXML, platformPosition_recordDefinitionFile);
                    break;
                case 40:
                    attitudeRecord = new BinaryRecord(reader, -1, attitudeXML, attitude_recordDefinitionFile);
                    break;
                case 50:
                    radiometricRecord = new BinaryRecord(reader, -1, radiometricXML, radiometric_recordDefinitionFile);
                    break;
                case 51:
                    radiometricCompRecord = new BinaryRecord(reader, -1, radiometricCompXML, radiometric_comp_recordDefinitionFile);
                    break;
            }
            header.seekToEnd();
        }

        reader.close();
    }

    @Override
    public Document loadDefinitionFile(final String mission, final String fileName) {
        return BinaryDBReader.loadDefinitionFile(resourcePath, mission, fileName, this.getClass());
    }
}
