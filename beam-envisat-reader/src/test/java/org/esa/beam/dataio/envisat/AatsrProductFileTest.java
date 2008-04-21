package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;

public class AatsrProductFileTest extends TestCase {

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
