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
import org.esa.beam.dataio.ceos.records.Ancillary2Record;

import java.io.IOException;

public class PrismAncillary2Record extends Ancillary2Record {

    private String _compressionMode;
    private double _ccdTemperature;
    private double _signalProcessingSectionTemperature;
    private double _absoluteCalibrationGain;
    private double _absoluteCalibrationOffset;

    public PrismAncillary2Record(final CeosFileReader reader) throws IOException, IllegalCeosFormatException {
        this(reader, -1);
    }

    public PrismAncillary2Record(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                          IllegalCeosFormatException {
        super(reader, startPos);
    }

    @Override
    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
        reader.seek(getAbsolutPosition(62));
        _compressionMode = reader.readAn(1);

        reader.seek(getAbsolutPosition(78));
        _ccdTemperature = reader.readFn(8);
        _signalProcessingSectionTemperature = reader.readFn(8);

        reader.seek(getAbsolutPosition(2702));
        _absoluteCalibrationGain = reader.readFn(8);
        _absoluteCalibrationOffset = reader.readFn(8);

    }

    public String getCompressionMode() {
        return _compressionMode;
    }

    public double getCcdTemperature() {
        return _ccdTemperature;
    }

    public double getSignalProcessingSectionTemperature() {
        return _signalProcessingSectionTemperature;
    }

    public double getAbsoluteCalibrationGain() {
        return _absoluteCalibrationGain;
    }

    public double getAbsoluteCalibrationOffset() {
        return _absoluteCalibrationOffset;
    }
}
