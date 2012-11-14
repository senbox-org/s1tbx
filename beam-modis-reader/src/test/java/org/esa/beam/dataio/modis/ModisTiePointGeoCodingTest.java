package org.esa.beam.dataio.modis;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.junit.Assert;
import org.junit.Test;

import java.awt.Rectangle;

public class ModisTiePointGeoCodingTest {

    @Test
    public void testMustRecalculateGeoCoding() {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();

        Assert.assertFalse(ModisTiePointGeoCoding.mustRecalculateTiePointGrids(null));
        Assert.assertFalse(ModisTiePointGeoCoding.mustRecalculateTiePointGrids(subsetDef));

        subsetDef.setRegion(new Rectangle(0, 3, 4, 5));
        Assert.assertTrue(ModisTiePointGeoCoding.mustRecalculateTiePointGrids(subsetDef));
    }

    @Test
    public void testIfModisProductIsHighResolutionProduct() {
        Assert.assertTrue(ModisTiePointGeoCoding.isHighResolution(120, 60));
        Assert.assertTrue(ModisTiePointGeoCoding.isHighResolution(3072, 1536));

        Assert.assertFalse(ModisTiePointGeoCoding.isHighResolution(120, 24));
        Assert.assertFalse(ModisTiePointGeoCoding.isHighResolution(2072, 208));
    }

    @Test
    public void testCalculateStartLine() {
        Assert.assertEquals(100, ModisTiePointGeoCoding.calculateStartLine(20, new Rectangle(0, 106, 10, 10)));
        Assert.assertEquals(1210, ModisTiePointGeoCoding.calculateStartLine(10, new Rectangle(0, 1217, 10, 10)));
    }

    @Test
    public void testCalculateStopLine() {
        Assert.assertEquals(120, ModisTiePointGeoCoding.calculateStopLine(20, new Rectangle(0, 106, 10, 10)));
        Assert.assertEquals(1230, ModisTiePointGeoCoding.calculateStopLine(10, new Rectangle(0, 1217, 10, 10)));
    }
}
