package org.esa.beam.dataio.envi;

import junit.framework.TestCase;

public class EnviMapInfoTest extends TestCase {

    public void testSetGetProjectionName() {
        final String name_1 = "hurtpiert";
        final String name_2 = "gnasenglanz popuen";

        mapInfo.setProjectionName(name_1);
        assertEquals(name_1, mapInfo.getProjectionName());

        mapInfo.setProjectionName(name_2);
        assertEquals(name_2, mapInfo.getProjectionName());
    }

    public void testSetGetReferencePixelX() {
        final double x_1 = 45.88;
        final double x_2 = 109.034;

        mapInfo.setReferencePixelX(x_1);
        assertEquals(x_1, mapInfo.getReferencePixelX(), 1e-8);

        mapInfo.setReferencePixelX(x_2);
        assertEquals(x_2, mapInfo.getReferencePixelX(), 1e-8);
    }

    public void testSetGetReferencePixelY() {
        final double y_1 = 34.77;
        final double y_2 = 2.99602;

        mapInfo.setReferencePixelY(y_1);
        assertEquals(y_1, mapInfo.getReferencePixelY(), 1e-8);

        mapInfo.setReferencePixelY(y_2);
        assertEquals(y_2, mapInfo.getReferencePixelY(), 1e-8);
    }

    public void testSetGetEasting() {
        final double easting_1 = -23.99;
        final double easting_2 = 0.0034;

        mapInfo.setEasting(easting_1);
        assertEquals(easting_1, mapInfo.getEasting(), 1e-8);

        mapInfo.setEasting(easting_2);
        assertEquals(easting_2, mapInfo.getEasting(), 1e-8);
    }

    public void testSetGetNorthing() {
        final double northing_1 = 3.7759;
        final double northing_2 = -10056.9;

        mapInfo.setNorthing(northing_1);
        assertEquals(northing_1, mapInfo.getNorthing(), 1e-8);

        mapInfo.setNorthing(northing_2);
        assertEquals(northing_2, mapInfo.getNorthing(), 1e-8);
    }

    public void testSetGetPixelSizeX() {
        final double sizeX_1 = 33.76;
        final double sizeX_2 = 18.44;

        mapInfo.setPixelSizeX(sizeX_1);
        assertEquals(sizeX_1, mapInfo.getPixelSizeX(), 1e-8);

        mapInfo.setPixelSizeX(sizeX_2);
        assertEquals(sizeX_2, mapInfo.getPixelSizeX(), 1e-8);
    }

    public void testSetGetPixelSizeY() {
        final double sizeY_1 = 7761.002;
        final double sizeY_2 = 105.4;

        mapInfo.setPixelSizeY(sizeY_1);
        assertEquals(sizeY_1, mapInfo.getPixelSizeY(), 1e-8);

        mapInfo.setPixelSizeY(sizeY_2);
        assertEquals(sizeY_2, mapInfo.getPixelSizeY(), 1e-8);
    }

    public void testSetGetDatum() {
        final String datum_1 = "heute";
        final String datum_2 = "WGS_1990";

        mapInfo.setDatum(datum_1);
        assertEquals(datum_1, mapInfo.getDatum());

        mapInfo.setDatum(datum_2);
        assertEquals(datum_2, mapInfo.getDatum());
    }

    public void testSetGetUnit() {
        final String unit_1 = "kilometer";
        final String unit_2 = "megazorks";

        mapInfo.setUnit(unit_1);
        assertEquals(unit_1, mapInfo.getUnit());

        mapInfo.setUnit(unit_2);
        assertEquals(unit_2, mapInfo.getUnit());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private EnviMapInfo mapInfo;

    protected void setUp() {
        mapInfo = new EnviMapInfo();
    }
}
