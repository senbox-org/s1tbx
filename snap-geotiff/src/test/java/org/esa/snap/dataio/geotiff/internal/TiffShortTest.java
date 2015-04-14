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

public class TiffShortTest extends TestCase {


    private static final int _TIFFSHORT_MAX = 0xffff;
    private static final int _TIFFSHORT_MIN = 0;

    public void testCreation_WithMaxValue() {
        new TiffShort(_TIFFSHORT_MAX);
    }

    public void testCreation_WithMinValue() {
        new TiffShort(_TIFFSHORT_MIN);
    }

    public void testCreation_ValueSmallerThanMinValue() {
        try {
            new TiffShort(_TIFFSHORT_MIN - 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {

        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testCreation_ValueBiggerThanMaxValue() {
        try {
            new TiffShort(_TIFFSHORT_MAX + 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {

        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected");
        }
    }

    public void testGetValue() {
        TiffShort tiffShort;

        tiffShort = new TiffShort(_TIFFSHORT_MAX);
        assertEquals(_TIFFSHORT_MAX, tiffShort.getValue());

        tiffShort = new TiffShort(_TIFFSHORT_MIN);
        assertEquals(_TIFFSHORT_MIN, tiffShort.getValue());

        final short value = 23456;
        tiffShort = new TiffShort(value);
        assertEquals(value, tiffShort.getValue());
    }

    public void testWriteToStream() throws IOException {
        final TiffShort tiffShort = new TiffShort(_TIFFSHORT_MAX);
        final MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());

        tiffShort.write(stream);

        assertEquals(2, stream.length());
        stream.seek(0);
        assertEquals((short) 0xffff, stream.readShort());
    }

    public void testGetSizeInBytes() {
        final TiffShort tiffShort = new TiffShort(234);
        assertEquals(2, tiffShort.getSizeInBytes());
    }
}
