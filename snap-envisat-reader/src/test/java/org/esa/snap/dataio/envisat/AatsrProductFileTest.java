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

package org.esa.snap.dataio.envisat;

import org.junit.Test;

import static org.junit.Assert.*;

public class AatsrProductFileTest {

    @Test
    public void testCalculateSceneRasterHeight_MDS_DSD_matches() {
        final int numRecords_1 = 109;
        final int numRecords_2 = 1865;
        final int numLines_1 = (numRecords_1 - 1) * 32;
        final int numLines_2 = (numRecords_2 - 1) * 32;

        DSD geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_1, 14);
        assertEquals(numLines_1, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD, numLines_1));

        geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_2, 14);
        assertEquals(numLines_2, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD, numLines_2));
    }

    @Test
    public void testCalculateSceneRasterHeight_MDS_DSD_lessLines() {
        final int numRecords_1 = 109;
        final int numRecords_2 = 1865;
        final int numLines_1 = (numRecords_1 - 1) * 32;
        final int numLines_2 = (numRecords_2 - 1) * 32;

        DSD geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_1, 14);
        assertEquals(numLines_1, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD, numLines_1 - 12));

        geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_2, 14);
        assertEquals(numLines_2, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD, numLines_2 - 24));
    }

    @Test
    public void testCalculateSceneRasterHeight_MDS_DSD_moreLines() {
        final int numRecords_1 = 109;
        final int numRecords_2 = 1865;
        final int numLines_1 = (numRecords_1 - 1) * 32;
        final int numLines_2 = (numRecords_2 - 1) * 32;

        DSD geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_1, 14);
        assertEquals(numLines_1 + 5, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD, numLines_1 + 5));

        geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_2, 14);
        assertEquals(numLines_2 + 11, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD, numLines_2 + 11));
    }

}
