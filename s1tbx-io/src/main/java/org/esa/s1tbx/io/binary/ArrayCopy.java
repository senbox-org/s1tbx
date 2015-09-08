package org.esa.s1tbx.io.binary;

/**
 * Created by luis on 08/02/2015.
 */
public class ArrayCopy {

    public static void copyLine(final byte[] srcLine, final byte[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public static void copyLine(final short[] srcLine, final short[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public static void copyLine(final char[] srcLine, final char[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public static void copyLine(final char[] srcLine, final short[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = (short) srcLine[i];
        }
    }

    public static void copyLine(final int[] srcLine, final int[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public static void copyLine(final long[] srcLine, final long[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public static void copyLine(final float[] srcLine, final float[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public static void copyLine(final double[] srcLine, final double[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public static void copyLine1Of2(final char[] srcLine, final char[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i << 1];
        }
    }

    public static void copyLine1Of2(final short[] srcLine, final short[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i << 1];
        }
    }

    public static void copyLine1Of2(final byte[] srcLine, final byte[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i << 1];
        }
    }

    public static void copyLine1Of2(final int[] srcLine, final int[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[i << 1];
        }
    }

    public static void copyLine1Of2(final float[] srcLine, final float[] destLine, final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; ++x, i += sourceStepX) {
            destLine[x] = (int) srcLine[i << 1];
        }
    }

    public static void copyLine2Of2(final char[] srcLine, final char[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[(i << 1) + 1];
        }
    }

    public static void copyLine2Of2(final short[] srcLine, final short[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[(i << 1) + 1];
        }
    }

    public static void copyLine2Of2(final byte[] srcLine, final byte[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[(i << 1) + 1];
        }
    }

    public static void copyLine2Of2(final int[] srcLine, final int[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = srcLine[(i << 1) + 1];
        }
    }

    public static void copyLine2Of2(final float[] srcLine, final float[] destLine, final int sourceStepX) {
        final int length = destLine.length;
        for (int x = 0, i = 0; x < length; ++x, i += sourceStepX) {
            destLine[x] = (int) srcLine[(i << 1) + 1];
        }
    }

    public static float convert16BitsTo32BitFloat(final char halfFloat) {

        // char is 16 bits

        int s = (halfFloat >> 15) & 0x00000001; // sign
        int e = (halfFloat >> 10) & 0x0000001f; // exponent
        int f =  halfFloat        & 0x000003ff; // fraction

        //System.out.println("s = " + s + " e = " + e + " f = " + f);

        // need to handle 7c00 INF and fc00 -INF?
        if (e == 0) {
            // need to handle +-0 case f==0 or f=0x8000?
            if (f == 0) // Plus or minus zero
                return Float.intBitsToFloat(s << 31);
            else { // Denormalized number -- renormalize it
                while ((f & 0x00000400)==0) {
                    f <<= 1;
                    e -=  1;
                }
                e += 1;
                f &= ~0x00000400;
            }
        } else if (e == 31) {
            if (f == 0) // Inf
                return Float.intBitsToFloat((s << 31) | 0x7f800000);
            else // NaN
                return Float.intBitsToFloat((s << 31) | 0x7f800000 | (f << 13));
        }

        e = e + (127 - 15);
        f = f << 13;

        return Float.intBitsToFloat(((s << 31) | (e << 23) | f));
    }
}
