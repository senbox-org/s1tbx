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
package org.esa.nest.dataio.binary;

import org.esa.beam.framework.datamodel.MetadataElement;

import java.io.IOException;

public class BinaryRecord {

    private final long startPos;
    private final BinaryFileReader reader;

    private final BinaryDBReader db;
    private final Integer recordLength;

    public BinaryRecord(final BinaryFileReader reader, final long startPos,
                        final org.jdom.Document recordDefinitionXML, final String recName) throws IOException {
        this.reader = reader;
        // reposition start if needed
        if (startPos != -1) {
            this.startPos = startPos;
            reader.seek(startPos);
        } else {
            this.startPos = reader.getCurrentPos();
        }

        if(startPos >= reader.getLength()) {
            recordLength = 0;
            db = null;
            return;
        }

        db = new BinaryDBReader(recordDefinitionXML, recName, this.startPos);
        db.readRecord(reader);

        recordLength = getAttributeInt("Record Length");
    }

    public final String getAttributeString(final String name) {
        return db.getAttributeString(name);
    }

    public final Integer getAttributeInt(final String name) {
        return db.getAttributeInt(name);
    }

    public final Double getAttributeDouble(final String name) {
        return db.getAttributeDouble(name);
    }

    public final int getRecordLength() {
        return recordLength;
    }

    public final long getStartPos() {
        return startPos;
    }

    public BinaryFileReader getReader() {
        return reader;
    }

    public final BinaryDBReader getBinaryDatabase() {
        return db; 
    }

    public long getRecordEndPosition() {
        if(recordLength != null)
            return startPos + recordLength;
        return startPos;
    }

    public long getAbsolutPosition(final long relativePosition) {
        return startPos + relativePosition;
    }

    public void assignMetadataTo(final MetadataElement elem) {
        if(db != null) {
            db.assignMetadataTo(elem);
        }
    }

    protected static MetadataElement createMetadataElement(final String name, final String suffix) {
        final MetadataElement elem;
        if (suffix != null && suffix.trim().length() > 0) {
            elem = new MetadataElement(name + ' ' + suffix.trim());
        } else {
            elem = new MetadataElement(name);
        }
        return elem;
    }
}
