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
package org.esa.s1tbx.io.sunraster;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.Unit;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * Reader for sun raster files
 */
public class SunRasterReader extends SARReader {

    private ImageInputStream inStream;
    private int width, height, depth;

    private static final int RAS_MAGIC = 0x59a66a95;
    private static final int COMPRESSION_NONE = 0x00000001;
    private static final int RAS_HEADER_SIZE = 32;

    public SunRasterReader() {
        super(null);
    }

    protected Product readProductNodesImpl() throws IOException {
        final Path inputPath = getPathFromInput(getInput());
        inStream = new FileImageInputStream(inputPath.toFile());
        inStream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

        readHeader();

        final Product product = new Product(inputPath.getFileName().toString(), "SunRaster",
                width, height);
        product.setProductReader(this);
        product.setFileLocation(inputPath.toFile());

        final Band band = new Band("data", ProductData.TYPE_FLOAT32, width, height);
        band.setUnit(Unit.AMPLITUDE);
        product.addBand(band);

        return product;
    }

    private void readHeader() throws IOException {
        byte[] header = new byte[RAS_HEADER_SIZE];
        inStream.readFully(header);
        int magic = getIntBE(header, 0);
        if (magic != RAS_MAGIC) {
            throw new IOException("This stream is not a valid " +
                    "Sun RAS stream (bad magic: " + Integer.toHexString(magic) +
                    " instead of " + Integer.toHexString(RAS_MAGIC));
        }
        width = getIntBE(header, 4);
        height = getIntBE(header, 8);
        if (width < 1 || height < 1) {
            throw new IOException("Width and height must both " +
                    "be larger than zero; found width=" + width + ", height=" +
                    height + ".");
        }
        depth = getIntBE(header, 12);
    }

    private static int getIntBE(byte[] src, int srcOffset) {
        return (src[srcOffset + 3] & 0xff) |
                ((src[srcOffset + 2] & 0xff) << 8) |
                ((src[srcOffset + 1] & 0xff) << 16) |
                ((src[srcOffset] & 0xff) << 24);
    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int elemSize = destBuffer.getElemSize();

        final int headerOffset = RAS_HEADER_SIZE;
        final long lineSizeInBytes = width * elemSize;
        ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth);

        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
        try {
            int destPos = 0;
            for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                synchronized (inStream) {
                    long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                    inStream.seek(lineStartPos + elemSize * sourceOffsetX);
                    lineData.readFrom(0, sourceWidth, inStream);
                }
                for (int x = 0; x < sourceWidth; x++) {
                    destBuffer.setElemDoubleAt(destPos++, lineData.getElemDoubleAt(x));
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void close() throws IOException {
        if (inStream != null) {
            inStream.close();
        }
        super.close();
    }
}
