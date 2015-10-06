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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.Guardian;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A TIFF header implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision: 2182 $ $Date: 2008-06-12 11:09:11 +0200 (Do, 12 Jun 2008) $
 */
public class TiffHeader {

    public static final TiffShort MAGIC_NUMBER = new TiffShort(42);
    private static final TiffShort LITTLE_ENDIAN = new TiffShort(0x4949);
    private static final TiffShort BIG_ENDIAN = new TiffShort(0x4D4D);
    public static final TiffLong FIRST_IFD_OFFSET = new TiffLong(10);

    private final TiffIFD[] ifds;
    private boolean bigEndianOrder = true;

    public TiffHeader(final Product[] products) {
        Guardian.assertNotNull("products", products);
        Guardian.assertGreaterThan("products.length", products.length, 0);
        ifds = new TiffIFD[products.length];
        for (int i = 0; i < products.length; i++) {
            ifds[i] = new TiffIFD(products[i]);
        }
    }

    public void write(final ImageOutputStream ios) throws IOException {
        if (bigEndianOrder) {
            ios.setByteOrder(ByteOrder.BIG_ENDIAN);
            BIG_ENDIAN.write(ios);
        } else {
            ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            LITTLE_ENDIAN.write(ios);
        }
        MAGIC_NUMBER.write(ios);
        FIRST_IFD_OFFSET.write(ios);

        long offset = FIRST_IFD_OFFSET.getValue();
        for (int i = 0; i < ifds.length; i++) {
            final TiffIFD ifd = ifds[i];
            final long nextOffset = computeNextIfdOffset(i, offset, ifd);
            ifd.write(ios, offset, nextOffset);
            offset = nextOffset;
        }
    }

    public TiffIFD getIfdAt(final int index) {
        Guardian.assertWithinRange("index", index, 0, ifds.length - 1);
        return ifds[index];
    }

    public void setBigEndianOrder(final boolean bigEndianOrder) {
        this.bigEndianOrder = bigEndianOrder;
    }

    private long computeNextIfdOffset(final int i, final long offset, final TiffIFD ifd) {
        if (i < ifds.length - 1) {
            return offset + ifd.getRequiredEntireSize();
        }
        return 0;
    }
}
