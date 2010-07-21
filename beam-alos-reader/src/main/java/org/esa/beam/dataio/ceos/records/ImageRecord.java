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

public class ImageRecord extends BaseRecord {

    public int _prefixDataLineNumber;
    public int _imageNumber;
    public int _scanStartTimeMillisAtDay;
    public short _scanStartTimeMicros;
    public int _numLeftDummyPixels;
    public int _numRightDummyPixels;
    public long _imageDataStart;

    public ImageRecord(final CeosFileReader reader) throws IOException,
                                                           IllegalCeosFormatException {
        this(reader, -1);
    }

    public ImageRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                IllegalCeosFormatException {
        super(reader, startPos);

        _prefixDataLineNumber = reader.readB4();
        _imageNumber = reader.readB4();
        _scanStartTimeMillisAtDay = reader.readB4();
        _scanStartTimeMicros = reader.readB2();
        _numLeftDummyPixels = reader.readB4();
        _numRightDummyPixels = reader.readB4();
        _imageDataStart = reader.getCurrentPos();
        reader.skipBytes(getStartPos() + getRecordLength() - _imageDataStart);
    }

    public int getPrefixDataLineNumber() {
        return _prefixDataLineNumber;
    }

    public int getImageNumber() {
        return _imageNumber;
    }

    public int getScanStartTimeMillisAtDay() {
        return _scanStartTimeMillisAtDay;
    }

    public short getScanStartTimeMicros() {
        return _scanStartTimeMicros;
    }

    public int getNumLeftDummyPixels() {
        return _numLeftDummyPixels;
    }

    public int getNumRightDummyPixels() {
        return _numRightDummyPixels;
    }

    public long getImageDataStart() {
        return _imageDataStart;
    }
}
