/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos.radarsat;

import org.esa.nest.dataio.binary.BinaryDBReader;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;
import org.esa.nest.dataio.ceos.CEOSLeaderFile;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


class RadarsatTrailerFile extends CEOSLeaderFile {

    private final static String mission = "radarsat";
    private final static String trailer_recordDefinitionFile = "trailer_file.xml";

    private final static org.jdom.Document trailerXML = BinaryDBReader.loadDefinitionFile(mission, trailer_recordDefinitionFile);
    private final static org.jdom.Document sceneXML = BinaryDBReader.loadDefinitionFile(mission, scene_recordDefinitionFile);
    private final static org.jdom.Document mapProjXML = BinaryDBReader.loadDefinitionFile(mission, mapproj_recordDefinitionFile);
    private final static org.jdom.Document dataQualityXML = BinaryDBReader.loadDefinitionFile(mission, dataQuality_recordDefinitionFile);
    private final static org.jdom.Document histogramXML = BinaryDBReader.loadDefinitionFile(mission, histogram_recordDefinitionFile);
    private final static org.jdom.Document detailProcXML = BinaryDBReader.loadDefinitionFile(mission, detailedProcessing_recordDefinitionFile);
    private final static org.jdom.Document platformXML = BinaryDBReader.loadDefinitionFile(mission, platformPosition_recordDefinitionFile);
    private final static org.jdom.Document attitudeXML = BinaryDBReader.loadDefinitionFile(mission, attitude_recordDefinitionFile);
    private final static org.jdom.Document radiometricXML = BinaryDBReader.loadDefinitionFile(mission, radiometric_recordDefinitionFile);
    private final static org.jdom.Document radiometricCompXML = BinaryDBReader.loadDefinitionFile(mission, radiometric_comp_recordDefinitionFile);
    private final static org.jdom.Document facilityXML = BinaryDBReader.loadDefinitionFile(mission, facility_recordDefinitionFile);

    public RadarsatTrailerFile(final ImageInputStream stream) throws IOException {
        final BinaryFileReader reader = new BinaryFileReader(stream);

        _leaderFDR = new BinaryRecord(reader, -1, trailerXML, trailer_recordDefinitionFile);
        reader.seek(_leaderFDR.getRecordEndPosition());
        int num = _leaderFDR.getAttributeInt("Number of data set summary records");
        for(int i=0; i < num; ++i) {
            _sceneHeaderRecord = new BinaryRecord(reader, -1, sceneXML, scene_recordDefinitionFile);
            reader.seek(_sceneHeaderRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of map projection data records");
        for(int i=0; i < num; ++i) {
            _mapProjRecord = new BinaryRecord(reader, -1, mapProjXML, mapproj_recordDefinitionFile);
            reader.seek(_mapProjRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of data quality summary records");
        for(int i=0; i < num; ++i) {
            _dataQualityRecord = new BinaryRecord(reader, -1, dataQualityXML, dataQuality_recordDefinitionFile);
            reader.seek(_dataQualityRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of data histograms records");
        for(int i=0; i < num; ++i) {
            _histogramRecord = new BinaryRecord(reader, -1, histogramXML, histogram_recordDefinitionFile);
            reader.seek(_histogramRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of det. processing records");
        for(int i=0; i < num; ++i) {
            _detailedProcessingRecord = new BinaryRecord(reader, -1, detailProcXML, detailedProcessing_recordDefinitionFile);
            reader.seek(_detailedProcessingRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of platform pos. data records");
        for(int i=0; i < num; ++i) {
            _platformPositionRecord = new BinaryRecord(reader, -1, platformXML, platformPosition_recordDefinitionFile);
            reader.seek(_platformPositionRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of attitude data records");
        for(int i=0; i < num; ++i) {
            _attitudeRecord = new BinaryRecord(reader, -1, attitudeXML, attitude_recordDefinitionFile);
            reader.seek(_attitudeRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of radiometric data records");
        for(int i=0; i < num; ++i) {
            _radiometricRecord = new BinaryRecord(reader, -1, radiometricXML, radiometric_recordDefinitionFile);
            reader.seek(_radiometricRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of rad. compensation records");
        for(int i=0; i < num; ++i) {
            _radiometricCompRecord = new BinaryRecord(reader, -1, radiometricCompXML, radiometric_comp_recordDefinitionFile);
            reader.seek(_radiometricCompRecord.getRecordEndPosition());
        }
        num = _leaderFDR.getAttributeInt("Number of facility data records");
        for(int i=0; i < num; ++i) {
            _facilityRecord = new BinaryRecord(reader, -1, facilityXML, facility_recordDefinitionFile);
            reader.seek(_facilityRecord.getRecordEndPosition());
        }

        reader.close();
    }
}