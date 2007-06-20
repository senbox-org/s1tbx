/*
 * $Id: LeaderFileDescriptorRecord.java,v 1.1 2006/09/13 09:12:34 marcop Exp $
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
package org.esa.beam.dataio.ceos.prism.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseLeaderFileDescriptorRecord;

import java.io.IOException;

// todo - this class can be deleted
// todo - use BaseLeaderFileDescriptorRecord instead

public class LeaderFileDescriptorRecord extends BaseLeaderFileDescriptorRecord {


    public LeaderFileDescriptorRecord(final CeosFileReader reader) throws IOException, IllegalCeosFormatException {
        this(reader, -1);
    }

    public LeaderFileDescriptorRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                               IllegalCeosFormatException {
        super(reader, startPos);

//        reader.skipBytes(16 + 16 + 16 + 16 + 4256); // skip blanks
    }
}
