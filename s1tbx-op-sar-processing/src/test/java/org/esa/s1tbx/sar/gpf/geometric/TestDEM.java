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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by lveci on 24/10/2014.
 */
public class TestDEM {

    private final static OperatorSpi spi = new AddElevationOp.Spi();

    private static float[] expectedValues = {
            537.56757f, 537.56757f, 537.56757f, 537.56757f, 537.56757f, 537.56757f, 537.56757f, 537.56757f
    };

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final Product sourceProduct = TestUtils.createProduct("GRD", 10,10);

        GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(0,0), null);
        assertEquals(10.25, geoPos.lat, 1e-8);
        assertEquals(10.249634044222699, geoPos.lon, 1e-8);

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

        final double[] demValues = new double[8];
        elevBand.readPixels(0, 0, 4, 2, demValues, ProgressMonitor.NULL);

        TestUtils.comparePixels(targetProduct, elevBand.getName(), expectedValues);
    }
}
