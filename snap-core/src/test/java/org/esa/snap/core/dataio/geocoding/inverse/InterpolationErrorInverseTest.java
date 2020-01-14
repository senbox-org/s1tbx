package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.*;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InterpolationErrorInverseTest {

    private static final int MEAN = 0;
    private static final int MIN_X = 1;
    private static final int MAX_X = 2;
    private static final int MIN_Y = 3;
    private static final int MAX_Y = 4;

    @Test
    public void testTiePointInverse_AMSRE_3() {
        final InverseCoding inverseCoding = createTiePointInverse_AMSRE_3();

        final double[] errors = calculateMeanErrorInPixels(25, 25, inverseCoding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(0.018617607084496182, errors[MEAN], 1e-8);
        assertEquals(1.9444184616190796E-5, errors[MIN_X], 1e-8);
        assertEquals(0.24629321405415538, errors[MAX_X], 1e-8);
        assertEquals(1.0602195837705608E-5, errors[MIN_Y], 1e-8);
        assertEquals(0.11348830635786555, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testTiePointInverse_AMSUB_3_anti_meridian() {
        final InverseCoding inverseCoding = createTiePointInverse_AMSUB_3_anti_meridian();

        final double[] errors = calculateMeanErrorInPixels(31, 31, inverseCoding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(4.896357341838677E-4, errors[MEAN], 1e-8);
        assertEquals(2.209911329487113E-7, errors[MIN_X], 1e-8);
        assertEquals(0.003937304142990428, errors[MAX_X], 1e-8);
        assertEquals(1.4075091314680321E-7, errors[MIN_Y], 1e-8);
        assertEquals(0.004176857969538261, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testTiePointInverse_AMSRE_4() {
        final InverseCoding inverseCoding = createTiePointInverse_AMSRE_4();

        final double[] errors = calculateMeanErrorInPixels(25, 25, inverseCoding, AMSRE.AMSRE_HIGH_RES_LON, AMSRE.AMSRE_HIGH_RES_LAT);
        assertEquals(0.017544771094679418, errors[MEAN], 1e-8);
        assertEquals(5.120642210698634E-6, errors[MIN_X], 1e-8);
        assertEquals(0.2435989028775296, errors[MAX_X], 1e-8);
        assertEquals(7.745631425137844E-8, errors[MIN_Y], 1e-8);
        assertEquals(0.11133898874271608, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testTiePointInverse_AMSUB_5_anti_meridian() {
        final InverseCoding inverseCoding = createTiePointInverse_AMSUB_5_anti_meridian();

        final double[] errors = calculateMeanErrorInPixels(31, 31, inverseCoding, AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT);
        assertEquals(9.052621816634511E-4, errors[MEAN], 1e-8);
        assertEquals(2.434554289720836E-6, errors[MIN_X], 1e-8);
        assertEquals(0.01357558185304697, errors[MAX_X], 1e-8);
        assertEquals(6.824713167929986E-7, errors[MIN_Y], 1e-8);
        assertEquals(0.0021667037066173123, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelQuadTreeInverse_SLSTR_OL() {
        final InverseCoding inverseCoding = createPixelQuadTreeInverse_SLSTR_OL();

        final double[] errors = calculateMeanErrorInPixels(32, 26, inverseCoding, S3_SYN.SLSTR_OL_LON, S3_SYN.SLSTR_OL_LAT);
        assertEquals(0.15625, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(1.0, errors[MAX_X], 1e-8); // this is caused by a replicated pixels in the ESA data tb 2019-12-18
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelQuadTreeInverse_OLCI() {
        final InverseCoding inverseCoding = createPixelQuadTreeInverse_OLCI();

        final double[] errors = calculateMeanErrorInPixels(32, 36, inverseCoding, OLCI.OLCI_L2_LON, OLCI.OLCI_L2_LAT);
        assertEquals(0.375, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(1.0, errors[MAX_X], 1e-8); // this is caused by a replicated pixels in the ESA data tb 2019-12-19
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelQuadTreeInverse_AMSR2_anti_meridian() {
        final InverseCoding inverseCoding = createPixelQuadTreeInverse_AMSR2_anti_meridian();

        final double[] errors = calculateMeanErrorInPixels(32, 26, inverseCoding, AMSR2.AMSR2_ANTI_MERID_LON, AMSR2.AMSR2_ANTI_MERID_LAT);
        assertEquals(0.0, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(0.0, errors[MAX_X], 1e-8);
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelQuadTreeInverse_MER_FSG() {
        final InverseCoding inverseCoding = createPixelQuadTreeInverse_MER_FSG();

        final double[] errors = calculateMeanErrorInPixels(26, 35, inverseCoding, MERIS.MER_FSG_LON, MERIS.MER_FSG_LAT);
        assertEquals(0.23076923076923078, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(1.0, errors[MAX_X], 1e-8); // this is caused by a replicated pixels in the ESA data tb 2020-01-06
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelGeoIndexInverse_SLSTR_OL() {
        final InverseCoding inverseCoding = createPixelGeoIndexInverse_SLSTR_OL();

        final double[] errors = calculateMeanErrorInPixels(32, 26, inverseCoding, S3_SYN.SLSTR_OL_LON, S3_SYN.SLSTR_OL_LAT);
        assertEquals(0.15625, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(1.0, errors[MAX_X], 1e-8); // this is caused by a replicated pixels in the ESA data tb 2019-12-18
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelGeoIndexInverse_OLCI() {
        final InverseCoding inverseCoding = createPixelGeoIndexInverse_OLCI();

        final double[] errors = calculateMeanErrorInPixels(32, 36, inverseCoding, OLCI.OLCI_L2_LON, OLCI.OLCI_L2_LAT);
        assertEquals(0.375, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(1.0, errors[MAX_X], 1e-8); // this is caused by a replicated pixels in the ESA data tb 2019-12-19
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelGeoIndexInverse_AMSR2_anti_meridian() {
        final InverseCoding inverseCoding = createPixelGeoIndexInverse_AMSR2_anti_meridian();

        final double[] errors = calculateMeanErrorInPixels(32, 26, inverseCoding, AMSR2.AMSR2_ANTI_MERID_LON, AMSR2.AMSR2_ANTI_MERID_LAT);
        assertEquals(0.0, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(0.0, errors[MAX_X], 1e-8);
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    @Test
    public void testPixelGeoIndexInverse_MER_FSG() {
        final InverseCoding inverseCoding = createPixelGeoIndexInverse_MER_FSG();

        final double[] errors = calculateMeanErrorInPixels(26, 35, inverseCoding, MERIS.MER_FSG_LON, MERIS.MER_FSG_LAT);
        assertEquals(0.23076923076923078, errors[MEAN], 1e-8);
        assertEquals(0.0, errors[MIN_X], 1e-8);
        assertEquals(1.0, errors[MAX_X], 1e-8); // this is caused by a replicated pixels in the ESA data tb 2020-01-06
        assertEquals(0.0, errors[MIN_Y], 1e-8);
        assertEquals(0.0, errors[MAX_Y], 1e-8);
    }

    private double[] calculateMeanErrorInPixels(int width, int height, InverseCoding inverse, double[] lonRaster, double[] latRaster) {
        final double[] result = new double[5];
        double min_delta_x = Double.MAX_VALUE;
        double max_delta_x = Double.MIN_VALUE;
        double min_delta_y = Double.MAX_VALUE;
        double max_delta_y = Double.MIN_VALUE;
        double distanceSum = 0.0;
        for (int y = 0; y < height; y++) {
            final int lineOffset = y * width;
            final double realY = y + 0.5;

            for (int x = 0; x < width; x++) {
                final int index = lineOffset + x;

                final PixelPos pixelPos = inverse.getPixelPos(new GeoPos(latRaster[index], lonRaster[index]), null);

                final double realX = x + 0.5;

                final double deltaX = Math.abs(realX - pixelPos.x);
                final double deltaY = Math.abs(realY - pixelPos.y);
                final double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                if (deltaX < min_delta_x) {
                    min_delta_x = deltaX;
                }
                if (deltaX > max_delta_x) {
                    max_delta_x = deltaX;
                }
                if (deltaY < min_delta_y) {
                    min_delta_y = deltaY;
                }
                if (deltaY > max_delta_y) {
                    max_delta_y = deltaY;
                }
                distanceSum += distance;
            }
        }

        result[MEAN] = distanceSum / (width * height);
        result[MIN_X] = min_delta_x;
        result[MAX_X] = max_delta_x;
        result[MIN_Y] = min_delta_y;
        result[MAX_Y] = max_delta_y;

        return result;
    }

    private InverseCoding createTiePointInverse_AMSRE_3() {
        final GeoRaster geoRaster = TestData.get_AMSRE_subs_3();

        final TiePointInverse inverse = new TiePointInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createTiePointInverse_AMSRE_4() {
        final GeoRaster geoRaster = TestData.get_AMSRE_subs_4();

        final TiePointInverse inverse = new TiePointInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createTiePointInverse_AMSUB_3_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSUB_subs_3_anti_meridian();
        final TiePointInverse inverse = new TiePointInverse();
        inverse.initialize(geoRaster, true, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createTiePointInverse_AMSUB_5_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSUB_subs_5_anti_meridian();

        final TiePointInverse inverse = new TiePointInverse();
        inverse.initialize(geoRaster, true, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelQuadTreeInverse_SLSTR_OL() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        final PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelQuadTreeInverse_OLCI() {
        final GeoRaster geoRaster = TestData.get_OLCI();

        final PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelQuadTreeInverse_AMSR2_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSR_2_anti_meridian();

        final PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, true, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelQuadTreeInverse_MER_FSG() {
        final GeoRaster geoRaster = TestData.get_MER_FSG();

        final PixelQuadTreeInverse inverse = new PixelQuadTreeInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelGeoIndexInverse_SLSTR_OL() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        final PixelGeoIndexInverse inverse = new PixelGeoIndexInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelGeoIndexInverse_OLCI() {
        final GeoRaster geoRaster = TestData.get_OLCI();

        final PixelGeoIndexInverse inverse = new PixelGeoIndexInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelGeoIndexInverse_AMSR2_anti_meridian() {
        final GeoRaster geoRaster = TestData.get_AMSR_2_anti_meridian();

        final PixelGeoIndexInverse inverse = new PixelGeoIndexInverse();
        inverse.initialize(geoRaster, true, new PixelPos[0]);
        return inverse;
    }

    private InverseCoding createPixelGeoIndexInverse_MER_FSG() {
        final GeoRaster geoRaster = TestData.get_MER_FSG();

        final PixelGeoIndexInverse inverse = new PixelGeoIndexInverse();
        inverse.initialize(geoRaster, false, new PixelPos[0]);
        return inverse;
    }
}
