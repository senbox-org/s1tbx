package org.esa.beam.dataio.geotiff;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class Utils_isValidModelPixelScaleTest {

    @Test
    public void testNull() {
        assertEquals(false, Utils.isValidModelPixelScale(null));
    }

    @Test
    public void testBadArraySize() {
        assertEquals(false, Utils.isValidModelPixelScale(new double[]{3, 4}));
    }

    @Test
    public void testDefaultValuesNotNeedet() {
        assertEquals(false, Utils.isValidModelPixelScale(new double[]{1, 1, 0}));
    }

    @Test
    public void testValidModelPixelScaleValues() {
        assertEquals(true, Utils.isValidModelPixelScale(new double[]{1, 2, 0}));
    }
}