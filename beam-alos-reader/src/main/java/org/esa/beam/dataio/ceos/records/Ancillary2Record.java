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
package org.esa.beam.dataio.ceos.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;

import java.io.IOException;

public class Ancillary2Record extends BaseRecord {

    private String _sensorOperationMode;
    private int _lowerLimitOfStrengthAfterCorrection;
    private int _upperLimitOfStrengthAfterCorrection;
    private String _sensorGains;

    public Ancillary2Record(final CeosFileReader reader) throws IOException, IllegalCeosFormatException {
        this(reader, -1);
    }

    public Ancillary2Record(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                     IllegalCeosFormatException {
        super(reader, startPos);

        readGeneralFields(reader);

        reader.seek(getAbsolutPosition(getRecordLength()));
    }

    private void readGeneralFields(final CeosFileReader reader) throws IOException,
                                                                       IllegalCeosFormatException {
        _sensorOperationMode = reader.readAn(4);
        _lowerLimitOfStrengthAfterCorrection = reader.readI4();
        _upperLimitOfStrengthAfterCorrection = reader.readI4();
        reader.skipBytes(32);   // skip 30 + 1 + 1
        _sensorGains = reader.readAn(6);

        readSpecificFields(reader);
    }

    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
    }

    public String getSensorOperationMode() {
        return _sensorOperationMode;
    }

    public int getLowerLimitOfStrengthAfterCorrection() {
        return _lowerLimitOfStrengthAfterCorrection;
    }

    public int getUpperLimitOfStrengthAfterCorrection() {
        return _upperLimitOfStrengthAfterCorrection;
    }

    public String getSensorGains() {
        return _sensorGains;
    }
}
