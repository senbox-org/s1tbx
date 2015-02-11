package org.esa.beam.dataio.bigtiff.internal;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TiffTypeTest {

    @Test
    public void testGetType_nullInput() {
        try {
            TiffType.getType(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetType_zeroLengthInput() {
        try {
            TiffType.getType(new TiffValue[0]);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetType_elementsAreNull() {
        try {
            TiffType.getType(new TiffValue[2]);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testGetType() {
        final TiffValue[] array = new TiffValue[1];

        array[0] = new TiffShort(1);
        TiffShort type = TiffType.getType(array);
        assertEquals(3, type.getValue());

        array[0] = new TiffUInt(2);
        type = TiffType.getType(array);
        assertEquals(4, type.getValue());

        array[0] = new TiffRational(3, 4);
        type = TiffType.getType(array);
        assertEquals(5, type.getValue());

        array[0] = new TiffAscii("bla");
        type = TiffType.getType(array);
        assertEquals(2, type.getValue());

        array[0] = new GeoTiffAscii("blubb");
        type = TiffType.getType(array);
        assertEquals(2, type.getValue());

        array[0] = new TiffDouble(5.6);
        type = TiffType.getType(array);
        assertEquals(12, type.getValue());

        array[0] = new TiffLong(7);
        type = TiffType.getType(array);
        assertEquals(17, type.getValue());
    }
}
