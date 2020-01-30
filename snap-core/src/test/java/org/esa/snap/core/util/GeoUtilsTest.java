package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class GeoUtilsTest {

    @Test
    public void testCreateRectBoundary_usePixelCenter_false() {
        final boolean usePixelCenter = false;
        final PixelPos[] rectBoundary = GeoUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7,
                usePixelCenter);
        assertEquals(12, rectBoundary.length);
        assertEquals(new PixelPos(2, 3), rectBoundary[0]);
        assertEquals(new PixelPos(9, 3), rectBoundary[1]);
        assertEquals(new PixelPos(16, 3), rectBoundary[2]);
        assertEquals(new PixelPos(17, 3), rectBoundary[3]);
        assertEquals(new PixelPos(17, 10), rectBoundary[4]);
        assertEquals(new PixelPos(17, 17), rectBoundary[5]);
        assertEquals(new PixelPos(17, 23), rectBoundary[6]);
        assertEquals(new PixelPos(16, 23), rectBoundary[7]);
        assertEquals(new PixelPos(9, 23), rectBoundary[8]);
        assertEquals(new PixelPos(2, 23), rectBoundary[9]);
        assertEquals(new PixelPos(2, 17), rectBoundary[10]);
        assertEquals(new PixelPos(2, 10), rectBoundary[11]);
    }

    @Test
    public void testCreateRectBoundary_usePixelCenter_true() {
        final boolean usePixelCenter = true;
        final PixelPos[] rectBoundary = GeoUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 7, usePixelCenter);
        assertEquals(10, rectBoundary.length);
        assertEquals(new PixelPos(2.5f, 3.5f), rectBoundary[0]);
        assertEquals(new PixelPos(9.5f, 3.5f), rectBoundary[1]);
        assertEquals(new PixelPos(16.5f, 3.5f), rectBoundary[2]);
        assertEquals(new PixelPos(16.5f, 10.5f), rectBoundary[3]);
        assertEquals(new PixelPos(16.5f, 17.5f), rectBoundary[4]);
        assertEquals(new PixelPos(16.5f, 22.5f), rectBoundary[5]);
        assertEquals(new PixelPos(9.5f, 22.5f), rectBoundary[6]);
        assertEquals(new PixelPos(2.5f, 22.5f), rectBoundary[7]);
        assertEquals(new PixelPos(2.5f, 17.5f), rectBoundary[8]);
        assertEquals(new PixelPos(2.5f, 10.5f), rectBoundary[9]);
    }

    @Test
    public void testCreateRectBoundary_usePixelCenter_true_step_zero() {
        final boolean usePixelCenter = true;
        final PixelPos[] rectBoundary = GeoUtils.createRectBoundary(new Rectangle(2, 3, 15, 20), 0, usePixelCenter);
        assertEquals(4, rectBoundary.length);
        assertEquals(new PixelPos(2.5f, 3.5f), rectBoundary[0]);
        assertEquals(new PixelPos(16.5f, 3.5f), rectBoundary[1]);
        assertEquals(new PixelPos(16.5f, 22.5f), rectBoundary[2]);
        assertEquals(new PixelPos(2.5f, 22.5f), rectBoundary[3]);
    }

    @Test
    public void testCreateRectBoundary() {
        final PixelPos[] rectBoundary = GeoUtils.createRectBoundary(new Rectangle(5, 10, 22, 44), 10);
        assertEquals(16, rectBoundary.length);
        assertEquals(new PixelPos(5.5, 10.5), rectBoundary[0]);
        assertEquals(new PixelPos(15.5, 10.5), rectBoundary[1]);
        assertEquals(new PixelPos(25.5, 10.5), rectBoundary[2]);
        assertEquals(new PixelPos(26.5, 10.5), rectBoundary[3]);
        assertEquals(new PixelPos(26.5, 20.5), rectBoundary[4]);
        assertEquals(new PixelPos(26.5, 30.5), rectBoundary[5]);
        assertEquals(new PixelPos(26.5, 40.5), rectBoundary[6]);
        assertEquals(new PixelPos(26.5, 50.5), rectBoundary[7]);
        assertEquals(new PixelPos(26.5, 53.5), rectBoundary[8]);
        assertEquals(new PixelPos(25.5, 53.5), rectBoundary[9]);
        assertEquals(new PixelPos(15.5, 53.5), rectBoundary[10]);
        assertEquals(new PixelPos(5.5, 53.5), rectBoundary[11]);
        assertEquals(new PixelPos(5.5, 50.5), rectBoundary[12]);
        assertEquals(new PixelPos(5.5, 40.5), rectBoundary[13]);
        assertEquals(new PixelPos(5.5, 30.5), rectBoundary[14]);
        assertEquals(new PixelPos(5.5, 20.5), rectBoundary[15]);
    }

    @Test
    public void testCreatePixelBoundary_rasterDataNode_without_rect() {
        final RasterDataNode rasterDataNode = mock(RasterDataNode.class);
        when(rasterDataNode.getRasterWidth()).thenReturn(40);
        when(rasterDataNode.getRasterHeight()).thenReturn(60);

        final PixelPos[] pixelBoundary = GeoUtils.createPixelBoundary(rasterDataNode, null, 15);
        assertEquals(14, pixelBoundary.length);
        assertEquals(new PixelPos(0.5, 0.5), pixelBoundary[0]);
        assertEquals(new PixelPos(15.5, 0.5), pixelBoundary[1]);
        assertEquals(new PixelPos(30.5, 0.5), pixelBoundary[2]);
        assertEquals(new PixelPos(39.5, 0.5), pixelBoundary[3]);
        assertEquals(new PixelPos(39.5, 15.5), pixelBoundary[4]);
        assertEquals(new PixelPos(39.5, 30.5), pixelBoundary[5]);
        assertEquals(new PixelPos(39.5, 45.5), pixelBoundary[6]);
        assertEquals(new PixelPos(39.5, 59.5), pixelBoundary[7]);
        assertEquals(new PixelPos(30.5, 59.5), pixelBoundary[8]);
        assertEquals(new PixelPos(15.5, 59.5), pixelBoundary[9]);
        assertEquals(new PixelPos(0.5, 59.5), pixelBoundary[10]);
        assertEquals(new PixelPos(0.5, 45.5), pixelBoundary[11]);
        assertEquals(new PixelPos(0.5, 30.5), pixelBoundary[12]);
        assertEquals(new PixelPos(0.5, 15.5), pixelBoundary[13]);

        verify(rasterDataNode, times(1)).getRasterWidth();
        verify(rasterDataNode, times(1)).getRasterHeight();
        verifyNoMoreInteractions(rasterDataNode);
    }

    @Test
    public void testCreatePixelBoundary_width_height_with_rect() {
        final PixelPos[] pixelBoundary = GeoUtils.createPixelBoundary(120, 240, new Rectangle(10, 10, 20, 30), 15);
        assertEquals(8, pixelBoundary.length);
        assertEquals(new PixelPos(10.5, 10.5), pixelBoundary[0]);
        assertEquals(new PixelPos(25.5, 10.5), pixelBoundary[1]);
        assertEquals(new PixelPos(29.5, 10.5), pixelBoundary[2]);
        assertEquals(new PixelPos(29.5, 25.5), pixelBoundary[3]);
        assertEquals(new PixelPos(29.5, 39.5), pixelBoundary[4]);
        assertEquals(new PixelPos(25.5, 39.5), pixelBoundary[5]);
        assertEquals(new PixelPos(10.5, 39.5), pixelBoundary[6]);
        assertEquals(new PixelPos(10.5, 25.5), pixelBoundary[7]);
    }

    @Test
    public void testCreatePixelBoundary_width_height() {
        final PixelPos[] pixelBoundary = GeoUtils.createPixelBoundary(18, 24, null, 10);
        assertEquals(10, pixelBoundary.length);
        assertEquals(new PixelPos(0.5, 0.5), pixelBoundary[0]);
        assertEquals(new PixelPos(10.5, 0.5), pixelBoundary[1]);
        assertEquals(new PixelPos(17.5, 0.5), pixelBoundary[2]);
        assertEquals(new PixelPos(17.5, 10.5), pixelBoundary[3]);
        assertEquals(new PixelPos(17.5, 20.5), pixelBoundary[4]);
        assertEquals(new PixelPos(17.5, 23.5), pixelBoundary[5]);
        assertEquals(new PixelPos(10.5, 23.5), pixelBoundary[6]);
        assertEquals(new PixelPos(0.5, 23.5), pixelBoundary[7]);
    }

    @Test
    public void testCreateGeoBoundary() {
        final Product slstrProduct = createSLSTR();

        final GeoPos[] geoBoundary = GeoUtils.createGeoBoundary(slstrProduct, null, 5, true);
        assertEquals(8, geoBoundary.length);

        assertEquals(-75.19421818715499, geoBoundary[0].lon, 1e-8);
        assertEquals(19.379739611214635, geoBoundary[0].lat, 1e-8);

        assertEquals(-76.25204980046607, geoBoundary[3].lon, 1e-8);
        assertEquals(19.24891813421567, geoBoundary[3].lat, 1e-8);

        assertEquals(-75.20218073710458, geoBoundary[7].lon, 1e-8);
        assertEquals(19.423577996240176, geoBoundary[7].lat, 1e-8);
    }

    @Test
    public void testCreateGeoBoundary_noGeoCoding() {
        final Product slstrProduct = createSLSTR();
        slstrProduct.setSceneGeoCoding(null);

        try {
            GeoUtils.createGeoBoundary(slstrProduct, null, 5, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected){
        }
    }


    private static Product createSLSTR() {
        final Product product = new Product("slstr-test", "TEST_TYPE", 8, 10);

        final TestGeoCoding geoCoding = new TestGeoCoding(SL_LON, SL_LAT, 8);
        product.setSceneGeoCoding(geoCoding);
        return product;
    }

    private static class TestGeoCoding implements GeoCoding {
        private final double[] longitudes;
        private final double[] latitudes;
        private final int width;
        private final int height;

        public TestGeoCoding(double[] longitudes, double[] latitudes, int width) {
            this.longitudes = longitudes;
            this.latitudes = latitudes;
            this.width = width;
            this.height = latitudes.length / width;
        }

        @Override
        public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.setInvalid();

            final int x = (int) Math.floor(pixelPos.x);
            final int y = (int) Math.floor(pixelPos.y);
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return geoPos;
            }

            final int index = y * width + x;
            geoPos.lon = longitudes[index];
            geoPos.lat = latitudes[index];

            return geoPos;
        }

        @Override
        public boolean isCrossingMeridianAt180() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean canGetPixelPos() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public boolean canGetGeoPos() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public Datum getDatum() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void dispose() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public CoordinateReferenceSystem getImageCRS() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public CoordinateReferenceSystem getMapCRS() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public CoordinateReferenceSystem getGeoCRS() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public MathTransform getImageToMapTransform() {
            throw new RuntimeException("not implemented");
        }
    }

    // SLSTR longitudes 8 x 10 px
    private static final double[] SL_LON = {
            -75.19421818715499, -75.34429272327932, -75.49432217252226, -75.64430638510662, -75.79424521269003, -75.94413850836722, -76.0939861266723, -76.24378792358118,
            -75.1958106971449, -75.34589384325557, -75.49593188360475, -75.64592466818468, -75.79587204842338, -75.94577387718698, -76.09563000878211, -76.24544029895816,
            -75.19740320713483, -75.34749496323181, -75.49754159468725, -75.64754295126274, -75.79749888415672, -75.94740924600674, -76.09727389089191, -76.24709267433514,
            -75.19899571712475, -75.34909608320805, -75.49915130576974, -75.6491612343408, -75.79912571989007, -75.94904461482652, -76.09891777300172, -76.24874504971211,
            -75.20058822711466, -75.3506972031843, -75.50076101685225, -75.65077951741887, -75.8007525556234, -75.9506799836463, -76.10056165511152, -76.25039742508909,
            -75.20218073710458, -75.35229832316054, -75.50237072793475, -75.65239780049693, -75.80237939135675, -75.95231535246606, -76.10220553722132, -76.25204980046607,
            -75.2037732470945, -75.35389944313678, -75.50398043901724, -75.65401608357499, -75.8040062270901, -75.95395072128582, -76.10384941933113, -76.25370217584305,
            -75.20536575708442, -75.35550056311303, -75.50559015009975, -75.65563436665306, -75.80563306282343, -75.9555860901056, -76.10549330144093, -76.25535455122002,
            -75.20695826707434, -75.35710168308927, -75.50719986118224, -75.65725264973112, -75.80725989855678, -75.95722145892537, -76.10713718355073, -76.257006926597,
            -75.20855077706426, -75.35870280306551, -75.50880957226474, -75.65887093280918, -75.80888673429013, -75.95885682774514, -76.10878106566054, -76.25865930197398

    };

    // SLSTR latitudes 8 x 10 px
    private static final double[] SL_LAT = {
            19.379739611214635, 19.3551428922283, 19.330422556557192, 19.305578809926615, 19.280611858875773, 19.25552191075319, 19.230309173712357, 19.20497385670721,
            19.388507288219742, 19.363913778579516, 19.339196590922832, 19.314355931003995, 19.289392005391896, 19.264305021465443, 19.239095187409212, 19.2137627122089,
            19.397274965224852, 19.372684664930734, 19.347970625288475, 19.323133052081378, 19.29817215190802, 19.273088132177698, 19.24788120110607, 19.222551567710592,
            19.40604264222996, 19.38145555128195, 19.35674465965412, 19.33191017315876, 19.30695229842414, 19.28187124288995, 19.25666721480292, 19.231340423212284,
            19.414810319235066, 19.390226437633164, 19.365518694019762, 19.340687294236144, 19.315732444940263, 19.290654353602203, 19.265453228499773, 19.240129278713976,
            19.423577996240176, 19.39899732398438, 19.374292728385406, 19.349464415313527, 19.324512591456383, 19.299437464314458, 19.27423924219663, 19.24891813421567,
            19.432345673245283, 19.407768210335593, 19.38306676275105, 19.35824153639091, 19.333292737972506, 19.30822057502671, 19.283025255893484, 19.257706989717363,
            19.44111335025039, 19.41653909668681, 19.391840797116693, 19.36701865746829, 19.34207288448863, 19.317003685738964, 19.291811269590337, 19.266495845219055,
            19.4498810272555, 19.425309983038026, 19.400614831482336, 19.375795778545672, 19.35085303100475, 19.325786796451215, 19.30059728328719, 19.275284700720746,
            19.458648704260607, 19.43408086938924, 19.40938886584798, 19.384572899623056, 19.359633177520873, 19.33456990716347, 19.309383296984045, 19.284073556222438
    };
}
