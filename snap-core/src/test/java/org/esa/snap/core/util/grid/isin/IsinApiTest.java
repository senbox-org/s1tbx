package org.esa.snap.core.util.grid.isin;

import org.junit.Test;

import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_1_KM;
import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_250_M;
import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_500_M;
import static org.junit.Assert.assertEquals;

/* This test covers the API functionality for the Integerized Sinusoidal Transformation.
 * All values have been compared with the "official" NASA Modis Land tile computation Tool:
 *
 * https://landweb.modaps.eosdis.nasa.gov/cgi-bin/developer/tilemap.cgi
 *
 * tb 2018-05-28
 *
 */

public class IsinApiTest {

    @Test
    public void testToGlobalMap_1km() {
        final IsinAPI isinAPI = new IsinAPI(GRID_1_KM);

        // front pole
        IsinPoint isinPoint = isinAPI.toGlobalMap(0.0, 0.0);
        assertEquals(0.0, isinPoint.getX(), 1e-8);
        assertEquals(0.0, isinPoint.getY(), 1e-8);

        // Hamburg
        isinPoint = isinAPI.toGlobalMap(9.993682, 53.551086);
        assertEquals(660163.620386195, isinPoint.getX(), 1e-8);
        assertEquals(5954615.791176178, isinPoint.getY(), 1e-8);

        // Cape of Good Hope
        isinPoint = isinAPI.toGlobalMap(-18.49755, -34.357203);
        assertEquals(-1698032.4144802324, isinPoint.getX(), 1e-8);
        assertEquals(-3820350.9733573943, isinPoint.getY(), 1e-8);

        // Green Patch
        isinPoint = isinAPI.toGlobalMap(-58.130897, -51.557058);
        assertEquals(-4019582.789408277, isinPoint.getX(), 1e-8);
        assertEquals(-5732889.744073278, isinPoint.getY(), 1e-8);

        // Tatoosh island light house
        isinPoint = isinAPI.toGlobalMap(-124.736636, 48.391578);
        assertEquals(-9211556.01868853, isinPoint.getX(), 1e-8);
        assertEquals(5380904.030942225, isinPoint.getY(), 1e-8);

        // close to north pole
        isinPoint = isinAPI.toGlobalMap(116.8765, 89.45673);
        assertEquals(123944.1772881452, isinPoint.getX(), 1e-8);
        assertEquals(9947145.742011352, isinPoint.getY(), 1e-8);
    }

    @Test
    public void testToGlobalMap_500m() {
        final IsinAPI isinAPI = new IsinAPI(GRID_500_M);

        IsinPoint isinPoint = isinAPI.toGlobalMap(0.0, 0.0);
        assertEquals(0.0, isinPoint.getX(), 1e-8);
        assertEquals(0.0, isinPoint.getY(), 1e-8);

        // Hamburg
        isinPoint = isinAPI.toGlobalMap(9.993682, 53.551086);
        assertEquals(660189.3437192842, isinPoint.getX(), 1e-8);
        assertEquals(5954615.791176178, isinPoint.getY(), 1e-8);

        // Cape of Good Hope
        isinPoint = isinAPI.toGlobalMap(-18.49755, -34.357203);
        assertEquals(-1698240.2648659053, isinPoint.getX(), 1e-8);
        assertEquals(-3820350.9733573943, isinPoint.getY(), 1e-8);

        // Green Patch
        isinPoint = isinAPI.toGlobalMap(-58.130897, -51.557058);
        assertEquals(-4019126.6931849453, isinPoint.getX(), 1e-8);
        assertEquals(-5732889.744073278, isinPoint.getY(), 1e-8);

        // Tatoosh island light house
        isinPoint = isinAPI.toGlobalMap(-124.736636, 48.391578);
        assertEquals(-9210842.761749571, isinPoint.getX(), 1e-8);
        assertEquals(5380904.030942225, isinPoint.getY(), 1e-8);

        // close to north pole
        isinPoint = isinAPI.toGlobalMap(116.8765, 89.45673);
        assertEquals(123342.5065246105, isinPoint.getX(), 1e-8);
        assertEquals(9947145.742011352, isinPoint.getY(), 1e-8);
    }

