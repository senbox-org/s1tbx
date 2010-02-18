/*
 * $Id: TrailerFileDescriptorRecord.java,v 1.1 2006/09/13 09:12:35 marcop Exp $
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
