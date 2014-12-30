package org.esa.nest.gpf.geometric;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Created by lveci on 24/10/2014.
 */
public class TestALOSDeskew {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new ALOSDeskewingOp.Spi();

    private String[] exceptionExemptions = {"PALSAR products only"};

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final File inputFile = TestData.inputALOS1_1;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final ALOSDeskewingOp op = (ALOSDeskewingOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final float[] expected = new float[] { 86331.03f, 36645.12f, 14375.038f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), 300, 400, expected);
    }

    @Test
    public void testProcessAllALOS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsALOS, "ALOS PALSAR CEOS", null, exceptionExemptions);
    }
}
