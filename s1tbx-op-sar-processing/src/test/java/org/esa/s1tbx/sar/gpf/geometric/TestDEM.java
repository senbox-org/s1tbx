package org.esa.s1tbx.sar.gpf.geometric;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.dem.gpf.AddElevationOp;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by lveci on 24/10/2014.
 */
@Ignore("Different values on lin/win coming from geotiff reader")
public class TestDEM {

    private final static OperatorSpi spi = new AddElevationOp.Spi();

    private static float[] expectedValues = {
            2684.945f, 3127.9426f, 1614.7288f, 2583.1665f, 2906.9287f, 2384.6487f, 2998.3179f, 1949.5698f
    };

    private static float[] expectedValuesLinux = {
            2665.3777f, 3070.6567f, 1605.4856f, 2604.9739f, 2926.6204f, 2362.638f, 2926.9727f, 1936.5419f
    };

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        int w = 10, h = 10;
        final Product sourceProduct = TestUtils.createProduct("GRD", w,h);

        GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        GeoPos geoPos1 = geoCoding.getGeoPos(new PixelPos(0,0), null);
        assertEquals(47.01368163108826, geoPos1.lat, 1e-8);
        assertEquals(11.150777896003394, geoPos1.lon, 1e-8);

        GeoPos geoPos2 = geoCoding.getGeoPos(new PixelPos(w,0), null);
        assertEquals(47.09209979057312, geoPos2.lat, 1e-8);
        assertEquals(10.60330894340638, geoPos2.lon, 1e-8);

        GeoPos geoPos3 = geoCoding.getGeoPos(new PixelPos(w,h), null);
        assertEquals(46.73274329185486, geoPos3.lat, 1e-8);
        assertEquals(10.495820911551409, geoPos3.lon, 1e-8);

        GeoPos geoPos4 = geoCoding.getGeoPos(new PixelPos(0,h), null);
        assertEquals(46.654466276168826, geoPos4.lat, 1e-8);
        assertEquals(11.039697677677857, geoPos4.lon, 1e-8);

        final AddElevationOp op = (AddElevationOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        op.doExecute(ProgressMonitor.NULL);
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final Band elevBand = targetProduct.getBand("elevation");
        assertNotNull(elevBand);
        assertEquals("SRTM 3Sec", elevBand.getDescription());
        assertEquals(-32768.0, elevBand.getNoDataValue(), 1e-8);

        final double[] demValues = new double[w*h];
        elevBand.readPixels(0, 0, w, h, demValues, ProgressMonitor.NULL);

        TestUtils.comparePixels(targetProduct, elevBand.getName(), expectedValuesLinux);
    }
}