    @Test
    public void testToGlobalMap_250m() {
        final IsinAPI isinAPI = new IsinAPI(GRID_250_M);

        IsinPoint isinPoint = isinAPI.toGlobalMap(0.0, 0.0);
        assertEquals(0.0, isinPoint.getX(), 1e-8);
        assertEquals(0.0, isinPoint.getY(), 1e-8);

        // Hamburg
        isinPoint = isinAPI.toGlobalMap(9.993682, 53.551086);
        assertEquals(660202.2053858287, isinPoint.getX(), 1e-8);
        assertEquals(5954615.791176178, isinPoint.getY(), 1e-8);

        // Cape of Good Hope
        isinPoint = isinAPI.toGlobalMap(-18.49755, -34.357203);
        assertEquals(-1697984.8025350068, isinPoint.getX(), 1e-8);
        assertEquals(-3820350.9733573943, isinPoint.getY(), 1e-8);

        // Green Patch
        isinPoint = isinAPI.toGlobalMap(-58.130897, -51.557058);
        assertEquals(-4018898.6450732797, isinPoint.getX(), 1e-8);
        assertEquals(-5732889.744073278, isinPoint.getY(), 1e-8);

        // Tatoosh island light house
        isinPoint = isinAPI.toGlobalMap(-124.736636, 48.391578);
        assertEquals(-9210450.571864398, isinPoint.getX(), 1e-8);
        assertEquals(5380904.030942225, isinPoint.getY(), 1e-8);

        // close to north pole
        isinPoint = isinAPI.toGlobalMap(116.8765, 89.45673);
        assertEquals(123001.05180914262, isinPoint.getX(), 1e-8);
        assertEquals(9947145.742011352, isinPoint.getY(), 1e-8);
    }

    @Test
    public void testToTileImageCoordinates_1km() {
        final IsinAPI isinAPI = new IsinAPI(GRID_1_KM);

        // front pole
        IsinPoint isinPoint = isinAPI.toTileImageCoordinates(0.0, 0.0);
        assertEquals(-0.5, isinPoint.getX(), 1e-6);
        assertEquals(-0.5, isinPoint.getY(), 1e-6);
        assertEquals(9, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());

        // Hamburg
        isinPoint = isinAPI.toTileImageCoordinates(9.993682, 53.551086);
        assertEquals(711.9384858866397, isinPoint.getX(), 1e-6);
        assertEquals(773.369679449138, isinPoint.getY(), 1e-6);
        assertEquals(3, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());

        // Cape of Good Hope
        isinPoint = isinAPI.toTileImageCoordinates(-18.49755, -34.357203);
        assertEquals(567.00937995395, isinPoint.getX(), 1e-6);
        assertEquals(522.3643604597146, isinPoint.getY(), 1e-6);
        assertEquals(12, isinPoint.getTile_line());
        assertEquals(16, isinPoint.getTile_col());

        // Green Patch
        isinPoint = isinAPI.toTileImageCoordinates(-58.130897, -51.557058);
        assertEquals(461.6277099444924, isinPoint.getX(), 1e-6);
        assertEquals(186.34696065743992, isinPoint.getY(), 1e-6);
        assertEquals(14, isinPoint.getTile_line());
        assertEquals(14, isinPoint.getTile_col());

        // Tatoosh island light house
        isinPoint = isinAPI.toTileImageCoordinates(-124.736636, 48.391578);
        assertEquals(858.5295819438379, isinPoint.getX(), 1e-6);
        assertEquals(192.51063950844946, isinPoint.getY(), 1e-6);
        assertEquals(4, isinPoint.getTile_line());
        assertEquals(9, isinPoint.getTile_col());

        // close to north pole
        isinPoint = isinAPI.toTileImageCoordinates(116.8765, 89.45673);
        assertEquals(133.2586612534251, isinPoint.getX(), 1e-6);
        assertEquals(64.69239903637313, isinPoint.getY(), 1e-6);
        assertEquals(0, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());
    }

