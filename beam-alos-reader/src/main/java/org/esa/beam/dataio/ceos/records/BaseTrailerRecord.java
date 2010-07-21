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

public abstract class BaseTrailerRecord extends BaseRecord {

    private int _numTrailerRecords;
    private int _numTrailerRecordsInOneCCDUnit;

    public BaseTrailerRecord(final CeosFileReader reader) throws IOException,
                                                                 IllegalCeosFormatException {
        this(reader, -1);
    }

    public BaseTrailerRecord(final CeosFileReader reader, final long startPos) throws IOException,
                                                                                      IllegalCeosFormatException {
        super(reader, startPos);

        readGeneralFields(reader);
        reader.seek(getAbsolutPosition(getRecordLength()));
    }

    private void readGeneralFields(final CeosFileReader reader) throws IOException,
                                                                       IllegalCeosFormatException {
        _numTrailerRecords = reader.readI4();
        _numTrailerRecordsInOneCCDUnit = reader.readI4();

        readSpecificFields(reader);
    }

    protected void readSpecificFields(final CeosFileReader reader) throws IOException,
                                                                          IllegalCeosFormatException {
    }

    public int getNumTrailerRecords() {
        return _numTrailerRecords;
    }

    public int getNumTrailerRecordsInOneCCDUnit() {
        return _numTrailerRecordsInOneCCDUnit;
    }

    protected final void readHistograms(final CeosFileReader reader, final int[][] histos) throws IOException,
                                                                                                  IllegalCeosFormatException {
        for (int i = 0; i < histos.length; i++) {
            final int[] histoCCD = histos[i];
            reader.readB4(histoCCD);
        }
    }

    protected abstract int[] getHistogramFor(int index);
}