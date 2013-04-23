package org.esa.beam.dataio.modis.bandreader;

import junit.framework.TestCase;
import org.esa.beam.dataio.modis.ModisConstants;

public class ModisBandReaderTest extends TestCase {

    public void testDecodeScalingMethod() {
        assertEquals(ModisBandReader.SCALE_UNKNOWN, ModisBandReader.decodeScalingMethod(null));
        assertEquals(ModisBandReader.SCALE_UNKNOWN, ModisBandReader.decodeScalingMethod(""));

        assertEquals(ModisBandReader.SCALE_LINEAR, ModisBandReader.decodeScalingMethod(ModisConstants.LINEAR_SCALE_NAME));
        assertEquals(ModisBandReader.SCALE_EXPONENTIAL, ModisBandReader.decodeScalingMethod(ModisConstants.EXPONENTIAL_SCALE_NAME));
        assertEquals(ModisBandReader.SCALE_POW_10, ModisBandReader.decodeScalingMethod(ModisConstants.POW_10_SCALE_NAME));
        assertEquals(ModisBandReader.SCALE_SLOPE_INTERCEPT, ModisBandReader.decodeScalingMethod(ModisConstants.SLOPE_INTERCEPT_SCALE_NAME));
    }
}
