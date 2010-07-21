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
package org.esa.beam.dataio.ceos.prism.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseSceneHeaderRecord;

import java.io.IOException;

public class SceneHeaderRecord extends BaseSceneHeaderRecord {

    private String _compressionMode;
    private String _imageExtractionPoint;
    private String _flagYawSteering;

    public SceneHeaderRecord(final CeosFileReader reader) throws IOException,
                                                                 IllegalCeosFormatException {
        this(reader, -1);
    }

    public SceneHeaderRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                      IllegalCeosFormatException {
        super(reader, startPos);
    }

    @Override
    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
        reader.seek(getAbsolutPosition(388));
        _compressionMode = reader.readAn(1);
        reader.seek(getAbsolutPosition(1870));
        _imageExtractionPoint = reader.readAn(5);
        _flagYawSteering = reader.readAn(2);
    }

//    protected void readFields30To31(final CeosFileReader reader) throws IOException,
//                                                                        IllegalCeosFormatException {
//         Field 30
//        reader.skipBytes(16); // 16 x blank
//         Field 31
//        _compressionMode = reader.readAn(1);
//    }
//
//    protected void readField73ToEnd(final CeosFileReader reader) throws IOException,
//                                                                        IllegalCeosFormatException {
//         Field 73
//        _imageExtractionPoint = reader.readAn(5);
//         Field 74
//        _flagYawSteering = reader.readAn(2);
//         Field 75
//        reader.skipBytes(2803);
//
//    }

    @Override
    public String getYawSteeringFlag() {
        return _flagYawSteering;
    }

    public String getImageExtractionPoint() {
        return _imageExtractionPoint;
    }

    public String getCompressionMode() {
        return _compressionMode;
    }
}
