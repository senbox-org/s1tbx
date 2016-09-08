/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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

    public static float toFloat(final short s) {

        // FAB16 FLOATING-POINT ARITHMETIC FORMAT
        //
        // Based on c code...
        //
        // #define FLT16_ZERO     0
        //
        // typedef unsigned short Real2;
        // typedef float Real4;
        //
        // Real4 f32r_(Real2 *a)
        // {
        //    register unsigned int i32a;
        //    if ( !(i32a = (unsigned int)*a) )
        //    return (Real4)FLT16_ZERO;
        //    register unsigned int e_o = ((i32a & 0x00007800) + 0x0001D000) << 13;
        //    unsigned int f32r = ((i32a & 0x00008000) << 16) | e_o | (i32a & 0x000007FF) << 13;
        //    return *(Real4*)&f32r;
        // }

        if (s == 0) return 0;

        final int i32a = s & 0xffff; // unsigned short

        final int e_o = ((i32a & 0x00007800) + 0x0001D000) << 13;

        final int a1 = ((i32a & 0x00008000) << 16);
        final int a2 = (i32a & 0x000007FF) << 13;

        final int f32r = a1 | e_o | a2;

        final float f = Float.intBitsToFloat(f32r);

        return f;
    }
}
