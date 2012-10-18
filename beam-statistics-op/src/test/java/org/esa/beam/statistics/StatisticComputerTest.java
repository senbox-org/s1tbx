package org.esa.beam.statistics;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Test;

import static org.junit.Assert.*;

public class StatisticComputerTest {

    @Test
    public void testThatGetBandFailsWhenExpressionBandNameIsAlreadyContainedInProduct() {
        final BandConfiguration bandConfiguration = new BandConfiguration();
        bandConfiguration.expression = "contained";
        final Product product = new Product("productName", "productType", 5, 5);
        product.addBand(new Band("contained", ProductData.TYPE_INT8, 5, 5));
        try {
            StatisticComputer.getBand(bandConfiguration, product);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof OperatorException);
            assertEquals(e.getMessage(), "Band 'contained' already exists in product 'productName'.");
        }
    }
} 