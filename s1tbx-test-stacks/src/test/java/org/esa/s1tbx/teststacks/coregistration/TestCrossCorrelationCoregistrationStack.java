package org.esa.s1tbx.teststacks.coregistration;

import org.esa.s1tbx.commons.test.ProcessorTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.insar.gpf.coregistration.CreateStackOp;
import org.esa.s1tbx.insar.gpf.coregistration.CrossCorrelationOp;
import org.esa.s1tbx.insar.gpf.coregistration.WarpOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.test.LongTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(LongTestRunner.class)
public class TestCrossCorrelationCoregistrationStack extends ProcessorTest {

    private final static File asarSantoriniFolder = new File(S1TBXTests.inputPathProperty + "/SAR/ASAR/Santorini");
    private final static File rs2ManitobaFolder = new File(S1TBXTests.inputPathProperty + "/SAR/RS2/Manitoba");

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(asarSantoriniFolder + " not found", asarSantoriniFolder.exists());
        assumeTrue(rs2ManitobaFolder + " not found", rs2ManitobaFolder.exists());
    }

    @Test
    @Ignore
    public void testStackSantorini() throws Exception {
        final List<Product> products = readProducts(asarSantoriniFolder);

        final Product trgProduct = coregister(products);

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(trgProduct, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        MetadataElement warpData = getWarpData(trgProduct, "Band_i_VV_slv3_27Oct2004");
        assertEquals("rmsMean", 1.1229252748775072E-13, warpData.getAttributeDouble("rmsMean"), 0.0001);
        assertEquals("rmsStd", 1.3035429283962273E-13, warpData.getAttributeDouble("rmsStd"), 0.0001);

        trgProduct.dispose();
        delete(tmpFolder);
    }

    @Test
    @Ignore
    public void testStackQPManitoba() throws Exception {
        final List<Product> products = readProducts(rs2ManitobaFolder);
        final List<Product> firstPair = products.subList(0, 2);

        final Product trgProduct = coregister(firstPair);

        File tmpFolder = createTmpFolder("stack1");
        ProductIO.writeProduct(trgProduct, new File(tmpFolder,"stack.dim"), "BEAM-DIMAP", true);

        MetadataElement warpData = getWarpData(trgProduct, "Band_i_VV_slv3_27Oct2004");
        assertEquals("rmsMean", 1, warpData.getAttributeDouble("rmsMean"), 0.0001);
        assertEquals("rmsStd", 1, warpData.getAttributeDouble("rmsStd"), 0.0001);

        trgProduct.dispose();
        delete(tmpFolder);
    }

    public static Product coregister(final List<Product> products) {
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
        warp.setParameter("openResidualsFile", true);

        return warp.getTargetProduct();
    }

    private static MetadataElement getWarpData(final Product product, final String bandName) {
        MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        assertNotNull(absRoot);
        MetadataElement bandMeta = absRoot.getElement(bandName);
        assertNotNull(bandMeta);
        MetadataElement warpData = bandMeta.getElement("WarpData");
        assertNotNull(bandMeta);
        return warpData;
    }
}
