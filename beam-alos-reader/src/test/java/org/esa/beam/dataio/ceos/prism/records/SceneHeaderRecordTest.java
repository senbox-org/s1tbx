/*
 * $Id: SceneHeaderRecordTest.java,v 1.2 2006/09/14 09:15:55 marcop Exp $
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
import org.esa.beam.dataio.ceos.CeosTestHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseSceneHeaderRecord;
import org.esa.beam.dataio.ceos.records.BaseSceneHeaderRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

public class SceneHeaderRecordTest extends BaseSceneHeaderRecordTest {

    @Override
    protected BaseSceneHeaderRecord createSceneHeaderRecord(final CeosFileReader reader) throws IOException,
                                                                                                IllegalCeosFormatException {
        return new SceneHeaderRecord(reader);
    }

    @Override
    protected BaseSceneHeaderRecord createSceneHeaderRecord(final CeosFileReader reader, final int startPos) throws
                                                                                                             IOException,
                                                                                                             IllegalCeosFormatException {
        return new SceneHeaderRecord(reader, startPos);
    }

    @Override
    protected void writeFields30To31(final ImageOutputStream ios) throws IOException {
        // Field 30
        CeosTestHelper.writeBlanks(ios, 16); // 16 x blank
        // Field 31
        ios.writeBytes("2"); // compressionMode // A1
    }

    @Override
    protected void writeFields73ToEnd(final ImageOutputStream ios) throws IOException {
        ios.writeBytes("KJHGF"); // imageExtractionPoint // A5
        ios.writeBytes("54"); // flagYawSteering // A2
        CeosTestHelper.writeBlanks(ios, 2803); // 2803 x blank
    }

    @Override
    protected void assertFields30To31(final BaseSceneHeaderRecord record) {
        final SceneHeaderRecord sceneHeaderRecord = (SceneHeaderRecord) record;
        assertEquals("2", sceneHeaderRecord.getCompressionMode());
    }

    @Override
    protected void assertFields73ToEnd(final BaseSceneHeaderRecord record) {
        final SceneHeaderRecord sceneHeaderRecord = (SceneHeaderRecord) record;
        assertEquals("KJHGF", sceneHeaderRecord.getImageExtractionPoint());
        assertEquals("54", sceneHeaderRecord.getYawSteeringFlag());
    }
}
