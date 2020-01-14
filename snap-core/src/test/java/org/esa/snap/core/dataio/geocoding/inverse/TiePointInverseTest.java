package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.util.Approximation;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.math.FXYSum;
import org.junit.Test;

import java.awt.*;

import static java.lang.Double.NaN;
import static org.junit.Assert.*;

public class TiePointInverseTest {

    @Test
    public void testNormalizeLonGrid_no_anti_meridian() {
        final float[] floatLongitudes = RasterUtils.toFloat(MERIS.MER_RR_LON);
        final TiePointGrid lonGrid = new TiePointGrid("lon", 5, 5, 0.5, 0.5,
                16, 16, floatLongitudes);

        final TiePointGrid normalizedLonGrid = TiePointInverse.normalizeLonGrid(lonGrid);
        assertArrayEquals(floatLongitudes, normalizedLonGrid.getTiePoints(), 1e-8f);
        assertSameRasterParameter(lonGrid, normalizedLonGrid);
    }

    @Test
    public void testNormalizeLonGrid_anti_meridian_east_normalized() {
        final float[] floatLongitudes = RasterUtils.toFloat(AMSUB.AMSUB_ANTI_MERID_LON);
        final TiePointGrid lonGrid = new TiePointGrid("lon", 31, 31, 0.5, 0.5,
                1.0, 1.0, floatLongitudes);

        final TiePointGrid normalizedLonGrid = TiePointInverse.normalizeLonGrid(lonGrid);
        final float[] tiePoints = normalizedLonGrid.getTiePoints();
        assertEquals(170.64950561523438, tiePoints[0], 1e-8);
        assertEquals(180.68719482421875, tiePoints[23], 1e-8);
        assertEquals(181.29580688476562, tiePoints[215], 1e-8);
        assertEquals(174.9154052734375, tiePoints[421], 1e-8);

        assertSameRasterParameter(lonGrid, normalizedLonGrid);
    }

    @Test
    public void testNormalizeLonGrid_anti_meridian_west_normalized() {
        final float[] floatLongitudes = RasterUtils.toFloat(AMSR2.AMSR2_ANTI_MERID_LON);
        final TiePointGrid lonGrid = new TiePointGrid("lon", 32, 26, 0.5, 0.5,
                1.0, 1.0, floatLongitudes);

        final TiePointGrid normalizedLonGrid = TiePointInverse.normalizeLonGrid(lonGrid);
        final float[] tiePoints = normalizedLonGrid.getTiePoints();
        assertEquals(183.38528442382812, tiePoints[0], 1e-8);
        assertEquals(180.509033203125, tiePoints[24], 1e-8);
        assertEquals(179.2598114013672, tiePoints[216], 1e-8);
        assertEquals(179.90513610839844, tiePoints[422], 1e-8);

        assertSameRasterParameter(lonGrid, normalizedLonGrid);
    }

    @Test
    public void testInitLatLonMinMax() {
        final TiePointInverse.Boundaries boundaries = TiePointInverse.initLatLonMinMax(RasterUtils.toFloat(MERIS.MER_RR_LON),
                RasterUtils.toFloat(MERIS.MER_RR_LAT));
        assertEquals(17.256619943847657, boundaries.normalizedLonMin, 1e-8);
        assertEquals(19.767063604125976, boundaries.normalizedLonMax, 1e-8);

        assertEquals(71.40375518798828, boundaries.latMin, 1e-8);
        assertEquals(72.23346710205078, boundaries.latMax, 1e-8);

        assertEquals(17.256619943847657, boundaries.overlapStart, 1e-8);
        assertEquals(19.767063604125976, boundaries.overlapEnd, 1e-8);
    }

    @Test
    public void testInitLatLonMinMax_overlap_start_corrected() {
        final TiePointInverse.Boundaries boundaries = TiePointInverse.initLatLonMinMax(new float[]{-181, -180, -179, -178},
                new float[]{11, 12, 13, 14});
        assertEquals(-181.00001, boundaries.normalizedLonMin, 1e-8);
        assertEquals(-177.99999, boundaries.normalizedLonMax, 1e-8);

        assertEquals(11.0, boundaries.latMin, 1e-8);
        assertEquals(14.0, boundaries.latMax, 1e-8);

        assertEquals(178.99999, boundaries.overlapStart, 1e-8);
        assertEquals(-177.99999, boundaries.overlapEnd, 1e-8);
    }

    @Test
    public void testInitLatLonMinMax_overlap_end_corrected() {
        final TiePointInverse.Boundaries boundaries = TiePointInverse.initLatLonMinMax(new float[]{179, 180, 181, 182},
                new float[]{12, 13, 14, 15});
        assertEquals(178.99999, boundaries.normalizedLonMin, 1e-8);
        assertEquals(182.00001, boundaries.normalizedLonMax, 1e-8);

        assertEquals(12.0, boundaries.latMin, 1e-8);
        assertEquals(15.0, boundaries.latMax, 1e-8);

        assertEquals(178.99999, boundaries.overlapStart, 1e-8);
        assertEquals(-177.99999, boundaries.overlapEnd, 1e-8);
    }

