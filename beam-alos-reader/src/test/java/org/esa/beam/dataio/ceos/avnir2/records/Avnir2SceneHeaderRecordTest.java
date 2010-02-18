package org.esa.beam.dataio.ceos.avnir2.records;

/*
 * $Id: Avnir2SceneHeaderRecordTest.java,v 1.2 2006/09/14 09:15:55 marcop Exp $
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
import org.esa.beam.dataio.ceos.CeosTestHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseSceneHeaderRecord;
import org.esa.beam.dataio.ceos.records.BaseSceneHeaderRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;

/**
 * Created by marco.
 *
 * @author marco
 * @version $Revision$ $Date$
 */
public class Avnir2SceneHeaderRecordTest extends BaseSceneHeaderRecordTest {

    @Override
    protected BaseSceneHeaderRecord createSceneHeaderRecord(final CeosFileReader reader) throws IOException,
                                                                                                IllegalCeosFormatException {
        return new Avnir2SceneHeaderRecord(reader);
    }

    @Override
    protected BaseSceneHeaderRecord createSceneHeaderRecord(final CeosFileReader reader, final int startPos) throws
                                                                                                             IOException,
                                                                                                             IllegalCeosFormatException {
        return new Avnir2SceneHeaderRecord(reader, startPos);
    }

    @Override
    protected void writeFields30To31(final ImageOutputStream ios) throws IOException {
        // Field 30
        ios.writeBytes("         -26.000"); // OffNadirMirrorPointAngle
        // Field 31
        ios.writeBytes(" ");    // Blank
    }

    @Override
    protected void writeFields73ToEnd(final ImageOutputStream ios) throws IOException {
        // Field 73
        ios.writeBytes(" 1");   // YawSteeringFlag
        // Field 74
        CeosTestHelper.writeBlanks(ios, 2808);
    }

    @Override
    protected void assertFields30To31(final BaseSceneHeaderRecord record) {
        final Avnir2SceneHeaderRecord sceneHeaderRecord = (Avnir2SceneHeaderRecord) record;

        assertEquals("         -26.000", sceneHeaderRecord.getOffNadirMirrorPointAngle());
        // nothing else to test
    }

    @Override
    protected void assertFields73ToEnd(final BaseSceneHeaderRecord record) {
        final Avnir2SceneHeaderRecord sceneHeaderRecord = (Avnir2SceneHeaderRecord) record;

        assertEquals(" 1", sceneHeaderRecord.getYawSteeringFlag());
        // nothing else to test
    }
}