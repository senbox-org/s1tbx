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
import org.esa.beam.dataio.ceos.records.BaseTrailerRecord;
import org.esa.beam.dataio.ceos.records.BaseTrailerRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public class Avnir2TrailerRecordTest extends BaseTrailerRecordTest {

    @Override
    protected BaseTrailerRecord createTrailerRecord(final CeosFileReader reader) throws IOException,
                                                                                        IllegalCeosFormatException {
        return new Avnir2TrailerRecord(reader);
    }

    @Override
    protected BaseTrailerRecord createTrailerRecord(final CeosFileReader reader, final int startPos) throws IOException,
                                                                                                            IllegalCeosFormatException {
        return new Avnir2TrailerRecord(reader, startPos);
    }

    @Override
    protected void writeHistograms(final ImageOutputStream ios) throws IOException {
        for (int i = 0; i < 4000; i += 1000) {
            for (int j = 0; j < 256; j++) {
                ios.writeInt(i + j);
            }
        }
    }

    @Override
    protected void assertHistograms(final BaseTrailerRecord record) {
        final Avnir2TrailerRecord trailerRecord = (Avnir2TrailerRecord) record;

        for (int i = 0; i < 4000; i += 1000) {
            final int bandNumber = i / 1000 + 1;
            final int[] histoForBand = trailerRecord.getHistogramFor(bandNumber);
            for (int j = 0; j < 256; j++) {
                assertEquals(i + j, histoForBand[j]);
            }
        }
    }

}