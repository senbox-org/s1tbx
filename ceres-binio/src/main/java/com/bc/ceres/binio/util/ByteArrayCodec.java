/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.binio.util;

import java.nio.ByteOrder;

/**
 * A utility class used to decode/encode primitive Java types from/to a byte buffer using
 * a specified {@link ByteOrder}.
 */
public abstract class ByteArrayCodec {
    public final static ByteArrayCodec LITTLE_ENDIAN = new LE();
    public final static ByteArrayCodec BIG_ENDIAN = new BE();

    public static ByteArrayCodec getInstance(ByteOrder byteOrder) {
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            return LITTLE_ENDIAN;
        } else {
            return BIG_ENDIAN;
        }
    }

    protected ByteArrayCodec() {
    }

    public abstract ByteOrder getByteOrder();

    /////////////////////////////////////////////////////////////////////////
    // Getter

    public final byte getByte(byte[] b, int boff) {
        return b[boff];
    }

    public abstract short getShort(byte[] b, int boff);

    public abstract int getInt(byte[] b, int boff);

    public abstract long getLong(byte[] b, int boff);

    public abstract float getFloat(byte[] b, int boff);

    public abstract double getDouble(byte[] b, int boff);

    public final void getBytes(byte[] b, int boff, byte[] v, int voff, int vlen) {
        System.arraycopy(b, boff, v, voff, vlen);
    }

    public abstract void getShorts(byte[] b, int boff, short[] v, int voff, int vlen);

    public abstract void getInts(byte[] b, int boff, int[] v, int voff, int vlen);

    public abstract void getLongs(byte[] b, int boff, long[] v, int voff, int vlen);

    public abstract void getFloats(byte[] b, int boff, float[] v, int voff, int vlen);

    public abstract void getDoubles(byte[] b, int boff, double[] v, int voff, int vlen);

    /////////////////////////////////////////////////////////////////////////
    // Setter

    public final void setByte(byte[] b, int boff, byte v) {
        b[boff] = v;
    }

    public abstract void setShort(byte[] b, int boff, short v);

    public abstract void setInt(byte[] b, int boff, int v);

    public abstract void setLong(byte[] b, int boff, long v);

    public abstract void setFloat(byte[] b, int boff, float v);

    public abstract void setDouble(byte[] b, int boff, double v);

    public final void setBytes(byte[] b, int boff, byte[] v, int voff, int vlen) {
        System.arraycopy(v, voff, b, boff, vlen);
    }

    public abstract void setShorts(byte[] b, int boff, short[] v, int voff, int vlen);

    public abstract void setInts(byte[] b, int boff, int[] v, int voff, int vlen);

    public abstract void setLongs(byte[] b, int boff, long[] v, int voff, int vlen);

    public abstract void setFloats(byte[] b, int boff, float[] v, int voff, int vlen);

    public abstract void setDoubles(byte[] b, int boff, double[] v, int voff, int vlen);

    private static short decodeSLE(byte[] b, int boff) {
        int b0 = b[boff + 1];
        int b1 = b[boff + 0] & 0xff;
        return (short) ((b0 << 8) | b1);
    }

    private static short decodeSBE(byte[] b, int boff) {
        int b0 = b[boff + 0];
        int b1 = b[boff + 1] & 0xff;
        return (short) ((b0 << 8) | b1);
    }

    private static int decodeILE(byte[] b, int boff) {
        int b0 = b[boff + 3];
        int b1 = b[boff + 2] & 0xff;
        int b2 = b[boff + 1] & 0xff;
        int b3 = b[boff + 0] & 0xff;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    private static int decodeIBE(byte[] b, int boff) {
        int b0 = b[boff + 0];
        int b1 = b[boff + 1] & 0xff;
        int b2 = b[boff + 2] & 0xff;
        int b3 = b[boff + 3] & 0xff;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    private static long decodeLLE(byte[] b, int boff) {
        int b0 = b[boff + 7];
        int b1 = b[boff + 6] & 0xff;
        int b2 = b[boff + 5] & 0xff;
        int b3 = b[boff + 4] & 0xff;
        int b4 = b[boff + 3];
        int b5 = b[boff + 2] & 0xff;
        int b6 = b[boff + 1] & 0xff;
        int b7 = b[boff + 0] & 0xff;

        int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;

        return ((long) i0 << 32) | (i1 & 0xffffffffL);
    }

    private static long decodeLBE(byte[] b, int boff) {
        int b0 = b[boff + 0];
        int b1 = b[boff + 1] & 0xff;
        int b2 = b[boff + 2] & 0xff;
        int b3 = b[boff + 3] & 0xff;
        int b4 = b[boff + 4];
        int b5 = b[boff + 5] & 0xff;
        int b6 = b[boff + 6] & 0xff;
        int b7 = b[boff + 7] & 0xff;

        int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;

        return ((long) i0 << 32) | (i1 & 0xffffffffL);
    }

    private static float decodeFLE(byte[] b, int boff) {
        int b0 = b[boff + 3];
        int b1 = b[boff + 2] & 0xff;
        int b2 = b[boff + 1] & 0xff;
        int b3 = b[boff + 0] & 0xff;
        int i = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        return Float.intBitsToFloat(i);
    }

    private static float decodeFBE(byte[] b, int boff) {
        int b0 = b[boff + 0];
        int b1 = b[boff + 1] & 0xff;
        int b2 = b[boff + 2] & 0xff;
        int b3 = b[boff + 3] & 0xff;
        int i = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        return Float.intBitsToFloat(i);
    }

    private static double decodeDL(byte[] b, int boff) {
        int b0 = b[boff + 7];
        int b1 = b[boff + 6] & 0xff;
        int b2 = b[boff + 5] & 0xff;
        int b3 = b[boff + 4] & 0xff;
        int b4 = b[boff + 3];
        int b5 = b[boff + 2] & 0xff;
        int b6 = b[boff + 1] & 0xff;
        int b7 = b[boff + 0] & 0xff;

        int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
        long l = ((long) i0 << 32) | (i1 & 0xffffffffL);

        return Double.longBitsToDouble(l);
    }

    private static double decodeDBE(byte[] b, int boff) {
        int b0 = b[boff + 0];
        int b1 = b[boff + 1] & 0xff;
        int b2 = b[boff + 2] & 0xff;
        int b3 = b[boff + 3] & 0xff;
        int b4 = b[boff + 4];
        int b5 = b[boff + 5] & 0xff;
        int b6 = b[boff + 6] & 0xff;
        int b7 = b[boff + 7] & 0xff;

        int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
        long l = ((long) i0 << 32) | (i1 & 0xffffffffL);

        return Double.longBitsToDouble(l);
    }

    private static void encodeSLE(byte[] b, int boff, short v) {
        b[boff + 0] = (byte) (v >>> 0);
        b[boff + 1] = (byte) (v >>> 8);
    }

    private static void encodeSBE(byte[] b, int boff, short v) {
        b[boff + 0] = (byte) (v >>> 8);
        b[boff + 1] = (byte) (v >>> 0);
    }

    private static void encodeILE(byte[] b, int boff, int v) {
        b[boff + 0] = (byte) (v >>> 0);
        b[boff + 1] = (byte) (v >>> 8);
        b[boff + 2] = (byte) (v >>> 16);
        b[boff + 3] = (byte) (v >>> 24);
    }

    private static void encodeIBE(byte[] b, int boff, int v) {
        b[boff + 0] = (byte) (v >>> 24);
        b[boff + 1] = (byte) (v >>> 16);
        b[boff + 2] = (byte) (v >>> 8);
        b[boff + 3] = (byte) (v >>> 0);
    }

    private static void encodeLLE(byte[] b, int boff, long v) {
        b[boff + 0] = (byte) (v >>> 0);
        b[boff + 1] = (byte) (v >>> 8);
        b[boff + 2] = (byte) (v >>> 16);
        b[boff + 3] = (byte) (v >>> 24);
        b[boff + 4] = (byte) (v >>> 32);
        b[boff + 5] = (byte) (v >>> 40);
        b[boff + 6] = (byte) (v >>> 48);
        b[boff + 7] = (byte) (v >>> 56);
    }

    private static void encodeLBE(byte[] b, int boff, long v) {
        b[boff + 0] = (byte) (v >>> 56);
        b[boff + 1] = (byte) (v >>> 48);
        b[boff + 2] = (byte) (v >>> 40);
        b[boff + 3] = (byte) (v >>> 32);
        b[boff + 4] = (byte) (v >>> 24);
        b[boff + 5] = (byte) (v >>> 16);
        b[boff + 6] = (byte) (v >>> 8);
        b[boff + 7] = (byte) (v >>> 0);
    }

    private static void encodeFLE(byte[] b, int boff, float v) {
        encodeILE(b, boff, Float.floatToIntBits(v));
    }

    private static void encodeFBE(byte[] b, int boff, float v) {
        encodeIBE(b, boff, Float.floatToIntBits(v));
    }

    private static void encodeDLE(byte[] b, int boff, double v) {
        encodeLLE(b, boff, Double.doubleToLongBits(v));
    }

    private static void encodeDBE(byte[] b, int boff, double v) {
        encodeLBE(b, boff, Double.doubleToLongBits(v));
    }

    private final static class LE extends ByteArrayCodec {

        @Override
        public ByteOrder getByteOrder() {
            return ByteOrder.LITTLE_ENDIAN;
        }

        @Override
        public short getShort(byte[] b, int boff) {
            return decodeSLE(b, boff);
        }

        @Override
        public int getInt(byte[] b, int boff) {
            return decodeILE(b, boff);
        }

        @Override
        public long getLong(byte[] b, int boff) {
            return decodeLLE(b, boff);
        }

        @Override
        public float getFloat(byte[] b, int boff) {
            return decodeFLE(b, boff);
        }

        @Override
        public double getDouble(byte[] b, int boff) {
            return decodeDL(b, boff);
        }

        @Override
        public void getShorts(byte[] b, int boff, short[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeSLE(b, boff);
                boff += 2;
            }
        }

        @Override
        public void getInts(byte[] b, int boff, int[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeILE(b, boff);
                boff += 4;
            }
        }

        @Override
        public void getLongs(byte[] b, int boff, long[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeLLE(b, boff);
                boff += 8;
            }
        }

        @Override
        public void getFloats(byte[] b, int boff, float[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeFLE(b, boff);
                boff += 4;
            }
        }

        @Override
        public void getDoubles(byte[] b, int boff, double[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeDL(b, boff);
                boff += 8;
            }
        }

        @Override
        public void setShort(byte[] b, int boff, short v) {
            encodeSLE(b, boff, v);
        }

        @Override
        public void setInt(byte[] b, int boff, int v) {
            encodeILE(b, boff, v);
        }

        @Override
        public void setLong(byte[] b, int boff, long v) {
            encodeLLE(b, boff, v);
        }

        @Override
        public void setFloat(byte[] b, int boff, float v) {
            encodeFLE(b, boff, v);
        }

        @Override
        public void setDouble(byte[] b, int boff, double v) {
            encodeDLE(b, boff, v);
        }

        @Override
        public void setShorts(byte[] b, int boff, short[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeSLE(b, boff, v[voff + i]);
                boff += 2;
            }
        }

        @Override
        public void setInts(byte[] b, int boff, int[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeILE(b, boff, v[voff + i]);
                boff += 4;
            }
        }

        @Override
        public void setLongs(byte[] b, int boff, long[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeLLE(b, boff, v[voff + i]);
                boff += 8;
            }
        }

        @Override
        public void setFloats(byte[] b, int boff, float[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeFLE(b, boff, v[voff + i]);
                boff += 4;
            }
        }

        @Override
        public void setDoubles(byte[] b, int boff, double[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeDLE(b, boff, v[voff + i]);
                boff += 8;
            }
        }
    }

    private final static class BE extends ByteArrayCodec {

        @Override
        public ByteOrder getByteOrder() {
            return ByteOrder.BIG_ENDIAN;
        }

        @Override
        public short getShort(byte[] b, int boff) {
            return decodeSBE(b, boff);
        }

        @Override
        public int getInt(byte[] b, int boff) {
            return decodeIBE(b, boff);
        }

        @Override
        public long getLong(byte[] b, int boff) {
            return decodeLBE(b, boff);
        }

        @Override
        public float getFloat(byte[] b, int boff) {
            return decodeFBE(b, boff);
        }

        @Override
        public double getDouble(byte[] b, int boff) {
            return decodeDBE(b, boff);
        }

        @Override
        public void getShorts(byte[] b, int boff, short[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeSBE(b, boff);
                boff += 2;
            }
        }

        @Override
        public void getInts(byte[] b, int boff, int[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeIBE(b, boff);
                boff += 4;
            }
        }

        @Override
        public void getLongs(byte[] b, int boff, long[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeLBE(b, boff);
                boff += 8;
            }
        }

        @Override
        public void getFloats(byte[] b, int boff, float[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeFBE(b, boff);
                boff += 4;
            }
        }

        @Override
        public void getDoubles(byte[] b, int boff, double[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                v[voff + i] = decodeDBE(b, boff);
                boff += 8;
            }
        }

        @Override
        public void setShort(byte[] b, int boff, short v) {
            encodeSBE(b, boff, v);
        }

        @Override
        public void setInt(byte[] b, int boff, int v) {
            encodeIBE(b, boff, v);
        }

        @Override
        public void setLong(byte[] b, int boff, long v) {
            encodeLBE(b, boff, v);
        }

        @Override
        public void setFloat(byte[] b, int boff, float v) {
            encodeFBE(b, boff, v);
        }

        @Override
        public void setDouble(byte[] b, int boff, double v) {
            encodeDBE(b, boff, v);
        }

        @Override
        public void setShorts(byte[] b, int boff, short[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeSBE(b, boff, v[voff + i]);
                boff += 2;
            }
        }

        @Override
        public void setInts(byte[] b, int boff, int[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeIBE(b, boff, v[voff + i]);
                boff += 4;
            }
        }

        @Override
        public void setLongs(byte[] b, int boff, long[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeLBE(b, boff, v[voff + i]);
                boff += 8;
            }
        }

        @Override
        public void setFloats(byte[] b, int boff, float[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeFBE(b, boff, v[voff + i]);
                boff += 4;
            }
        }

        @Override
        public void setDoubles(byte[] b, int boff, double[] v, int voff, int vlen) {
            for (int i = 0; i < vlen; i++) {
                encodeDBE(b, boff, v[voff + i]);
                boff += 8;
            }
        }
    }
}