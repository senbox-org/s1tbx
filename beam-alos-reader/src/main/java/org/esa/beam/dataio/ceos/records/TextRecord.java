/*
 * $Id: TextRecord.java,v 1.1 2006/09/13 09:12:35 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
