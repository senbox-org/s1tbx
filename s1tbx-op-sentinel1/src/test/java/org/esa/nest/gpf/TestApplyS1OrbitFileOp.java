package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Test ApplyS1OrbitFileOp
 */
public class TestApplyS1OrbitFileOp {

    private final static OperatorSpi spi = new ApplyS1OrbitFileOp.Spi();

    private final static File orbitFileFolder = new File(TestData.inputSAR+"Orbits");

    @Test
    public void testSentinelPODOrbitFileOperations() throws Exception {
        if (!orbitFileFolder.exists()) {
            TestUtils.skipTest(this, orbitFileFolder + " not found");
            return;
        }

        final Product sourceProduct = TestUtils.readSourceProduct(TestData.inputS1_GRD);

        final ApplyS1OrbitFileOp op = (ApplyS1OrbitFileOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setOrbitFileFolder(orbitFileFolder);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);
    }
}
