package org.esa.snap.core.dataio.geocoding.forward;

import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.dataio.geocoding.util.EllipsoidDistance;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.junit.Test;

import static org.esa.snap.core.dataio.geocoding.TestData.*;
import static org.junit.Assert.assertEquals;

public class InterpolationErrorFwdTest {

    private static final int MEAN = 0;
    private static final int MIN = 1;
    private static final int MAX = 2;

    @Test
    public void testPixelForward_AMSRE() {
        final ForwardCoding coding = createPixelForward_AMSRE();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(0.0, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(0.0, errors[MAX], 1e-8);
    }

    @Test
    public void testPixelInterpolatingForward_AMSRE() {
        final ForwardCoding coding = createPixelInterpolatingForward_AMSRE();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(0.0, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(0.0, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointBilinearForward_AMSRE_3() {
        final ForwardCoding coding = createBilinearForward_AMSRE_3();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(23.736938835991822, errors[MEAN], 1e-8);
        assertEquals(0.001830998340783373, errors[MIN], 1e-8);
        assertEquals(57.797816807220315, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointSplineForward_AMSRE_3() {
        final ForwardCoding coding = createSplineForward_AMSRE_3();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(19.127992216866875, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(72.33830108811071, errors[MAX], 1e-8);
    }

    @Test
    public void testPixelForward_AMSUB_anti_meridian() {
        final ForwardCoding coding = createPixelForward_AMSUB();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(0.0, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(0.0, errors[MAX], 1e-8);
    }

    @Test
    public void testPixelInterpolatingForward_AMSUB_anti_meridian() {
        final ForwardCoding coding = createPixelInterpolatingForward_AMSUB();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(0.0, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(0.0, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointBilinearForward_AMSUB_anti_meridian_3() {
        final ForwardCoding coding = createBilinearForward_AMSUB_3_anti_meridian();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(160.31637755061445, errors[MEAN], 1e-8);
        assertEquals(0.00202828258210273866, errors[MIN], 1e-8);
        assertEquals(426.9728208000251, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointSplineForward_AMSUB_anti_meridian_3() {
        final ForwardCoding coding = createSplineForward_AMSUB_3_anti_meridian();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(81.68422208595456, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(288.52200386415717, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointBilinearForward_AMSRE_4() {
        final ForwardCoding coding = createBilinearForward_AMSRE_4();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(37.73394611165732, errors[MEAN], 1e-8);
        assertEquals(0.001830998340783373, errors[MIN], 1e-8);
        assertEquals(95.01026730865547, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointSplineForward_AMSRE_4() {
        final ForwardCoding coding = createSplineForward_AMSRE_4();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(21.612987014037213, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(51.88189356600489, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointBilinearForward_AMSUB_anti_meridian_5() {
        final ForwardCoding coding = createBilinearForward_AMSUB_anti_meridian_5();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(457.01232264076356, errors[MEAN], 1e-8);
        assertEquals(0.0020282825821027386, errors[MIN], 1e-8);
        assertEquals(1217.4803404626819, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointSplineForward_AMSUB_anti_meridian_5() {
        final ForwardCoding coding = createSplineForward_AMSUB_anti_meridian_5();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(240.82516776291124, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(770.2630404441957, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointBilinearForward_AMSRE_6() {
        final ForwardCoding coding = createBilinearForward_AMSRE_6();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(72.85015740857165, errors[MEAN], 1e-8);
        assertEquals(0.001830998340783373, errors[MIN], 1e-8);
        assertEquals(148.6995050465971, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointSplineForward_AMSRE_6() {
        final ForwardCoding coding = createSplineForward_AMSRE_6();

        final double[] errors = calculateMeanErrorInMeters(25, 25, coding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(40.31578487773834, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(95.88009370075234, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointBilinearForward_AMSUB_anti_meridian_6() {
        final ForwardCoding coding = createBilinearForward_AMSUB_anti_meridian_6();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(658.3380644452722, errors[MEAN], 1e-8);
        assertEquals(0.0020282825821027386, errors[MIN], 1e-8);
        assertEquals(1750.0701714467477, errors[MAX], 1e-8);
    }

    @Test
    public void testTiePointSplineForward_AMSUB_anti_meridian_6() {
        final ForwardCoding coding = createSplineForward_AMSUB_anti_meridian_6();

        final double[] errors = calculateMeanErrorInMeters(31, 31, coding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(312.67358500224475, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN], 1e-8);
        assertEquals(788.5714025558733, errors[MAX], 1e-8);
    }

    private double[] calculateMeanErrorInMeters(int width, int height, ForwardCoding coding, double[] lonRaster, double[] latRaster) {
        final double[] result = new double[3];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double distanceSum = 0.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final GeoPos geoPos = coding.getGeoPos(new PixelPos(x + 0.5, y + 0.5), null);

                final int index = y * width + x;
                final double realLon = lonRaster[index];
                final double realLat = latRaster[index];

                final EllipsoidDistance ellipsoidDistance = new EllipsoidDistance(realLon, realLat, DefaultEllipsoid.WGS84);
                final double distance = ellipsoidDistance.distance(geoPos.lon, geoPos.lat);
                if (distance > max) {
                    max = distance;
                }
                if (distance < min) {
                    min = distance;
                }
                distanceSum += distance;
            }
        }

        result[MEAN] = distanceSum / (width * height);
        result[MIN] = min;
        result[MAX] = max;
        return result;
    }

    private ForwardCoding createPixelForward_AMSRE() {
        final GeoRaster geoRaster = get_AMSRE();

        final PixelForward coding = new PixelForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createPixelInterpolatingForward_AMSRE() {
        final GeoRaster geoRaster = get_AMSRE();

        final PixelInterpolatingForward coding = new PixelInterpolatingForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createBilinearForward_AMSRE_3() {
        final GeoRaster geoRaster = get_AMSRE_subs_3();

        final TiePointBilinearForward coding = new TiePointBilinearForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createSplineForward_AMSRE_3() {
        final GeoRaster geoRaster = get_AMSRE_subs_3();
        final TiePointSplineForward coding = new TiePointSplineForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createPixelForward_AMSUB() {
        final GeoRaster geoRaster = get_AMSUB();

        final PixelForward coding = new PixelForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createPixelInterpolatingForward_AMSUB() {
        final GeoRaster geoRaster = get_AMSUB();

        final PixelInterpolatingForward coding = new PixelInterpolatingForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createBilinearForward_AMSUB_3_anti_meridian() {
        final GeoRaster geoRaster = get_AMSUB_subs_3_anti_meridian();

        final TiePointBilinearForward coding = new TiePointBilinearForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createSplineForward_AMSUB_3_anti_meridian() {
        final GeoRaster geoRaster = get_AMSUB_subs_3_anti_meridian();

        final TiePointSplineForward coding = new TiePointSplineForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createBilinearForward_AMSRE_4() {
        final GeoRaster geoRaster = TestData.get_AMSRE_subs_4();

        final TiePointBilinearForward coding = new TiePointBilinearForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createSplineForward_AMSRE_4() {
        final GeoRaster geoRaster = TestData.get_AMSRE_subs_4();

        final TiePointSplineForward coding = new TiePointSplineForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createBilinearForward_AMSUB_anti_meridian_5() {
        final GeoRaster geoRaster = get_AMSUB_subs_5_anti_meridian();

        final TiePointBilinearForward coding = new TiePointBilinearForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createSplineForward_AMSUB_anti_meridian_5() {
        final GeoRaster geoRaster = get_AMSUB_subs_5_anti_meridian();

        final TiePointSplineForward coding = new TiePointSplineForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createBilinearForward_AMSRE_6() {
        final GeoRaster geoRaster = get_AMSRE_subs_6();

        final TiePointBilinearForward coding = new TiePointBilinearForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createSplineForward_AMSRE_6() {
        final GeoRaster geoRaster = get_AMSRE_subs_6();

        final TiePointSplineForward coding = new TiePointSplineForward();
        coding.initialize(geoRaster, false, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createBilinearForward_AMSUB_anti_meridian_6() {
        final GeoRaster geoRaster = get_AMSUB_subs_6_anti_meridian();

        final TiePointBilinearForward coding = new TiePointBilinearForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }

    private ForwardCoding createSplineForward_AMSUB_anti_meridian_6() {
        final GeoRaster geoRaster = get_AMSUB_subs_6_anti_meridian();

        final TiePointSplineForward coding = new TiePointSplineForward();
        coding.initialize(geoRaster, true, new PixelPos[0]);
        return coding;
    }
}
