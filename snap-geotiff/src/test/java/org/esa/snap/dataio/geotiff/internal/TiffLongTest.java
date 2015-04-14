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

public class TiffLongTest extends TestCase {

    private final long _TIFFLONG_MAX = 0xffffffffL;
    private final long _TIFFLONG_MIN = 0;

    public void testCreation_WithMaxValue() {
        new TiffLong(_TIFFLONG_MAX);
    }

    public void testCreation_WithMinValue() {
        new TiffLong(_TIFFLONG_MIN);
    }

    public void testCreation_ValueSmallerThanMinValue() {
        try {
            new TiffLong(_TIFFLONG_MIN - 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {

        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testCreation_ValueBiggerThanMaxValue() {
        try {
            new TiffLong(_TIFFLONG_MAX + 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {

        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testGetValue() {
        TiffLong tiffLong;

        tiffLong = new TiffLong(_TIFFLONG_MAX);
        assertEquals(_TIFFLONG_MAX, tiffLong.getValue());

        tiffLong = new TiffLong(_TIFFLONG_MIN);
        assertEquals(_TIFFLONG_MIN, tiffLong.getValue());

        final int value = 23498756;
        tiffLong = new TiffLong(value);
        assertEquals(value, tiffLong.getValue());
    }

    public void testWriteToStream() throws IOException {
        final TiffLong tiffLong = new TiffLong(_TIFFLONG_MAX);
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());

        tiffLong.write(stream);

        assertEquals(4, stream.length());
        stream.seek(0);
        assertEquals(0xffffffff, stream.readInt());
    }

    public void testGetSizeInBytes() {
        final TiffLong tiffLong = new TiffLong(234);
        assertEquals(4, tiffLong.getSizeInBytes());
    }
}