    @Test
    public void testToTileImageCoordinates_500m() {
        final IsinAPI isinAPI = new IsinAPI(GRID_500_M);

        // front pole
        IsinPoint isinPoint = isinAPI.toTileImageCoordinates(0.0, 0.0);
        assertEquals(-0.5, isinPoint.getX(), 1e-6);
        assertEquals(-0.5, isinPoint.getY(), 1e-6);
        assertEquals(9, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());

        // Hamburg
        isinPoint = isinAPI.toTileImageCoordinates(9.993682, 53.551086);
        assertEquals(1424.4324922288506, isinPoint.getX(), 1e-6);
        assertEquals(1547.239358898276, isinPoint.getY(), 1e-6);
        assertEquals(3, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());

        // Cape of Good Hope
        isinPoint = isinAPI.toTileImageCoordinates(-18.49755, -34.357203);
        assertEquals(1134.070141991193, isinPoint.getX(), 1e-6);
        assertEquals(1045.2287209194292, isinPoint.getY(), 1e-6);
        assertEquals(12, isinPoint.getTile_line());
        assertEquals(16, isinPoint.getTile_col());

        // Green Patch
        isinPoint = isinAPI.toTileImageCoordinates(-58.130897, -51.557058);
        assertEquals(924.7398440307516, isinPoint.getX(), 1e-6);
        assertEquals(373.1939213148871, isinPoint.getY(), 1e-6);
        assertEquals(14, isinPoint.getTile_line());
        assertEquals(14, isinPoint.getTile_col());

        // Tatoosh island light house
        isinPoint = isinAPI.toTileImageCoordinates(-124.736636, 48.391578);
        assertEquals(1719.0986358544942, isinPoint.getX(), 1e-6);
        assertEquals(385.5212790168989, isinPoint.getY(), 1e-6);
        assertEquals(4, isinPoint.getTile_line());
        assertEquals(9, isinPoint.getTile_col());

        // close to north pole
        isinPoint = isinAPI.toTileImageCoordinates(116.8765, 89.45673);
        assertEquals(265.71869472895924, isinPoint.getX(), 1e-6);
        assertEquals(129.88479807274632, isinPoint.getY(), 1e-6);
        assertEquals(0, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());
    }

    @Test
    public void testToTileImageCoordinates_250m() {
        final IsinAPI isinAPI = new IsinAPI(GRID_250_M);

        // front pole
        IsinPoint isinPoint = isinAPI.toTileImageCoordinates(0.0, 0.0);
        assertEquals(-0.5, isinPoint.getX(), 1e-6);
        assertEquals(-0.5, isinPoint.getY(), 1e-6);
        assertEquals(9, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());

        // Hamburg
        isinPoint = isinAPI.toTileImageCoordinates(9.993682, 53.551086);
        assertEquals(2849.4205049132433, isinPoint.getX(), 1e-6);
        assertEquals(3094.9787177965554, isinPoint.getY(), 1e-6);
        assertEquals(3, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());

        // Cape of Good Hope
        isinPoint = isinAPI.toTileImageCoordinates(-18.49755, -34.357203);
        assertEquals(2269.743048149161, isinPoint.getX(), 1e-6);
        assertEquals(2090.9574418388584, isinPoint.getY(), 1e-6);
        assertEquals(12, isinPoint.getTile_line());
        assertEquals(16, isinPoint.getTile_col());

        // Green Patch
        isinPoint = isinAPI.toTileImageCoordinates(-58.130897, -51.557058);
        assertEquals(1850.964112203248, isinPoint.getX(), 1e-6);
        assertEquals(746.8878426297742, isinPoint.getY(), 1e-6);
        assertEquals(14, isinPoint.getTile_line());
        assertEquals(14, isinPoint.getTile_col());

        // Tatoosh island light house
        isinPoint = isinAPI.toTileImageCoordinates(-124.736636, 48.391578);
        assertEquals(3440.390253020232, isinPoint.getX(), 1e-6);
        assertEquals(771.5425580338015, isinPoint.getY(), 1e-6);
        assertEquals(4, isinPoint.getTile_line());
        assertEquals(9, isinPoint.getTile_col());

        // close to north pole
        isinPoint = isinAPI.toTileImageCoordinates(116.8765, 89.45673);
        assertEquals(530.463418624422, isinPoint.getX(), 1e-6);
        assertEquals(260.2695961454967, isinPoint.getY(), 1e-6);
        assertEquals(0, isinPoint.getTile_line());
        assertEquals(18, isinPoint.getTile_col());
    }

    @Test
    public void testGetTileDimensions_1km() {
        final IsinAPI isinAPI = new IsinAPI(GRID_1_KM);

        final IsinPoint tileDim = isinAPI.getTileDimensions();
        assertEquals(1200.0, tileDim.getX(), 1e-8);
        assertEquals(1200.0, tileDim.getY(), 1e-8);
    }

    @Test
    public void testGetTileDimensions_500m() {
        final IsinAPI isinAPI = new IsinAPI(GRID_500_M);

        final IsinPoint tileDim = isinAPI.getTileDimensions();
        assertEquals(2400.0, tileDim.getX(), 1e-8);
        assertEquals(2400.0, tileDim.getY(), 1e-8);
    }

    @Test
    public void testGetTileDimensions_250m() {
        final IsinAPI isinAPI = new IsinAPI(GRID_250_M);

        final IsinPoint tileDim = isinAPI.getTileDimensions();
        assertEquals(4800.0, tileDim.getX(), 1e-8);
        assertEquals(4800.0, tileDim.getY(), 1e-8);
    }
}
