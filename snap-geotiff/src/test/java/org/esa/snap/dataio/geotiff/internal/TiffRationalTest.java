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

package org.esa.snap.dataio.geotiff.internal;


import junit.framework.TestCase;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TiffRationalTest extends TestCase {

    private static final long _TIFFRATIONAL_MAX = 0xffffffffL;
    private static final long _TIFFRATIONAL_MIN = 1;

    public void testCreation() {
        new TiffRational(_TIFFRATIONAL_MAX, _TIFFRATIONAL_MAX);
        new TiffRational(_TIFFRATIONAL_MAX, _TIFFRATIONAL_MIN);
        new TiffRational(_TIFFRATIONAL_MIN, _TIFFRATIONAL_MIN);
        new TiffRational(_TIFFRATIONAL_MIN, _TIFFRATIONAL_MAX);
    }

    public void testCreation_WithIllegalValueRanges() {
        final int legalValue = 22689356;
        final long[][] values = new long[][]{
                new long[]{_TIFFRATIONAL_MAX + 1, legalValue}, // index 0
                new long[]{legalValue, _TIFFRATIONAL_MAX + 1}, // index 1
                new long[]{_TIFFRATIONAL_MAX + 1, _TIFFRATIONAL_MAX + 1}, // index 2
                new long[]{_TIFFRATIONAL_MIN - 1, legalValue}, // index 3
                new long[]{legalValue, _TIFFRATIONAL_MIN - 1}, // index 4
                new long[]{_TIFFRATIONAL_MIN - 1, _TIFFRATIONAL_MIN - 1}, // index 5
        };
        for (int i = 0; i < values.length; i++) {
            final long[] longs = values[i];
            try {
                new TiffRational(longs[0], longs[1]);
                fail("IllegalArgumentException expected at index " + i);
            } catch (IllegalArgumentException expected) {

            } catch (Exception notExpected) {
                fail("IllegalArgumentException expected at index " + i);
            }
        }
    }

    public void testGetNumeratorAndGetDenominator() {
        final int legalValue = 22689356;
        final long[][] values = new long[][]{
                new long[]{_TIFFRATIONAL_MAX, _TIFFRATIONAL_MAX},
                new long[]{_TIFFRATIONAL_MAX, _TIFFRATIONAL_MIN},
                new long[]{_TIFFRATIONAL_MIN, _TIFFRATIONAL_MIN},
                new long[]{_TIFFRATIONAL_MIN, _TIFFRATIONAL_MAX},
                new long[]{legalValue, legalValue},
        };
        for (int i = 0; i < values.length; i++) {
            final long[] longs = values[i];
            final TiffRational tiffRational = new TiffRational(longs[0], longs[1]);
            assertEquals("failure at index " + i, longs[0], tiffRational.getNumerator());
            assertEquals("failure at index " + i, longs[1], tiffRational.getDenominator());
        }
    }

    public void testWriteToStream() throws IOException {
        final long numerator = _TIFFRATIONAL_MAX;
        final long denominator = _TIFFRATIONAL_MAX - 3;
        final TiffRational tiffRational = new TiffRational(numerator, denominator);
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());

        tiffRational.write(stream);

        assertEquals(8, stream.length());
        stream.seek(0);
        assertEquals((int) numerator, stream.readInt());
        assertEquals((int) denominator, stream.readInt());
    }

    public void testGetSizeInBytes() {
        final TiffRational tiffRational = new TiffRational(234, 23478);
        assertEquals(8, tiffRational.getSizeInBytes());
    }
}
