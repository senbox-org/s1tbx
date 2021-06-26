package org.esa.s1tbx.teststacks.coregistration;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.insar.gpf.coregistration.CreateStackOp;
import org.esa.s1tbx.insar.gpf.coregistration.CrossCorrelationOp;
import org.esa.s1tbx.insar.gpf.coregistration.WarpOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.test.LongTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assume.assumeTrue;

@RunWith(LongTestRunner.class)
public class TestCrossCorrelationCoregistrationStack extends ProcessorTest {

    private final static File asarSantoriniFolder = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Santorini");

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(asarSantoriniFolder + " not found", asarSantoriniFolder.exists());
    }

    @Test
    public void testStack1() throws Exception {
        final Product[] products = readProducts(asarSantoriniFolder);

        Product trgProduct = coregister(products);

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(trgProduct, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        trgProduct.dispose();
        delete(tmpFolder);
    }

    public static Product coregister(final Product[] products) {
        CreateStackOp createStack = new CreateStackOp();
        int cnt = 0;
        for(Product product : products) {
            createStack.setSourceProduct("input"+cnt, product);
            ++cnt;
        }

        CrossCorrelationOp crossCorrelation = new CrossCorrelationOp();
        crossCorrelation.setSourceProduct(createStack.getTargetProduct());

        WarpOp warp = new WarpOp();
        warp.setSourceProduct(crossCorrelation.getTargetProduct());

        return warp.getTargetProduct();
    }
}
