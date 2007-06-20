/*
 * $Id: TrailerRecordTest.java,v 1.1 2006/09/13 09:12:36 marcop Exp $
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
import org.esa.beam.dataio.ceos.records.BaseTrailerRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public class TrailerRecordTest extends BaseTrailerRecordTest {

    protected BaseTrailerRecord createTrailerRecord(final CeosFileReader reader) throws IOException,
                                                                                        IllegalCeosFormatException {
        return new TrailerRecord(reader);
    }

    protected BaseTrailerRecord createTrailerRecord(final CeosFileReader reader, final int startPos) throws IOException,
                                                                                                            IllegalCeosFormatException {
        return new TrailerRecord(reader, startPos);
    }

    protected void writeHistograms(final ImageOutputStream ios) throws IOException {
        for (int i = 0; i < 8000; i += 1000) {
            for (int j = 0; j < 256; j++) {
                ios.writeInt(i + j);
            }
        }
    }

    protected void assertHistograms(final BaseTrailerRecord record) {
        final TrailerRecord trailerRecord = (TrailerRecord) record;

        for (int i = 0; i < 8000; i += 1000) {
            final int ccdNumber = i / 1000 + 1;
            final int[] histoForCCD = trailerRecord.getHistogramFor(ccdNumber);
            for (int j = 0; j < 256; j++) {
                assertEquals(i + j, histoForCCD[j]);
            }
        }
    }

}
