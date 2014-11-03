package org.esa.nest.gpf.geometric;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by lveci on 24/10/2014.
 */
public class TestUpdateGeoRef {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new UpdateGeoRefOp.Spi();

    private String[] exceptionExemptions = {};

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final File inputFile = TestData.inputASAR_WSM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final UpdateGeoRefOp op = (UpdateGeoRefOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final float[] expected = new float[] { 446224.0f,318096.0f,329476.0f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), expected);

        final GeoCoding geoCoding = targetProduct.getGeoCoding();
        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(100, 100), null);
        assertEquals(46.727386, geoPos.getLat(), 0.00001);
        assertEquals(10.363166, geoPos.getLon(), 0.00001);
    }

    @Test
    public void testProcessAllALOS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsALOS, "ALOS PALSAR CEOS", null, exceptionExemptions);
    }
}