    @Test
    public void testDetermineWarpParameters() {
        int[] warpParameters = TiePointInverse.determineWarpParameters(100, 100);
        assertEquals(25, warpParameters[0]);
        assertEquals(34, warpParameters[1]);
        assertEquals(4, warpParameters[2]);
        assertEquals(3, warpParameters[3]);

        warpParameters = TiePointInverse.determineWarpParameters(39, 2728);
        assertEquals(20, warpParameters[0]);
        assertEquals(39, warpParameters[1]);
        assertEquals(2, warpParameters[2]);
        assertEquals(70, warpParameters[3]);
    }

    @Test
    public void testCreateWarpPoints() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 9, 9, 0.5, 0.5,
                3, 3, RasterUtils.toFloat(TestData.getSubSampled(3, AMSRE.AMSRE_HIGH_RES_LON, 25)));
        final TiePointGrid latGrid = new TiePointGrid("lat", 9, 9, 0.5, 0.5,
                3, 3, RasterUtils.toFloat(TestData.getSubSampled(3, AMSRE.AMSRE_HIGH_RES_LAT, 25)));
        final Rectangle subsetRect = new Rectangle(2, 1, 6, 6);

        final double[][] warpPoints = TiePointInverse.createWarpPoints(lonGrid, latGrid, subsetRect);
        assertEquals(36, warpPoints.length);
        assertEquals(4, warpPoints[0].length);
        assertEquals(-0.47030532360076904, warpPoints[0][0], 1e-8);
        assertEquals(18.45865821838379, warpPoints[0][1], 1e-8);
        assertEquals(6.5, warpPoints[0][2], 1e-8);
        assertEquals(3.5, warpPoints[0][3], 1e-8);

        assertEquals(4, warpPoints[17].length);
        assertEquals(0.496040403842926, warpPoints[17][0], 1e-8);
        assertEquals(17.90327262878418, warpPoints[17][1], 1e-8);
        assertEquals(21.5, warpPoints[17][2], 1e-8);
        assertEquals(9.5, warpPoints[17][3], 1e-8);
    }

    @Test
    public void testGetMaxSquareDistance() {
        final double[][] data = new double[][]{
                {73.79769, 46.10822},
                {73.75458, 46.358704},
                {73.82426, 45.794384},
                {73.78152, 46.046085}
        };

        final double maxSquareDistance = TiePointInverse.getMaxSquareDistance(data, 73.8, 46.2);
        assertEquals(0.1651128870560015, maxSquareDistance, 1e-8);
    }

    @Test
    public void testRescaleLatitude() {
        assertEquals(0.9711111111111111, TiePointInverse.rescaleLatitude(87.4), 1e-8);
        assertEquals(0.12777777777777777, TiePointInverse.rescaleLatitude(11.5), 1e-8);
        assertEquals(-0.8066666666666666, TiePointInverse.rescaleLatitude(-72.6), 1e-8);
    }

    @Test
    public void testRescaleLongitude() {
        assertEquals(0.1044444444444445, TiePointInverse.rescaleLongitude(97.5, 88.1), 1e-8);
        assertEquals(-0.74, TiePointInverse.rescaleLongitude(21.6, 88.2), 1e-8);
        assertEquals(-1.8988888888888886, TiePointInverse.rescaleLongitude(-82.6, 88.3), 1e-8);
    }

    @Test
    public void testGetBestPolynomial_linear() {
        final int[] xIndices = new int[]{0, 1, 2};
        final double[][] data = new double[][]{
                {-0.47030532360076904, 18.45865821838379, 6.5, 3.5},
                {-0.38233229517936707, 18.3734130859375, 9.5, 3.5},
                {-0.295753538608551, 18.286771774291992, 12.5, 3.5},
                {-0.21059183776378632, 18.198755264282227, 15.5, 3.5}
        };

        final FXYSum bestPolynomial = TiePointInverse.getBestPolynomial(data, xIndices);
        assertNotNull(bestPolynomial);
        assertTrue(bestPolynomial instanceof FXYSum.Linear);
    }

    @Test
    public void testGetBestPolynomial_linear_six_points() {
        final int[] xIndices = new int[]{0, 1, 2};
        final double[][] data = new double[][]{
                {-0.47030532360076904, 18.45865821838379, 6.5, 3.5},
                {-0.38233229517936707, 18.3734130859375, 9.5, 3.5},
                {-0.295753538608551, 18.286771774291992, 12.5, 3.5},
                // next line
                {-0.20022891461849213, 18.400646209716797, 6.5, 6.5},
                {-0.1122138500213623, 18.315458297729492, 9.5, 6.5},
                {-0.025592802092432976, 18.228870391845703, 12.5, 6.5},
        };

        final FXYSum bestPolynomial = TiePointInverse.getBestPolynomial(data, xIndices);
        assertNotNull(bestPolynomial);
        assertTrue(bestPolynomial instanceof FXYSum.Quadric);
    }

    @Test
    public void testCreateApproximation() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 5, 5, 0.5, 0.5,
                16, 16, RasterUtils.toFloat(MERIS.MER_RR_LON));
        final TiePointGrid latGrid = new TiePointGrid("lat", 5, 5, 0.5, 0.5,
                16, 16, RasterUtils.toFloat(MERIS.MER_RR_LAT));
        final Rectangle subsetRect = new Rectangle(2, 1, 3, 3);

        final Approximation approximation = TiePointInverse.createApproximation(lonGrid, latGrid, subsetRect);
        assertNotNull(approximation);
        assertEquals(18.94758203294542, approximation.getCenterLon(), 1e-8);
        assertEquals(71.76979488796658, approximation.getCenterLat(), 1e-8);
        assertEquals(0.4521953022058632, approximation.getMinSquareDistance(), 1e-8);
        assertTrue(approximation.getFX() instanceof FXYSum.Quadric);
        assertTrue(approximation.getFY() instanceof FXYSum.Quadric);
    }

    @Test
    public void testGetApproximations_MER_RR() {
        final TiePointGrid lonGrid = new TiePointGrid("lon", 5, 5, 0.5, 0.5,
                16, 16, RasterUtils.toFloat(MERIS.MER_RR_LON));
        final TiePointGrid latGrid = new TiePointGrid("lat", 5, 5, 0.5, 0.5,
                16, 16, RasterUtils.toFloat(MERIS.MER_RR_LAT));

        final Approximation[] approximations = TiePointInverse.getApproximations(lonGrid, latGrid);
        assertNotNull(approximations);
        assertEquals(1, approximations.length);

        final Approximation approximation = approximations[0];
        assertEquals(18.4977986907959, approximation.getCenterLon(), 1e-8);
        assertEquals(71.81846588134766, approximation.getCenterLat(), 1e-8);
        assertEquals(1.8236899446395034, approximation.getMinSquareDistance(), 1e-8);
        assertTrue(approximation.getFX() instanceof FXYSum.Quadric);
        //noinspection ConstantConditions
        assertTrue(approximation.getFY() instanceof FXYSum);
    }

    @Test
    public void testNormalizeLat() {
        assertEquals(NaN, TiePointInverse.normalizeLat(-91.1), 1e-8);
        assertEquals(-87.65, TiePointInverse.normalizeLat(-87.65), 1e-8);
        assertEquals(88.29, TiePointInverse.normalizeLat(88.29), 1e-8);
        assertEquals(NaN, TiePointInverse.normalizeLat(90.034), 1e-8);
    }

    @Test
    public void testNormalizeLon() {
        // beyond valid range
        assertEquals(NaN, TiePointInverse.normalizeLon(-180.1, 75, 85), 1e-8);
        assertEquals(NaN, TiePointInverse.normalizeLon(180.234, 75, 85), 1e-8);

        // normalize due to normalizeLonMin
        assertEquals(186.0, TiePointInverse.normalizeLon(-174, -165, 200), 1e-8);

        // normalize due to normalizeLonMin - picked out because out of range
        assertEquals(NaN, TiePointInverse.normalizeLon(-174, -165, 185), 1e-8);
        assertEquals(NaN, TiePointInverse.normalizeLon(-174, 190, 200), 1e-8);

        // and finally - the normal way
        assertEquals(14.0, TiePointInverse.normalizeLon(14, -165, 200), 1e-8);
    }

    @Test
    public void testGetBestApproximation() {
        final Approximation[] approximations = new Approximation[3];
        approximations[0] = new Approximation(null, null, 10, 20, 1);
        approximations[1] = new Approximation(null, null, 20, 30, 1);
        approximations[2] = new Approximation(null, null, 30, 40, 1);

        Approximation approximation = TiePointInverse.getBestApproximation(approximations, 10.4, 19.9);
        assertEquals(10.0, approximation.getCenterLat(), 1e-8);

        approximation = TiePointInverse.getBestApproximation(approximations, 29.85, 40.004);
        assertEquals(30.0, approximation.getCenterLat(), 1e-8);
    }

    @Test
    public void testGetBestApproximation_too_far_away() {
        final Approximation[] approximations = new Approximation[3];
        approximations[0] = new Approximation(null, null, 10, 20, 1);
        approximations[1] = new Approximation(null, null, 20, 30, 1);
        approximations[2] = new Approximation(null, null, 30, 40, 1);

        final Approximation approximation = TiePointInverse.getBestApproximation(approximations, 12.4, 12.9);
        assertNull(approximation);
    }

    @Test
    public void testGetBestApproximation_just_one() {
        final Approximation[] approximations = new Approximation[1];
        approximations[0] = new Approximation(null, null, 10, 20, 1);

        final Approximation approximation = TiePointInverse.getBestApproximation(approximations, 9.92, 20.045);
        assertEquals(10.0, approximation.getCenterLat(), 1e-8);
    }

    @Test
    public void testGetBestApproximation_just_one_too_far_away() {
        final Approximation[] approximations = new Approximation[1];
        approximations[0] = new Approximation(null, null, 10, 20, 1);

        final Approximation approximation = TiePointInverse.getBestApproximation(approximations, 11.9, 21.9);
        assertNull(approximation);
    }

    @Test
    public void testFindRenormalizedApproximation() {
        final Approximation[] approximations = new Approximation[3];
        approximations[0] = new Approximation(null, null, 10, 20, 1);
        approximations[1] = new Approximation(null, null, 30, 40, 1);
        approximations[2] = new Approximation(null, null, 50, 60, 1);

        final Approximation approximation = TiePointInverse.findRenormalizedApproximation(approximations, 29.98, 40.03, 0.5);
        assertNotNull(approximation);
        assertEquals(30, approximation.getCenterLat(), 1e-8);
    }

    @Test
    public void testFindRenormalizedApproximation_distance_too_large() {
        final Approximation[] approximations = new Approximation[3];
        approximations[0] = new Approximation(null, null, 10, 20, 1);
        approximations[1] = new Approximation(null, null, 30, 40, 1);
        approximations[2] = new Approximation(null, null, 50, 60, 1);

        final Approximation approximation = TiePointInverse.findRenormalizedApproximation(approximations, 29.9, 40.3, 0.05);
        assertNull(approximation);
    }

    @Test
    public void testDispose() {
        final TiePointInverse inverse = new TiePointInverse();
        inverse.dispose();

        final GeoRaster geoRaster = TestData.get_MER_RR();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        inverse.dispose();
    }

    @Test
    public void testGetPixelPos_MER_RR() {
        final TiePointInverse inverse = new TiePointInverse();
        final GeoRaster geoRaster = TestData.get_MER_RR();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(72.23347, 17.934608), null);
        assertEquals(0.49953691719531435, pixelPos.x, 1e-8);
        assertEquals(0.4992613374779962, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(72.03501, 19.767054), null);
        assertEquals(64.50022001607829, pixelPos.x, 1e-8);
        assertEquals(0.5002002253142156, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(71.403755, 19.034266), null);
        assertEquals(64.50034531684295, pixelPos.x, 1e-8);
        assertEquals(64.50045026784427, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(71.59687, 17.25663), null);
        assertEquals(0.4998781895296531, pixelPos.x, 1e-8);
        assertEquals(64.49991367701459, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_MER_RR_outside() {
        final TiePointInverse inverse = new TiePointInverse();
        final GeoRaster geoRaster = TestData.get_MER_RR();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        PixelPos pixelPos = inverse.getPixelPos(new GeoPos(72.34, 18.56), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(71.82, 20.03), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(71.34, 17.96), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);

        pixelPos = inverse.getPixelPos(new GeoPos(71.96, 17.05), null);
        assertEquals(NaN, pixelPos.x, 1e-8);
        assertEquals(NaN, pixelPos.y, 1e-8);
    }

    @Test
    public void testGetPixelPos_MER_RR_invalid_geoPos() {
        final TiePointInverse inverse = new TiePointInverse();
        final GeoRaster geoRaster = TestData.get_MER_RR();

        inverse.initialize(geoRaster, false, new PixelPos[0]);

        final PixelPos pixelPos = inverse.getPixelPos(new GeoPos(NaN, 18.56), null);
        assertTrue(Double.isNaN(pixelPos.x));
        assertTrue(Double.isNaN(pixelPos.y));
    }

    private void assertSameRasterParameter(TiePointGrid lonGrid, TiePointGrid normalizedLonGrid) {
        assertEquals(lonGrid.getGridWidth(), normalizedLonGrid.getGridWidth());
        assertEquals(lonGrid.getGridHeight(), normalizedLonGrid.getGridHeight());
        assertEquals(lonGrid.getSubSamplingX(), normalizedLonGrid.getSubSamplingX(), 1e-8);
        assertEquals(lonGrid.getSubSamplingY(), normalizedLonGrid.getSubSamplingY(), 1e-8);
        assertEquals(lonGrid.getOffsetX(), normalizedLonGrid.getOffsetX(), 1e-8);
        assertEquals(lonGrid.getOffsetY(), normalizedLonGrid.getOffsetY(), 1e-8);
    }
}
