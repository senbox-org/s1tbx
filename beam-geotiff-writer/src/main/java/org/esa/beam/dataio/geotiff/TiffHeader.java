/*
 * $Id: TiffHeader.java,v 1.1 2006/09/14 13:19:21 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.Guardian;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A TIFF header implementation for the GeoTIFF format.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class TiffHeader {

    public final static TiffShort MAGIC_NUMBER = new TiffShort(42);
    public final static TiffLong FIRST_IFD_OFFSET = new TiffLong(10);

    private final static TiffShort _LITTLE_ENDIAN = new TiffShort(0x4949);
    private final static TiffShort _BIG_ENDIAN = new TiffShort(0x4D4D);

    private final TiffIFD[] _ifds;
    private boolean _bigEndianOrder = true;

    public TiffHeader(final Product[] products) {
        Guardian.assertNotNull("products", products);
        Guardian.assertGreaterThan("products.length", products.length, 0);
        _ifds = new TiffIFD[products.length];
        for (int i = 0; i < products.length; i++) {
            _ifds[i] = new TiffIFD(products[i]);
        }
    }

    public void write(final ImageOutputStream ios) throws IOException {
        if (_bigEndianOrder) {
            ios.setByteOrder(ByteOrder.BIG_ENDIAN);
            _BIG_ENDIAN.write(ios);
        } else {
            ios.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            _LITTLE_ENDIAN.write(ios);
        }
        MAGIC_NUMBER.write(ios);
        FIRST_IFD_OFFSET.write(ios);

        long offset = FIRST_IFD_OFFSET.getValue();
        for (int i = 0; i < _ifds.length; i++) {
            final TiffIFD ifd = _ifds[i];
            final long nextOffset = computeNextIfdOffset(i, offset, ifd);
            ifd.write(ios, offset, nextOffset);
            offset = nextOffset;
        }
    }

    public TiffIFD getIfdAt(final int index) {
        Guardian.assertWithinRange("index", index, 0, _ifds.length - 1);
        return _ifds[index];
    }

    public void setBigEndianOrder(final boolean bigEndianOrder) {
        _bigEndianOrder = bigEndianOrder;
    }

    private long computeNextIfdOffset(final int i, final long offset, final TiffIFD ifd) {
        if (i < _ifds.length - 1) {
            return offset + ifd.getRequiredEntireSize();
        }
        return 0;
    }
}
