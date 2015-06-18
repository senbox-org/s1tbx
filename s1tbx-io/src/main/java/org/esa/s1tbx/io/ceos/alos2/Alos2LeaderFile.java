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
package org.esa.s1tbx.io.ceos.alos2;

import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.s1tbx.io.ceos.CeosRecordHeader;
import org.esa.s1tbx.io.ceos.alos.AlosPalsarConstants;
import org.esa.s1tbx.io.ceos.alos.AlosPalsarLeaderFile;
import org.jdom2.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * This class represents a leader file of a product.
 */
public class Alos2LeaderFile extends AlosPalsarLeaderFile {

    protected final static String mission = "alos2";
    private final static String leader_recordDefinitionFile = "leader_file.xml";

    private final static String facility_record1_5DefinitionFile = "facility_record1_5.xml";

    private final static Document leaderXML = BinaryDBReader.loadDefinitionFile(mission, leader_recordDefinitionFile);
    private final static Document facilityXML = BinaryDBReader.loadDefinitionFile(mission, facility_recordDefinitionFile);
    private final static Document facility1_5XML = BinaryDBReader.loadDefinitionFile(mission, facility_record1_5DefinitionFile);

    public Alos2LeaderFile(final ImageInputStream stream) throws IOException {
        super(stream, leaderXML);
    }

    protected void readFacilityRelatedRecords(final BinaryFileReader reader) {
        for (int i = 0; i < _leaderFDR.getAttributeInt("Number of facility data records"); ++i) {
            try {
                CeosRecordHeader header = new CeosRecordHeader(reader);
                int level = getProductLevel();
                if (level == AlosPalsarConstants.LEVEL1_0 || level == AlosPalsarConstants.LEVEL1_1) {
                    int facilityRecordNum = 11;

                    while (header.getRecordNum() < facilityRecordNum && header.getRecordLength() > 0) {
                        header.seekToEnd();
                        header = new CeosRecordHeader(reader);
                    }

                    _facilityRecord = new BinaryRecord(reader, -1, facilityXML, facility_recordDefinitionFile);
                    header.seekToEnd();
                } else {
                    _facilityRecord = new BinaryRecord(reader, -1, facility1_5XML, facility_record1_5DefinitionFile);
                    header.seekToEnd();
                }
            } catch (Exception e) {
                System.out.println("Unable to read ALOS facility record: " + e.getMessage());
            }
        }
    }
}
