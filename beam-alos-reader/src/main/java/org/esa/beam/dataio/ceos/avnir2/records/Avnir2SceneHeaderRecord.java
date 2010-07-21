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
package org.esa.beam.dataio.ceos.avnir2.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseSceneHeaderRecord;

import java.io.IOException;

public class Avnir2SceneHeaderRecord extends BaseSceneHeaderRecord {

    private String _offNadirMirrorPointAngle;
    private String _yawSteeringFlag;

    public Avnir2SceneHeaderRecord(final CeosFileReader reader) throws IOException,
                                                                       IllegalCeosFormatException {
        this(reader, -1);
    }

    public Avnir2SceneHeaderRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                            IllegalCeosFormatException {
        super(reader, startPos);
    }

    @Override
    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
        reader.seek(getAbsolutPosition(372));
        _offNadirMirrorPointAngle = reader.readAn(16);
        reader.seek(getAbsolutPosition(1870));
        _yawSteeringFlag = reader.readAn(2);
    }

//    protected void readFields30To31(final CeosFileReader reader) throws IOException,
//                                                                        IllegalCeosFormatException {
//        // Field 30
//        _offNadirMirrorPointAngle = reader.readAn(16);
//        // Field 31
//        reader.skipBytes(1); // Blank
//    }
//
//    protected void readField73ToEnd(final CeosFileReader reader) throws IOException,
//                                                                        IllegalCeosFormatException {
//        // Field 73
//        _yawSteeringFlag = reader.readAn(2);
//        // Field 74
//        reader.skipBytes(2808);
//    }

    public String getOffNadirMirrorPointAngle() {
        return _offNadirMirrorPointAngle;
    }

    @Override
    public String getYawSteeringFlag() {
        return _yawSteeringFlag;
    }
}
