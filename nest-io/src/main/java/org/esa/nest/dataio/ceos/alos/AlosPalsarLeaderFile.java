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
package org.esa.nest.dataio.ceos.alos;

import org.esa.nest.dataio.binary.BinaryDBReader;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;
import org.esa.nest.dataio.ceos.CEOSLeaderFile;
import org.esa.nest.dataio.ceos.CeosRecordHeader;
import org.jdom.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * This class represents a leader file of a product.
 *
 */
class AlosPalsarLeaderFile extends CEOSLeaderFile {

    protected final static String mission = "alos";
    private final static String leader_recordDefinitionFile = "leader_file.xml";

    private int productLevel = -1;

    private final static String facility_record1_5DefinitionFile = "facility_record1_5.xml";

    private final static Document leaderXML = BinaryDBReader.loadDefinitionFile(mission, leader_recordDefinitionFile);
    private final static Document sceneXML = BinaryDBReader.loadDefinitionFile(mission, scene_recordDefinitionFile);
    private final static Document mapProjXML = BinaryDBReader.loadDefinitionFile(mission, mapproj_recordDefinitionFile);
    private final static Document platformXML = BinaryDBReader.loadDefinitionFile(mission, platformPosition_recordDefinitionFile);
    private final static Document attitudeXML = BinaryDBReader.loadDefinitionFile(mission, attitude_recordDefinitionFile);
    private final static Document radiometricXML = BinaryDBReader.loadDefinitionFile(mission, radiometric_recordDefinitionFile);
    private final static Document dataQualityXML = BinaryDBReader.loadDefinitionFile(mission, dataQuality_recordDefinitionFile);
    private final static Document facilityXML = BinaryDBReader.loadDefinitionFile(mission, facility_recordDefinitionFile);
    private final static Document facility1_5XML = BinaryDBReader.loadDefinitionFile(mission, facility_record1_5DefinitionFile);

    public AlosPalsarLeaderFile(final ImageInputStream stream) throws IOException {
        this(stream, leaderXML);
    }

    public AlosPalsarLeaderFile(final ImageInputStream stream, final Document fdrXML) throws IOException {

        final BinaryFileReader reader = new BinaryFileReader(stream);

        CeosRecordHeader header = new CeosRecordHeader(reader);
        _leaderFDR = new BinaryRecord(reader, -1, fdrXML, leader_recordDefinitionFile);
        header.seekToEnd();

        for(int i=0; i < _leaderFDR.getAttributeInt("Number of data set summary records"); ++i) {
            header = new CeosRecordHeader(reader);
            _sceneHeaderRecord = new BinaryRecord(reader, -1, sceneXML, scene_recordDefinitionFile);
            header.seekToEnd();
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of map projection data records"); ++i) {
            try {
                header = new CeosRecordHeader(reader);
                _mapProjRecord = new BinaryRecord(reader, -1, mapProjXML, mapproj_recordDefinitionFile);
                header.seekToEnd();
             } catch(Exception e) {
                System.out.println("unable to read projection");
            }
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of platform pos. data records"); ++i) {
            try {
                header = new CeosRecordHeader(reader);
                _platformPositionRecord = new BinaryRecord(reader, -1, platformXML, platformPosition_recordDefinitionFile);
                header.seekToEnd();
            } catch(Exception e) {
                System.out.println("unable to read platform pos");
            }
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of attitude data records"); ++i) {
            try {
                header = new CeosRecordHeader(reader);
                _attitudeRecord = new BinaryRecord(reader, -1, attitudeXML, attitude_recordDefinitionFile);
                header.seekToEnd();
            } catch(Exception e) {
                System.out.println("unable to read attitude");
            }
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of radiometric data records"); ++i) {
            try {
                header = new CeosRecordHeader(reader);
                _radiometricRecord = new BinaryRecord(reader, -1, radiometricXML, radiometric_recordDefinitionFile);
                header.seekToEnd();
            } catch(Exception e) {
                System.out.println("unable to read radiometric");
            }
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of data quality summary records"); ++i) {
            try {
                header = new CeosRecordHeader(reader);
                _dataQualityRecord = new BinaryRecord(reader, -1, dataQualityXML, dataQuality_recordDefinitionFile);
                header.seekToEnd();
            } catch(Exception e) {
                System.out.println("unable to read quality");
            }
        }
        for(int i=0; i < _leaderFDR.getAttributeInt("Number of facility data records"); ++i) {
            try {
                header = new CeosRecordHeader(reader);
                int level = getProductLevel();
                if(level == AlosPalsarConstants.LEVEL1_0 || level == AlosPalsarConstants.LEVEL1_1) {
                    int facilityRecordNum = 17;

                    while(header.getRecordNum() < facilityRecordNum && header.getRecordLength() > 0) {
                        header.seekToEnd();
                        header = new CeosRecordHeader(reader);
                    }

                    _facilityRecord = new BinaryRecord(reader, -1, facilityXML, facility_recordDefinitionFile);
                    header.seekToEnd();
                } else {
                    _facilityRecord = new BinaryRecord(reader, -1, facility1_5XML, facility_record1_5DefinitionFile);
                    header.seekToEnd();
                }
            } catch(Exception e) {
                System.out.println("Unable to read ALOS facility record: "+e.getMessage());
            }
        }
        reader.close();

        if(getProductLevel() == AlosPalsarConstants.LEVEL1_0)
            throw new IOException("ALOS L0 products are not supported");
    }

    public final int getProductLevel() {
        if(productLevel < 0) {
            String level = null;
            if(_sceneHeaderRecord != null) {
                level = _sceneHeaderRecord.getAttributeString("Product level code");
            }
            if(level != null) {
                if(level.contains("1.5"))
                    productLevel = AlosPalsarConstants.LEVEL1_5;
                else if(level.contains("1.1"))
                    productLevel = AlosPalsarConstants.LEVEL1_1;
                else if(level.contains("1.0"))
                    productLevel = AlosPalsarConstants.LEVEL1_0;
                else if(level.contains("4.1"))
                    productLevel = AlosPalsarConstants.LEVEL4_1;
                else if(level.contains("4.2"))
                    productLevel = AlosPalsarConstants.LEVEL4_2;
            }
        }
        return productLevel;
    }

    public String getProductType() {
        return _sceneHeaderRecord.getAttributeString("Product type specifier");
    }
}
