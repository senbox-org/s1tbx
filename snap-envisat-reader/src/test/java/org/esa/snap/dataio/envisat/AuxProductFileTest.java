package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Marco Peters
 */
public class AuxProductFileTest {

    @Test
    public void testAuxDemGeoCodingCreation() throws Exception {
        int width = 2160;
        int height = 4320;
        CrsGeoCoding geoCoding = AuxProductFile.creatDemGeoCoding(width, height);

        assertEquals(new GeoPos(90, -180), geoCoding.getGeoPos(new PixelPos(width, 0), null)); // UL
        assertEquals(new GeoPos(-90, -180), geoCoding.getGeoPos(new PixelPos(0, 0), null));   // LL
        assertEquals(new GeoPos(90, 180), geoCoding.getGeoPos(new PixelPos(width, height), null)); // UR
        assertEquals(new GeoPos(-90, 180), geoCoding.getGeoPos(new PixelPos(0, height), null)); // LR

    }


}