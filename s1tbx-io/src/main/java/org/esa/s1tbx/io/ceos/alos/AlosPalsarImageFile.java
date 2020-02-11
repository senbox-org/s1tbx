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
package org.esa.s1tbx.io.ceos.alos;

import org.esa.s1tbx.io.binary.BinaryDBReader;
import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.s1tbx.io.ceos.CEOSImageFile;
import org.jdom2.Document;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


public class AlosPalsarImageFile extends CEOSImageFile {

    private final static String mission = "alos";
    private final static String image_DefinitionFile = "image_file.xml";
    private final static String image_recordDefinition = "image_record.xml";
    private final static String processedData_recordDefinition = "processed_data_record.xml";
    private final String imageFileName;
    private final int productLevel;

    private final static Document imgDefXML = BinaryDBReader.loadDefinitionFile(mission, image_DefinitionFile);
    private final static Document imgRecordXML = BinaryDBReader.loadDefinitionFile(mission, image_recordDefinition);
    private final static Document procDataXML = BinaryDBReader.loadDefinitionFile(mission, processedData_recordDefinition);

    int startYear;
    int startDay;
    int startMsec;
    int endYear;
    int endDay;
    int endMsec;
    boolean isProductIPF = false;

    public AlosPalsarImageFile(final ImageInputStream imageStream, int prodLevel, String fileName)
            throws IOException {
        if (prodLevel < 0) {  // To avoid perturbing the AlosPalsarImageFile interface definition, a negative prodLevel is used
            // to signify a product of IPF origin to this object - relevant because the L1.1 PDR header structure
            // is different between JAXA and IPF - the IPF uses a PDR header structure.
            isProductIPF = true;
            prodLevel = -prodLevel;
        }
        this.productLevel = prodLevel;
        imageFileName = fileName.toUpperCase();

        binaryReader = new BinaryFileReader(imageStream);
        imageFDR = new BinaryRecord(binaryReader, -1, imgDefXML, image_DefinitionFile);
        binaryReader.seek(imageFDR.getAbsolutPosition(imageFDR.getRecordLength()));
        int numRecs = imageFDR.getAttributeInt("Number of lines per data set");

        imageRecords = new BinaryRecord[imageFDR.getAttributeInt("Number of lines per data set")];
        if (imageRecords.length > 0) {
            imageRecords[0] = createNewImageRecord(0);
            _imageRecordLength = imageRecords[0].getRecordLength(); //get the PDR record length
            imageRecords[numRecs - 1] = createNewImageRecord(numRecs - 1); //read the last record (to access timing info)
            startPosImageRecords = imageRecords[0].getStartPos();
            startYear = imageRecords[0].getAttributeInt("Sensor acquisition year");
            startDay = imageRecords[0].getAttributeInt("Sensor acquisition day of year");
            startMsec = imageRecords[0].getAttributeInt("Sensor acquisition milliseconds of day");
            int startRange = imageRecords[0].getAttributeInt("Slant range to 1st pixel");

            endYear = imageRecords[numRecs - 1].getAttributeInt("Sensor acquisition year");
            endDay = imageRecords[numRecs - 1].getAttributeInt("Sensor acquisition day of year");
            endMsec = imageRecords[numRecs - 1].getAttributeInt("Sensor acquisition milliseconds of day");
        }
        imageHeaderLength = imageFDR.getAttributeInt("Number of bytes of prefix data per record");
    }

    boolean isIPF() {
        return isProductIPF;
    }

    protected BinaryRecord createNewImageRecord(int line) throws IOException {
        long pos = imageFDR.getAbsolutPosition(imageFDR.getRecordLength()) + (line * _imageRecordLength);
        if ((productLevel == AlosPalsarConstants.LEVEL1_5) || isIPF()) //IPF 1.1 SLC has PDR header structure
            return new BinaryRecord(binaryReader, pos, procDataXML, processedData_recordDefinition);
        else
            return new BinaryRecord(binaryReader, pos, imgRecordXML, image_recordDefinition);
    }

    public String getPolarization() {
        if (imageFileName.startsWith("IMG-") && imageFileName.length() > 6) {

            String pol = imageFileName.substring(4, 6);
            String pol1 = imageFileName.substring(4, 9);
            if (pol1.equals("HH+VV") || pol1.equals("HH-VV") || pol1.equals("HV+VH") || pol1.equals("HV-VH") ) {
                switch (pol1) {
                    case "HH+VV":
                        return "HHplusVV";
                    case "HH-VV":
                        return "HHminusVV";
                    case "HV+VH":
                        return "HVplusVH";
                    default:
                        return "HVminusVH";
                }
            } else if (pol.equals("HH") || pol.equals("VV") || pol.equals("HV") || pol.equals("VH")) {
                return pol;
            } else if (imageRecords[0] != null) {
                try {
                    final int tx = imageRecords[0].getAttributeInt("Transmitted polarization");
                    final int rx = imageRecords[0].getAttributeInt("Received polarization");
                    if (tx == 1)
                        pol = "V";
                    else
                        pol = "H";

                    if (rx == 1)
                        pol += "V";
                    else
                        pol += "H";

                    return pol;
                } catch (Exception e) {
                    return "";
                }
            }
        }
        return "";
    }
}
