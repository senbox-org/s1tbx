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
package org.esa.nest.dataio.ceos.ers;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.nest.dataio.binary.BinaryDBReader;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;
import org.esa.nest.dataio.ceos.CeosHelper;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * This class represents a leader file of a product.
 *
 */
class ERSLeaderFile {

    private final BinaryRecord _leaderFDR;
    private final BinaryRecord _sceneHeaderRecord;
    private final BinaryRecord _mapProjRecord;
    private final BinaryRecord _platformPositionRecord;
    private final BinaryRecord _facilityRecord;
    private final BinaryRecord _facilityRelatedPCSRecord;

    private final static String mission = "ers";
    private final static String leader_recordDefinitionFile = "leader_file.xml";
    private final static String scene_recordDefinitionFile = "scene_record.xml";
    private final static String mapproj_recordDefinitionFile = "map_proj_record.xml";
    private final static String platform_recordDefinitionFile = "platform_position_record.xml";
    private final static String facility_recordDefinitionFile = "facility_record.xml";
    private final static String facilityRelatedPCS_recordDefinitionFile = "facility_related_pcs_record.xml";

    private final static org.jdom.Document leaderXML = BinaryDBReader.loadDefinitionFile(mission, leader_recordDefinitionFile);
    private final static org.jdom.Document sceneXML = BinaryDBReader.loadDefinitionFile(mission, scene_recordDefinitionFile);
    private final static org.jdom.Document mapProjXML = BinaryDBReader.loadDefinitionFile(mission, mapproj_recordDefinitionFile);
    private final static org.jdom.Document platformXML = BinaryDBReader.loadDefinitionFile(mission, platform_recordDefinitionFile);
    private final static org.jdom.Document facilityXML = BinaryDBReader.loadDefinitionFile(mission, facility_recordDefinitionFile);
    private final static org.jdom.Document facilityRelXML = BinaryDBReader.loadDefinitionFile(mission, facilityRelatedPCS_recordDefinitionFile);

    public ERSLeaderFile(final ImageInputStream leaderStream)
            throws IOException {

        final BinaryFileReader reader = new BinaryFileReader(leaderStream);
        _leaderFDR = new BinaryRecord(reader, -1, leaderXML, leader_recordDefinitionFile);
        reader.seek(_leaderFDR.getRecordEndPosition());
        _sceneHeaderRecord = new BinaryRecord(reader, -1, sceneXML, scene_recordDefinitionFile);
        reader.seek(_sceneHeaderRecord.getRecordEndPosition());
        _mapProjRecord = new BinaryRecord(reader, -1, mapProjXML, mapproj_recordDefinitionFile);
        reader.seek(_mapProjRecord.getRecordEndPosition());
        _platformPositionRecord = new BinaryRecord(reader, -1, platformXML, platform_recordDefinitionFile);
        reader.seek(_platformPositionRecord.getRecordEndPosition());
        _facilityRecord = new BinaryRecord(reader, -1, facilityXML, facility_recordDefinitionFile);
        reader.seek(_facilityRecord.getRecordEndPosition());
        if(reader.getCurrentPos() + 4000 < reader.getLength()) {
            _facilityRelatedPCSRecord = new BinaryRecord(reader, -1, facilityRelXML, facilityRelatedPCS_recordDefinitionFile);
            reader.seek(_facilityRelatedPCSRecord.getRecordEndPosition());
        } else {
            _facilityRelatedPCSRecord = null;
        }
        reader.close();
    }

    public String getProductLevel() {
        return _sceneHeaderRecord.getAttributeString("Scene reference number").trim();
    }

    public final BinaryRecord getSceneRecord() {
        return _sceneHeaderRecord;
    }

    public final BinaryRecord getFacilityRecord() {
        return _facilityRecord;
    }

    public final BinaryRecord getMapProjRecord() {
        return _mapProjRecord;
    }

    public final BinaryRecord getPlatformPositionRecord() {
        return _platformPositionRecord;
    }

    public void addLeaderMetadata(MetadataElement sphElem) {

        CeosHelper.addMetadata(sphElem, _leaderFDR, "Leader File Descriptor");
        CeosHelper.addMetadata(sphElem, _sceneHeaderRecord, "Scene Parameters");
        CeosHelper.addMetadata(sphElem, _mapProjRecord, "Map Projection");
        CeosHelper.addMetadata(sphElem, _platformPositionRecord, "Platform Position");
        CeosHelper.addMetadata(sphElem, _facilityRecord, "Facility Related");
        CeosHelper.addMetadata(sphElem, _facilityRelatedPCSRecord, "Facility Related PCS");
    }
}
