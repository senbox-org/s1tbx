package com.bc.ceres.binio.util;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class NumberUtilsTest {

    @Test
    public void unsignedLongMaskBits() {
        final byte[] bytes = NumberUtils.ULONG_MASK.toByteArray();
        // 8 bytes of data plus one byte for the sign
        assertEquals(9, bytes.length);

        // positive sign
        assertEquals(0x00, bytes[0]);

        assertEquals(0xff, bytes[1] & 0xff);
        assertEquals(0xff, bytes[2] & 0xff);
        assertEquals(0xff, bytes[3] & 0xff);
        assertEquals(0xff, bytes[4] & 0xff);
        assertEquals(0xff, bytes[5] & 0xff);
        assertEquals(0xff, bytes[6] & 0xff);
        assertEquals(0xff, bytes[7] & 0xff);
        assertEquals(0xff, bytes[8] & 0xff);
    }

    @Test
    public void bigIntegerFromUnsignedLongValue() {
        final BigInteger unsignedLong = BigInteger.valueOf(0x8000000000007001L).and(NumberUtils.ULONG_MASK);

        final byte[] bytes = unsignedLong.toByteArray();
        // 8 bytes of data plus one byte for the sign
        assertEquals(9, bytes.length);

        // positive sign
        assertEquals(0x00, bytes[0]);

        assertEquals(0x80, bytes[1] & 0xff);
        assertEquals(0x00, bytes[2] & 0xff);
        assertEquals(0x00, bytes[3] & 0xff);
        assertEquals(0x00, bytes[4] & 0xff);
        assertEquals(0x00, bytes[5] & 0xff);
        assertEquals(0x00, bytes[6] & 0xff);
        assertEquals(0x70, bytes[7] & 0xff);
        assertEquals(0x01, bytes[8] & 0xff);
    }
}
