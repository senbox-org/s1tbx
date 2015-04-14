package org.esa.snap.dataio.bigtiff.internal;


import org.junit.Test;

import static org.junit.Assert.*;

public class TiffUIntTest {

    @Test
    public void testConstructAndGetData() {
        final TiffUInt tiffUInt = new TiffUInt(45226765l);

        assertEquals(45226765l, tiffUInt.getValue());
    }

    @Test
    public void testConstruct_ValueOutOfRange() {
        final long UNSIGNED_INT_MAX = 0xffffffffL;

        try {
            new TiffUInt(UNSIGNED_INT_MAX + 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
