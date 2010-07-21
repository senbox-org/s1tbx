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
package org.esa.beam.dataio.ceos.prism.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseLeaderFileDescriptorRecord;
import org.esa.beam.dataio.ceos.records.BaseLeaderFileDescriptorRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class LeaderFileDescriptorRecordTest extends BaseLeaderFileDescriptorRecordTest {

    @Override
    protected BaseLeaderFileDescriptorRecord createLeaderFDR(final CeosFileReader reader, final int startPos) throws
                                                                                                              IOException,
                                                                                                              IllegalCeosFormatException {
        return new LeaderFileDescriptorRecord(reader, startPos);
    }

    @Override
    protected BaseLeaderFileDescriptorRecord createLeaderFDR(final CeosFileReader reader) throws IOException,
                                                                                                 IllegalCeosFormatException {
        return new LeaderFileDescriptorRecord(reader);
    }


    @Override
    protected void writeFields17To21(final ImageOutputStream ios) throws IOException {
        ios.writeBytes("                "); // 16 blanks
        ios.writeBytes("                "); // 16 blanks
        ios.writeBytes("                "); // 16 blanks
        ios.writeBytes("                "); // 16 blanks

        // write 4256 blanks
        final char[] blanks = new char[4256];
        Arrays.fill(blanks, ' ');
        ios.writeBytes(new String(blanks));
    }

    @Override
    protected void assertRecords17To21(final BaseLeaderFileDescriptorRecord record) {
        // nothing to test - all blanks
    }
}

