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
import org.esa.beam.dataio.ceos.records.BaseLeaderFileDescriptorRecord;

import java.io.IOException;

public class Avnir2LeaderFDR extends BaseLeaderFileDescriptorRecord {

    private long _pixelSizeLocator;
    private long _pixelSizeLocatorDataStart;
    private long _pixelSizeLocatorNumBytes;
    private String _pixelSizeLocatorDataType;


    public Avnir2LeaderFDR(final CeosFileReader reader) throws IOException, IllegalCeosFormatException {
        this(reader, -1);
    }

    public Avnir2LeaderFDR(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                    IllegalCeosFormatException {
        super(reader, startPos);
//        reader.skipBytes(16);   // 16 blanks
//
//        _pixelSizeLocator = reader.readIn(6);
//        _pixelSizeLocatorDataStart = reader.readIn(6);
//        _pixelSizeLocatorNumBytes = reader.readIn(3);
//        _pixelSizeLocatorDataType = reader.readAn(1);
//
//        reader.skipBytes(16 + 16 + 4256); // skip blanks
    }

    @Override
    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
        reader.seek(getAbsolutPosition(376));
        _pixelSizeLocator = reader.readIn(6);
        _pixelSizeLocatorDataStart = reader.readIn(6);
        _pixelSizeLocatorNumBytes = reader.readIn(3);
        _pixelSizeLocatorDataType = reader.readAn(1);

    }

    public long getPixelSizeLocator() {
        return _pixelSizeLocator;
    }

    public long getPixelSizeLocatorDataStart() {
        return _pixelSizeLocatorDataStart;
    }

    public long getPixelSizeLocatorNumBytes() {
        return _pixelSizeLocatorNumBytes;
    }

    public String getPixelSizeLocatorDataType() {
        return _pixelSizeLocatorDataType;
    }

}
