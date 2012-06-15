package org.esa.beam.dataio.modis;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductSubsetDef;

import java.awt.*;

public class ModisTiePointGeoCodingTest extends TestCase {

    public void testMustRecalculateGeoCoding() {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();

        assertFalse(ModisTiePointGeoCoding.mustRecalculateTiePointGrids(subsetDef));

        subsetDef.setRegion(new Rectangle(0, 3, 4, 5));
        assertTrue(ModisTiePointGeoCoding.mustRecalculateTiePointGrids(subsetDef));
    }

    public void testIfModisProductIsHighResolutionProduct() {
        assertTrue(ModisTiePointGeoCoding.isHighResolution(120, 60));
        assertTrue(ModisTiePointGeoCoding.isHighResolution(3072, 1536));

        assertFalse(ModisTiePointGeoCoding.isHighResolution(120, 24));
        assertFalse(ModisTiePointGeoCoding.isHighResolution(2072, 208));
    }

    public void testCalculateStartLine() {
        assertEquals(100, ModisTiePointGeoCoding.calculateStartLine(20, new Rectangle(0, 106, 10, 10)));
        assertEquals(1210, ModisTiePointGeoCoding.calculateStartLine(10, new Rectangle(0, 1217, 10, 10)));
    }

    public void testCalculateStopLine() {
        assertEquals(120, ModisTiePointGeoCoding.calculateStopLine(20, new Rectangle(0, 106, 10, 10)));
        assertEquals(1230, ModisTiePointGeoCoding.calculateStopLine(10, new Rectangle(0, 1217, 10, 10)));
    }
}
