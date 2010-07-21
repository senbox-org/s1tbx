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

public class TextRecord extends BaseRecord {

    private final String _codeCharacter;
    private final String _productID;
    private final String _facility;
    private final String _sceneID;
    private final String _imageFormat;

    public TextRecord(final CeosFileReader reader) throws IOException,
                                                          IllegalCeosFormatException {
        this(reader, -1);
    }

    public TextRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                               IllegalCeosFormatException {
        super(reader, startPos);

        _codeCharacter = reader.readAn(2);
        reader.skipBytes(2);    // blank
        _productID = reader.readAn(40).trim().substring(8);
        _facility = reader.readAn(60);
        _sceneID = reader.readAn(40).trim().substring(6);
        _imageFormat = reader.readAn(4);
        // The last 200 bytes are blanks.
        reader.skipBytes(200);
    }

    public String getCodeCharacter() {
        return _codeCharacter;
    }

    public String getProductID() {
        return _productID;
    }

    public String getFacility() {
        return _facility;
    }

    public String getSceneID() {
        return _sceneID;
    }

    public String getImageFormat() {
        return _imageFormat;
    }
}
