package org.esa.beam.dataio.geotiff;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class Utils_isValidModelTransformationTest {

    @Test
    public void testNull() {
        assertEquals(false, Utils.isValidModelTransformation(null));
    }

    @Test
    public void testBadArraySize() {
        assertEquals(false, Utils.isValidModelTransformation(new double[]{3, 4}));
    }

    @Test
    public void testAllValuesAreZero() {
        assertEquals(false, Utils.isValidModelTransformation(new double[]{
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0
        }));
    }

    @Test
    public void testValidTransformationValues() {
        assertEquals(true, Utils.isValidModelTransformation(new double[]{
                1, 2, 0, 3,
                4, 5, 0, 6,
                7, 8, 0, 9,
                0, 0, 0, 0
        }));
    }
}