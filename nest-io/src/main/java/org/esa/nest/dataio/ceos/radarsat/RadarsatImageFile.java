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
import org.esa.nest.dataio.binary.IllegalBinaryFormatException;
import org.esa.nest.dataio.ceos.CEOSImageFile;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;


/**
 * This class represents an image file of a CEOS product.

 */
class RadarsatImageFile extends CEOSImageFile {

    private final static String mission = "radarsat";
    private final static String image_recordDefinitionFile = "image_file.xml";
    private final static String image_recordDefinition = "image_record.xml";

    private final static org.jdom.Document imgDefXML = BinaryDBReader.loadDefinitionFile(mission, image_recordDefinitionFile);
    private final static org.jdom.Document imgRecordXML = BinaryDBReader.loadDefinitionFile(mission, image_recordDefinition);

    public RadarsatImageFile(final ImageInputStream imageStream, final BinaryRecord histogramRecord)
            throws IOException, IllegalBinaryFormatException {
        binaryReader = new BinaryFileReader(imageStream);
        _imageFDR = new BinaryRecord(binaryReader, -1, imgDefXML, image_recordDefinitionFile);
        binaryReader.seek(_imageFDR.getAbsolutPosition(_imageFDR.getRecordLength()));
        if(getRasterHeight() == 0) {
            final int height = histogramRecord.getAttributeInt("Data samples in line");
            _imageFDR.getBinaryDatabase().set("Number of lines per data set", height);
        }
        _imageRecords = new BinaryRecord[getRasterHeight()];
        _imageRecords[0] = createNewImageRecord(0);

        _imageRecordLength = _imageRecords[0].getRecordLength();
        _startPosImageRecords = _imageRecords[0].getStartPos();
        _imageHeaderLength = _imageFDR.getAttributeInt("Number of bytes of prefix data per record");
    }

    protected BinaryRecord createNewImageRecord(final int line) throws IOException {
        final long pos = _imageFDR.getAbsolutPosition(_imageFDR.getRecordLength()) + (line*_imageRecordLength);
        return new BinaryRecord(binaryReader, pos, imgRecordXML, image_recordDefinition);
    }
}