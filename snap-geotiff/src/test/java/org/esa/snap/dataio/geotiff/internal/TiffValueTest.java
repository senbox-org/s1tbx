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
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TiffValueTest extends TestCase {

    private TiffValue _tiffValue;
    private ImageOutputStream _stream;

    @Override
    public void setUp() throws Exception {
        _tiffValue = createTiffValueInstance();
        _stream = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testWrite() throws IOException {
        final int[] expected = new int[]{3, 6, 128973, 8};
        _tiffValue.setData(ProductData.createInstance(expected));

        _tiffValue.write(_stream);

        assertEquals(16, _stream.length());
        _stream.seek(0);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("failure at index " + i, expected[i], _stream.readInt());
        }
    }

    public void testWrite_ProductDataThrowsIOException() {
        _tiffValue.setData(new ProductDataDummy());

        try {
            _tiffValue.write(_stream);
            fail("IOException expected");
        } catch (IOException expected) {

        } catch (Exception notExpected) {
            fail("IOException expected");
        }
    }

    public void testWrite_ThrowsIllegalStateExceptionBecauseDataWasNotSetToValue() {
        _tiffValue.setData(null);

        try {
            _tiffValue.write(_stream);
            fail("IllegalStateException expected");
        } catch (IOException notExpected) {
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {

        } catch (Exception notExpected) {
            fail("IllegalStateException expected");
        }
    }

    private TiffValue createTiffValueInstance() {
        return new TiffValue() {

        };
    }

    private class ProductDataDummy extends ProductData.Byte {

        public ProductDataDummy() {
            super(5);
        }

        @Override
        public void writeTo(final ImageOutputStream output) throws IOException {
            throw new IOException("dummy exception");
        }
    }
}
