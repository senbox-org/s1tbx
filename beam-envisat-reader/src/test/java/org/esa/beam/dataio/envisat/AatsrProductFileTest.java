package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;

public class AatsrProductFileTest extends TestCase {

    public void testCalculateSceneRasterHeight() {
        final int numRecords_1 = 109;
        final int numRecords_2 = 1865;
        DSD geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_1, 14);

        assertEquals((numRecords_1 - 1) * 32 + 1, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD));

        geolocationDSD = new DSD(0, "ignore", 'A', "", 12, 13, numRecords_2, 14);
        assertEquals((numRecords_2 - 1) * 32 + 1, AatsrProductFile.calculateSceneRasterHeight(geolocationDSD));
    }
}
