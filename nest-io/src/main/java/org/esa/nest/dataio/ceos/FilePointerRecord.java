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
package org.esa.nest.dataio.ceos;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.nest.dataio.binary.BinaryFileReader;
import org.esa.nest.dataio.binary.BinaryRecord;

import java.io.IOException;

public class FilePointerRecord extends BinaryRecord {

    public FilePointerRecord(final BinaryFileReader reader, final org.jdom.Document filePointerXML, 
                             final String recName) throws IOException {
        this(reader, filePointerXML, -1, recName);
    }

    public FilePointerRecord(final BinaryFileReader reader, final org.jdom.Document filePointerXML, final long startPos,
                             final String recName) throws IOException {
        super(reader, startPos, filePointerXML, recName);
    }

    public void assignMetadataTo(final MetadataElement root, final String suffix) {
        final MetadataElement elem = createMetadataElement("FilePointerRecord", suffix);
        root.addElement(elem);

        super.assignMetadataTo(elem);
    }
}
