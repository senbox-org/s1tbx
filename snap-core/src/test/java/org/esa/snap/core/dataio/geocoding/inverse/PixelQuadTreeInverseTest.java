package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.AMSR2;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.TestData;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.esa.snap.core.dataio.geocoding.TestData.get_SLSTR_OL;
import static org.junit.Assert.assertEquals;

public class PixelQuadTreeInverseTest {

    private PixelQuadTreeInverse inverse;

    @Before
    public void setUp() {
        inverse = new PixelQuadTreeInverse();
    }

    @Test
    public void testGetPixelPos_SLSTR_OL() {
        final GeoRaster geoRaster = get_SLSTR_OL();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(45.836541, -130.33507), null);
        assertEquals(6.5, pixelPos.x, 1e-8);
        assertEquals(6.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.819392, -130.310602), null);
        assertEquals(14.5, pixelPos.x, 1e-8);
        assertEquals(11.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.8391669999999962, -130.245281), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.774808, -130.265089), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.790684, -130.37037999999998), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_SLSTR_OL_outside() {
        final GeoRaster geoRaster = get_SLSTR_OL();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(45.856, -130.358), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.838, -130.24), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.773, -130.26), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(45.791, -130.375), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSR2() {
        final GeoRaster geoRaster = TestData.get_AMSR_2_anti_meridian();

        inverse.initialize(geoRaster, true, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-70.659836, -176.61472), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-70.275215, 179.66467), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-68.75776, 174.80211), null);
        assertEquals(31.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-69.24617, 178.1685), null);
        assertEquals(0.5, pixelPos.x, 1e-8);
        assertEquals(25.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGeoPixelPos_SYN_AOD_fillValues() {
        final GeoRaster geoRaster = TestData.get_SYN_AOD();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(59.2421, -136.13405), null);
        assertEquals(9.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(59.19238, -135.08754), null);
        assertEquals(24.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(58.232895, -135.29134), null);
        assertEquals(24.5, pixelPos.x, 1e-8);
        assertEquals(26.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(58.28423, -136.32547), null);
        assertEquals(9.5, pixelPos.x, 1e-8);
        assertEquals(26.5, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_OLCI_interpolating() {
        inverse = new PixelQuadTreeInverse(true);

        final GeoRaster geoRaster = TestData.get_OLCI();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(66.495834, -24.168955), null);
        assertEquals(5.5, pixelPos.x, 1e-8);
        assertEquals(12.5, pixelPos.y, 1e-8);

        // interpolate in y direction between replicated pixels
        pixelPos = inverse.getPixelPos(new GeoPos(66.49456, -24.169599), null);
        assertEquals(6, pixelPos.x, 1e-8);
        assertEquals(13.750093939267787, pixelPos.y, 1e-8);

        // interpolate in lat-direction
        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.22587), null);
        assertEquals(0.5, pixelPos.x, 1e-8);    // duplicated pixel, the left of a pair
        assertEquals(34.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.4419375, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.574172929186844, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.441745, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.64807215085877, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.4415525, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.72162155510092, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44136, -24.22587), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.794716527858334, pixelPos.y, 1e-8);

        // interpolate in lon-direction
        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.22587), null);
        assertEquals(0.5, pixelPos.x, 1e-8);    // duplicated pixel, the left of a pair
        assertEquals(34.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.2234825), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.74547419281143, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.221095), null);
        assertEquals(1.0, pixelPos.x, 1e-8);
        assertEquals(34.85228235368744, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.2187075), null);
        assertEquals(3.0, pixelPos.x, 1e-8);
        assertEquals(34.15289103293384, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(66.44213, -24.21632), null);
        assertEquals(3.0, pixelPos.x, 1e-8);
        assertEquals(34.20636166925025, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_AMSRE_interpolating() {
        inverse = new PixelQuadTreeInverse(true);

        final GeoRaster geoRaster = TestData.get_AMSRE();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(-0.8298334, 18.600895), null);
        assertEquals(3.5, pixelPos.x, 1e-8);
        assertEquals(0.5, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(-0.7098208, 18.553856), null);
        assertEquals(4.5, pixelPos.x, 1e-8);
        assertEquals(1.5, pixelPos.y, 1e-8);

        // @todo 1 tb/tb this is suspicious! 2020-01-10
        pixelPos = inverse.getPixelPos(new GeoPos(-0.7698271, 18.5773755), null);
        assertEquals(2.9929638622796446, pixelPos.x, 1e-8);
        assertEquals(1.7308288269813148, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_SLSTR_OL_invalid_geo_pos() {
        final GeoRaster geoRaster = get_SLSTR_OL();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final PixelPos pixelPos = inverse.getPixelPos(new GeoPos(NaN, -130.33507), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetGeoPos_AMSR2() {
        final GeoRaster geoRaster = new GeoRaster(AMSR2.AMSR2_ANTI_MERID_LON, AMSR2.AMSR2_ANTI_MERID_LAT, 32, 26,
                32, 26, 0.3, 0.5, 0.5, 1.0, 1.0);

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos();
        inverse.getGeoPos(3, 5, geoPos);
        assertEquals(-178.06575, geoPos.lon, 1e-8);
        assertEquals(-70.34471, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetGeoPos_SLST_OL() {
        final GeoRaster geoRaster = get_SLSTR_OL();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final GeoPos geoPos = new GeoPos();
        inverse.getGeoPos(1, 3, geoPos);
        assertEquals(-130.34898099999998, geoPos.lon, 1e-8);
        assertEquals(45.846712, geoPos.lat, 1e-8);
    }

    @Test
    public void testGetEpsilon_AMSR2() {
        final GeoRaster geoRaster = new GeoRaster(AMSR2.AMSR2_ANTI_MERID_LON, AMSR2.AMSR2_ANTI_MERID_LAT, 32, 26,
                32, 26, 0.3, 0.5, 0.5, 1.0, 1.0);

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final double epsilon = inverse.getEpsilon();
        assertEquals(0.2218123261904139, epsilon, 1e-8);
    }

    @Test
    public void testGetEpsilon_SLSTR_OL() {
        final GeoRaster geoRaster = get_SLSTR_OL();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final double epsilon = inverse.getEpsilon();
        assertEquals(0.0023030467863381344, epsilon, 1e-8);
    }

    @Test
    public void testGetPositiveLonMin() {
        double lon0 = 160.0;
        double lon1 = 150.0;
        double lon2 = -169.0;
        double lon3 = 165.0;
        double result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(150.0, result, 0.0);

        lon0 = -175.0;
        lon1 = 170.0;
        lon2 = -169.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);

        lon0 = 170.0;
        lon1 = 160.0;
        lon2 = -175.0;
        lon3 = -165.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = -150.0;
        lon1 = +160.0;
        lon2 = -140.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -175.0;
        lon2 = -165.0;
        lon3 = -150.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);

        lon0 = 140.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(140.0, result, 0.0);

        lon0 = -175.0;
        lon1 = -165.0;
        lon2 = 170.0;
        lon3 = 160.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -140.0;
        lon2 = 160.0;
        lon3 = -150.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(160.0, result, 0.0);

        lon0 = -160.0;
        lon1 = -150.0;
        lon2 = 170.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -170.0;
        lon2 = 150.0;
        lon3 = 160.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(150.0, result, 0.0);

        lon0 = -170.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = 140.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(140.0, result, 0.0);

        lon0 = -150.0;
        lon1 = -170.0;
        lon2 = -160.0;
        lon3 = 170.0;
        result = PixelQuadTreeInverse.getPositiveLonMin(lon0, lon1, lon2, lon3);
        assertEquals(170.0, result, 0.0);
    }

    @Test
    public void testGetNegativeLonMax() {
        double lon0 = 160.0;
        double lon1 = 150.0;
        double lon2 = -169.0;
        double lon3 = 165.0;
        double result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-169.0, result, 0.0);

        lon0 = -175.0;
        lon1 = 170.0;
        lon2 = -169.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-169.0, result, 0.0);

        lon0 = 170.0;
        lon1 = 160.0;
        lon2 = -175.0;
        lon3 = -165.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-165.0, result, 0.0);

        lon0 = -150.0;
        lon1 = +160.0;
        lon2 = -140.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-140.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -175.0;
        lon2 = -165.0;
        lon3 = -150.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-150.0, result, 0.0);

        lon0 = 140.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-170.0, result, 0.0);

        lon0 = -175.0;
        lon1 = -165.0;
        lon2 = 170.0;
        lon3 = 160.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-165.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -140.0;
        lon2 = 160.0;
        lon3 = -150.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-140.0, result, 0.0);

        lon0 = -160.0;
        lon1 = -150.0;
        lon2 = 170.0;
        lon3 = -170.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-150.0, result, 0.0);

        lon0 = 170.0;
        lon1 = -170.0;
        lon2 = 150.0;
        lon3 = 160.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-170.0, result, 0.0);

        lon0 = -170.0;
        lon1 = 150.0;
        lon2 = 160.0;
        lon3 = 140.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-170.0, result, 0.0);

        lon0 = -150.0;
        lon1 = -170.0;
        lon2 = -160.0;
        lon3 = 170.0;
        result = PixelQuadTreeInverse.getNegativeLonMax(lon0, lon1, lon2, lon3);
        assertEquals(-150.0, result, 0.0);
    }

    @Test
    public void testSq() {
        assertEquals(4.0, PixelQuadTreeInverse.sq(2.0, 0.0), 1e-8);
        assertEquals(13.0, PixelQuadTreeInverse.sq(2.0, 3.0), 1e-8);
        assertEquals(16.0, PixelQuadTreeInverse.sq(0.0, 4.0), 1e-8);
    }

    @Test
    public void testDispose() {
        inverse.dispose();

        final GeoRaster geoRaster = get_SLSTR_OL();
        inverse.initialize(geoRaster, false, new PixelPos[0]);

        inverse.dispose();
    }
}
