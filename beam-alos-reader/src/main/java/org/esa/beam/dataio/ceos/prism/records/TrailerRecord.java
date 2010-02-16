/*
 * $Id: TrailerRecord.java,v 1.1 2006/09/13 09:12:34 marcop Exp $
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
import org.esa.beam.dataio.ceos.records.BaseTrailerRecord;

import java.io.IOException;

public class TrailerRecord extends BaseTrailerRecord {

    private int[][] _histoCCDs;

    public TrailerRecord(final CeosFileReader reader) throws IOException,
                                                             IllegalCeosFormatException {
        this(reader, -1);
    }

    public TrailerRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                  IllegalCeosFormatException {
        super(reader, startPos);

    }

    @Override
    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
        reader.seek(getAbsolutPosition(20));
        _histoCCDs = new int[8][256];
        readHistograms(reader, _histoCCDs);
    }

    @Override
    public int[] getHistogramFor(final int ccdIndex) {
        return _histoCCDs[ccdIndex - 1];
    }
}