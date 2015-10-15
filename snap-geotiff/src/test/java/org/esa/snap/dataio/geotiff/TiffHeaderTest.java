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

package org.esa.snap.dataio.geotiff;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.geotiff.internal.TiffHeader;
import org.esa.snap.dataio.geotiff.internal.TiffIFD;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * TiffHeader Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>02/11/2005</pre>
 */

public class TiffHeaderTest extends TestCase {

    private TiffHeader _tiffHeader;

    @Override
    public void setUp() throws Exception {
        final Product product = new Product("name", "type", 10, 15);
        product.addBand("b1", ProductData.TYPE_UINT16);
        _tiffHeader = new TiffHeader(new Product[]{product});
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testCreation_WithNull() {
        try {
            new TiffHeader(null);
            fail("IllegalArgumentException expected because a null parameter is given");
        } catch (IllegalArgumentException expected) {
            // expected Exception
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected: but was [" + notExpected.getClass().getName() + "]");
        }
    }

    public void testCreation_WithZeroSizedArray() {
        try {
            new TiffHeader(new Product[0]);
            fail("IllegalArgumentException expected because the parameter is a zero sized array");
        } catch (IllegalArgumentException expected) {
            // expected Exception
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected: but was [" + notExpected.getClass().getName() + "]");
        }
    }

    public void testCreation() {
        final Product product = new Product("name", "type", 10, 15);
        product.addBand("b1", ProductData.TYPE_UINT16);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{product});
        assertNotNull(tiffHeader.getIfdAt(0));
    }

    public void testGetIfdAt() {
        assertNotNull(_tiffHeader.getIfdAt(0));

        try {
            _tiffHeader.getIfdAt(-1);
            fail("IllegalArgumentException expected because index is less than zero");
        } catch (IllegalArgumentException expected) {
            // expected Exception
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected: but was [" + notExpected.getClass().getName() + "]");
        }

        try {
            _tiffHeader.getIfdAt(1);
            fail("IllegalArgumentException expected because index ist greater than number of ifd's");
        } catch (IllegalArgumentException expected) {
            // expected Exception
        } catch (Exception notExpected) {
            fail("IllegalArgumentException expected: but was [" + notExpected.getClass().getName() + "]");
        }
    }

    public void testWrite_withOneProduct() throws Exception {
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
        _tiffHeader.write(ios);

        final long ifdSize = _tiffHeader.getIfdAt(0).getRequiredIfdSize();
        final long valuesSize = _tiffHeader.getIfdAt(0).getRequiredReferencedValuesSize();
        final long startIfd = TiffHeader.FIRST_IFD_OFFSET.getValue();
        final long expectedStreamLength = startIfd + ifdSize + valuesSize;
        final long expectedNextIFDOffset = 0;
        assertEquals(expectedStreamLength, ios.length());
        ios.seek(startIfd + ifdSize - 4);
        assertEquals(expectedNextIFDOffset, ios.readInt());
    }

    public void testWrite_withTwoProducts() throws Exception {
        final Product p1 = new Product("p1", "type", 10, 12);
        final Product p2 = new Product("p2", "type", 8, 6);
        p1.addBand("b1", ProductData.TYPE_INT16);
        p2.addBand("b1", ProductData.TYPE_UINT16);
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{p1, p2});

        final TiffIFD ifd1 = tiffHeader.getIfdAt(0);
        final TiffIFD ifd2 = tiffHeader.getIfdAt(1);

        assertNotNull(ifd1);
        assertNotNull(ifd2);

        final long startFirstIfd = TiffHeader.FIRST_IFD_OFFSET.getValue();
        final long firstIfdEntireSize = ifd1.getRequiredEntireSize();
        final long secondIfdSize = ifd2.getRequiredIfdSize();
        final long secondIfdValuesSize = ifd2.getRequiredReferencedValuesSize();
        final long expectedStreamLength = startFirstIfd + firstIfdEntireSize + secondIfdSize + secondIfdValuesSize;

        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
        tiffHeader.write(ios);
        assertEquals(expectedStreamLength, ios.length());
    }

    public void testSetBigEndianOrder_littleEndian() throws Exception {
        _tiffHeader.setBigEndianOrder(false);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(stream);
        _tiffHeader.write(ios);

        ios.close();
        final byte[] bytes = stream.toByteArray();
        assertEquals(0x49, bytes[0]);
        assertEquals(0x49, bytes[1]);
    }

    public void testSetBigEndianOrder_bigEndian() throws Exception {
        _tiffHeader.setBigEndianOrder(true);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(stream);
        _tiffHeader.write(ios);

        ios.close();
        final byte[] bytes = stream.toByteArray();
        assertEquals(0x4D, bytes[0]);
        assertEquals(0x4D, bytes[1]);
    }

    public void testMagicNumber_littleEndian() throws Exception {
        _tiffHeader.setBigEndianOrder(false);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(stream);
        _tiffHeader.write(ios);

        ios.close();
        final byte[] bytes = stream.toByteArray();
        assertEquals(0x2a, bytes[2]);
        assertEquals(0x00, bytes[3]);
    }

    public void testMagicNumber_bigEndian() throws Exception {
        _tiffHeader.setBigEndianOrder(true);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(stream);
        _tiffHeader.write(ios);

        ios.close();
        final byte[] bytes = stream.toByteArray();
        assertEquals(0x00, bytes[2]);
        assertEquals(0x2a, bytes[3]);
    }

    public void testFirstIfdOffset_littleEndian() throws Exception {
        _tiffHeader.setBigEndianOrder(false);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(stream);
        _tiffHeader.write(ios);

        ios.close();
        final byte[] bytes = stream.toByteArray();
        assertEquals(0x0a, bytes[4]);
        assertEquals(0x00, bytes[5]);
        assertEquals(0x00, bytes[6]);
        assertEquals(0x00, bytes[7]);
    }

    public void testFirstIfdOffset_bigEndian() throws Exception {
        _tiffHeader.setBigEndianOrder(true);
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(stream);
        _tiffHeader.write(ios);

        ios.close();
        final byte[] bytes = stream.toByteArray();
        assertEquals(0x00, bytes[4]);
        assertEquals(0x00, bytes[5]);
        assertEquals(0x00, bytes[6]);
        assertEquals(0x0a, bytes[7]);
    }
}
