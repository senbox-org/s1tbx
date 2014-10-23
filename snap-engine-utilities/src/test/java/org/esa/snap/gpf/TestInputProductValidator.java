package org.esa.snap.gpf;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Validates input products using commonly used verifications
 */
public class TestInputProductValidator {

    @Test
    public void TestNotSentinel1Product() throws IOException {
        final Product sourceProduct = TestUtils.readSourceProduct(TestData.inputASAR_WSM);
        final InputProductValidator validator = new InputProductValidator(sourceProduct);

        try {
            validator.checkIfSentinel1Product();
        } catch (OperatorException e) {
            assertEquals(e.getMessage(), "Input should be a Sentinel-1 product.");
        }
        try {
            validator.checkProductType(new String[]{"GRD"});
        } catch (OperatorException e) {
            assertTrue(e.getMessage().contains("is not a valid product type"));
        }
        try {
            validator.checkIfMapProjected();
        } catch (OperatorException e) {
            assertEquals(e.getMessage(), "Source product should not be map projected");
        }
        try {
            validator.checkIfTOPSARBurstProduct(true);
        } catch (OperatorException e) {
            assertEquals(e.getMessage(), "Source product should be an SLC burst product");
        }
        try {
            validator.checkAcquisitionMode(new String[]{"IW","EW"});
        } catch (OperatorException e) {
            assertTrue(e.getMessage().contains("is not a valid product type"));
        }
    }

    @Test
    public void TestSentinel1GRDProduct() throws IOException {
        final Product sourceProduct = TestUtils.readSourceProduct(TestData.inputS1_GRD);
        final InputProductValidator validator = new InputProductValidator(sourceProduct);

        validator.checkIfSentinel1Product();
        validator.checkProductType(new String[] {"GRD"});
        validator.checkIfTOPSARBurstProduct(false);
        validator.checkAcquisitionMode(new String[]{"SM"});
    }

    @Test
    public void TestSentinel1SLCProduct() throws IOException {
        final Product sourceProduct = TestUtils.readSourceProduct(TestData.inputS1_StripmapSLC);
        final InputProductValidator validator = new InputProductValidator(sourceProduct);

        validator.checkIfSentinel1Product();
        validator.checkProductType(new String[] {"SLC"});
        validator.checkIfTOPSARBurstProduct(false);
        validator.checkAcquisitionMode(new String[]{"SM"});
    }
}


