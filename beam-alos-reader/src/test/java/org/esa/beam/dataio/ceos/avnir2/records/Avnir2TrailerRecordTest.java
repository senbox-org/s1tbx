package org.esa.beam.dataio.ceos.avnir2.records;

/*
 * $Id: Avnir2TrailerRecordTest.java,v 1.1 2006/09/13 09:12:35 marcop Exp $
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

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseTrailerRecord;
import org.esa.beam.dataio.ceos.records.BaseTrailerRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * Created by marco.
 *
 * @author marco
 * @version $Revision$ $Date$
 */
public class Avnir2TrailerRecordTest extends BaseTrailerRecordTest {

    protected BaseTrailerRecord createTrailerRecord(final CeosFileReader reader) throws IOException,
                                                                                        IllegalCeosFormatException {
        return new Avnir2TrailerRecord(reader);
    }

    protected BaseTrailerRecord createTrailerRecord(final CeosFileReader reader, final int startPos) throws IOException,
                                                                                                            IllegalCeosFormatException {
        return new Avnir2TrailerRecord(reader, startPos);
    }

    protected void writeHistograms(final ImageOutputStream ios) throws IOException {
        for (int i = 0; i < 4000; i += 1000) {
            for (int j = 0; j < 256; j++) {
                ios.writeInt(i + j);
            }
        }
    }

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