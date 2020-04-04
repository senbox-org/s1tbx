
package org.esa.s1tbx.utilities.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import static org.junit.Assert.*;


public class BandMergeOpTest {

    @Test
    public void testMergeOp_includeAll() {
        final Product productA = new Product("dummy1", "bandMergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "bandMergeOpTest", 10, 10);

        productA.addBand("A", ProductData.TYPE_FLOAT32);
        productB.addBand("B", ProductData.TYPE_FLOAT32);

        final BandMergeOp mergeOp = new BandMergeOp();
        mergeOp.setSourceProduct("dummy1", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        final Product mergedProduct = mergeOp.getTargetProduct();

        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("A"));
        assertTrue(mergedProduct.containsBand("B"));
        assertEquals("dummy1", mergedProduct.getName());
    }

    @Test
    public void testMergeOp_duplicateBandNames() {
        final Product productA = new Product("dummy1", "bandMergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "bandMergeOpTest", 10, 10);

        productA.addBand("A", ProductData.TYPE_FLOAT32);
        productA.addBand("Common", ProductData.TYPE_FLOAT32);
        productB.addBand("B", ProductData.TYPE_FLOAT32);
        productB.addBand("Common", ProductData.TYPE_FLOAT32);

        // add flag bands
        Band flagsA = productA.addBand("flags", ProductData.TYPE_INT8);
        FlagCoding flagCodingA = new FlagCoding("flags");
        productA.getFlagCodingGroup().add(flagCodingA);
        flagsA.setSampleCoding(flagCodingA);

        Band flagsB = productB.addBand("flags", ProductData.TYPE_INT8);
        FlagCoding flagCodingB = new FlagCoding("flags");
        productB.getFlagCodingGroup().add(flagCodingB);
        flagsB.setSampleCoding(flagCodingB);


        final BandMergeOp mergeOp = new BandMergeOp();
        mergeOp.setSourceProduct("dummy1", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        final Product mergedProduct = mergeOp.getTargetProduct();

        assertNotNull(mergedProduct);
        assertTrue(mergedProduct.containsBand("A"));
        assertTrue(mergedProduct.containsBand("B"));
        assertEquals(6, mergedProduct.getNumBands());
        assertTrue(mergedProduct.containsBand("Common"));
        assertTrue(mergedProduct.containsBand("Common_2"));
        assertTrue(mergedProduct.containsBand("flags"));
        assertTrue(mergedProduct.containsBand("flags_2"));

        assertEquals("dummy1", mergedProduct.getName());
    }

    @Test
    public void testValidateSourceProducts_Failing() {
        final BandMergeOp mergeOp = new BandMergeOp();

        final Product productA = new Product("dummy1", "bandMergeOpTest", 10, 10);
        final Product productB = new Product("dummy2", "bandMergeOpTest", 11, 11);

        mergeOp.setSourceProduct("dummy1", productA);
        mergeOp.setSourceProduct("dummy2", productB);
        try {
            mergeOp.getTargetProduct();
            fail();
        } catch (OperatorException e) {
            final String expectedErrorMessage = "Product .* is not compatible to master product";
            assertTrue("expected: '" + expectedErrorMessage + "', actual: '" + e.getMessage() + "'",
                       e.getMessage().replace(".", "").matches(expectedErrorMessage));
        }
    }
}
