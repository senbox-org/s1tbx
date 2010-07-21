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

public class TrailerFileDescriptorRecord extends CommonFileDescriptorRecord {

    public long _numTrailerRecords;
    public long _trailerRecordLength;

    public TrailerFileDescriptorRecord(final CeosFileReader reader)
            throws IOException,
                   IllegalCeosFormatException {
        this(reader, -1);
    }

    public TrailerFileDescriptorRecord(final CeosFileReader reader, final long startPos)
            throws IOException,
                   IllegalCeosFormatException {
        super(reader, startPos);

        _numTrailerRecords = reader.readIn(6);
        _trailerRecordLength = reader.readIn(6);

        reader.seek(getAbsolutPosition(getTrailerRecordLength()));
    }

    public long getNumTrailerRecords() {
        return _numTrailerRecords;
    }

    public long getTrailerRecordLength() {
        return _trailerRecordLength;
    }
}
