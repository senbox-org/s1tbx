package org.esa.beam.dataio.envi;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.ProductData;

public class DataTypeUtilsTest extends TestCase {

    public void testMapToBEAM() {
        assertEquals(ProductData.TYPE_UINT8, DataTypeUtils.toBeam(EnviConstants.TYPE_ID_BYTE));
        assertEquals(ProductData.TYPE_INT16, DataTypeUtils.toBeam(EnviConstants.TYPE_ID_INT16));
        assertEquals(ProductData.TYPE_INT32, DataTypeUtils.toBeam(EnviConstants.TYPE_ID_INT32));
        assertEquals(ProductData.TYPE_FLOAT32, DataTypeUtils.toBeam(EnviConstants.TYPE_ID_FLOAT32));
        assertEquals(ProductData.TYPE_FLOAT64, DataTypeUtils.toBeam(EnviConstants.TYPE_ID_FLOAT64));
        assertEquals(ProductData.TYPE_UNDEFINED, DataTypeUtils.toBeam(6)); // 2 x 32 bit complex
        assertEquals(ProductData.TYPE_UNDEFINED, DataTypeUtils.toBeam(9)); // 2 x 64 bit complex
        assertEquals(ProductData.TYPE_UINT16, DataTypeUtils.toBeam(EnviConstants.TYPE_ID_UINT16));
        assertEquals(ProductData.TYPE_UINT32, DataTypeUtils.toBeam(EnviConstants.TYPE_ID_UINT32));
        assertEquals(ProductData.TYPE_UNDEFINED, DataTypeUtils.toBeam(14));    // 64 bit signed long
        assertEquals(ProductData.TYPE_UNDEFINED, DataTypeUtils.toBeam(15));    // 64 bit unsigned long

        assertEquals(ProductData.TYPE_UNDEFINED, DataTypeUtils.toBeam(-11));
    }

    public void testGetSizeInBytes() {
        assertEquals(1, DataTypeUtils.getSizeInBytes(EnviConstants.TYPE_ID_BYTE));
        assertEquals(2, DataTypeUtils.getSizeInBytes(EnviConstants.TYPE_ID_INT16));
        assertEquals(4, DataTypeUtils.getSizeInBytes(EnviConstants.TYPE_ID_INT32));
        assertEquals(4, DataTypeUtils.getSizeInBytes(EnviConstants.TYPE_ID_FLOAT32));
        assertEquals(8, DataTypeUtils.getSizeInBytes(EnviConstants.TYPE_ID_FLOAT64));
        assertEquals(2, DataTypeUtils.getSizeInBytes(EnviConstants.TYPE_ID_UINT16));
        assertEquals(4, DataTypeUtils.getSizeInBytes(EnviConstants.TYPE_ID_UINT32));
    }
}
