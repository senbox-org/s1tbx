package org.esa.s1tbx.gpf.geometric;

import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by lveci on 24/10/2014.
 */
public class TestMosaic {

    private final static OperatorSpi spi = new MosaicOp.Spi();

    /**
     * Processes a product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    public void testProcessing() throws Exception {
        final File inputFile1 = TestData.inputASAR_IMM;
        if (!inputFile1.exists()) {
            TestUtils.skipTest(this, inputFile1 + " not found");
            return;
        }
        final Product sourceProduct1 = TestUtils.readSourceProduct(inputFile1);

        final File inputFile2 = TestData.inputASAR_IMMSub;
        if (!inputFile2.exists()) {
            TestUtils.skipTest(this, inputFile2 + " not found");
            return;
        }
        final Product sourceProduct2 = TestUtils.readSourceProduct(inputFile2);

        final MosaicOp op = (MosaicOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProducts(new Product[] {sourceProduct1,sourceProduct2});

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, true, true);

    }
}
