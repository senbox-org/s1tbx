package org.esa.s1tbx.teststacks.productgroups;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.insar.gpf.InterferogramOp;
import org.esa.s1tbx.teststacks.coregistration.TestCrossCorrelationCoregistrationStack;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.test.LongTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assume.assumeTrue;

@RunWith(LongTestRunner.class)
public class TestPGInterferogram extends ProcessorTest {

    private final static File asarSantoriniFolder = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Santorini");

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(asarSantoriniFolder + " not found", asarSantoriniFolder.exists());
    }

    @Override
    protected File createTmpFolder(final String folderName) {
        File folder = new File("c:\\tmp\\" + folderName);
        folder.mkdirs();
        return folder;
    }

    @Test
    public void testStack1() throws Exception {
        File tmpFolder = createTmpFolder("stack1");
        final List<Product> products = readProducts(asarSantoriniFolder);
        final List<Product> firstPair = products.subList(0, 2);

        File trgFolder = new File(tmpFolder,"stackPG");
        coregisterInterferogram(firstPair, trgFolder);

        final List<Product> firstThree = products.subList(0, 3);
        trgFolder = new File(tmpFolder,"stackPG");
        coregisterInterferogram(firstThree, trgFolder);

        //delete(tmpFolder);
    }

    private void coregisterInterferogram(final List<Product> srcProducts, final File trgFolder) throws IOException {
        Product coregisteredStack = TestCrossCorrelationCoregistrationStack.coregister(srcProducts);

        InterferogramOp interferogram = new InterferogramOp();
        interferogram.setSourceProduct(coregisteredStack);

        Product trgProduct = interferogram.getTargetProduct();


        ProductIO.writeProduct(trgProduct, trgFolder, "ProductGroup", true);

        trgProduct.dispose();
    }

}
