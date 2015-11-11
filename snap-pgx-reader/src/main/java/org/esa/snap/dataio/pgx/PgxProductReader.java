/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.dataio.pgx;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * @author Norman Fomferra
 * @see PgxProductReaderPlugIn
 */
public class PgxProductReader extends AbstractProductReader {

    private Band band;
    private long dataPosition;

    public static class Header {
        public final ByteOrder byteOrder;
        public final boolean signed;
        public final int bitDepth;
        public final int width;
        public final int height;

        public Header(ByteOrder byteOrder, boolean signed, int bitDepth, int width, int height) {
            this.byteOrder = byteOrder;
            this.signed = signed;
            this.bitDepth = bitDepth;
            this.width = width;
            this.height = height;
        }
    }

    private ImageInputStream stream;

    public PgxProductReader(PgxProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Object input = getInput();
        if (input instanceof String || input instanceof File) {
            stream = new FileImageInputStream(new File(input.toString()));
        } else if (input instanceof ImageInputStream) {
            stream = (ImageInputStream) input;
        } else {
            throw new IllegalStateException("Illegal input: " + input);
        }

        final String inputName = input != stream ? input.toString() : "PGX-stream";
        final Header header = readHeader(stream);
        if (header == null) {
            throw new IOException(inputName + " does not appear to have a valid PGX format");
        }

        if (header.bitDepth != 16) {
            throw new IOException(inputName + ": can (currently) only decode a bit depths of 16, but found " + header.bitDepth);
        }
        if (header.byteOrder != ByteOrder.BIG_ENDIAN) {
            throw new IOException(inputName + ": can (currently) only decode " + ByteOrder.BIG_ENDIAN + ", but found " + header.byteOrder);
        }

        dataPosition = stream.getStreamPosition();
        final Product product = new Product(FileUtils.getFilenameWithoutExtension(new File(inputName)), "PGX", header.width, header.height);
        product.setFileLocation(new File(inputName));
        product.setPreferredTileSize(new Dimension(512, 512));
        band = product.addBand("data", header.signed ? ProductData.TYPE_INT16 : ProductData.TYPE_UINT16);
        return product;
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;

            band = null;
        }
        super.close();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX,
                                          int destOffsetY,
                                          int destWidth,
                                          int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (band != destBand) {
            return;
        }
        synchronized (this) {
            final int width = destBand.getRasterWidth();
            if (sourceOffsetX == 0 && sourceWidth == width && destBuffer.getNumElems() == sourceWidth * sourceHeight) {
                long pos = dataPosition + sourceOffsetY * width * 2;
                stream.seek(pos);
                destBuffer.readFrom(stream);
            } else if (destWidth == sourceWidth || destHeight == sourceHeight) {
                for (int i = 0; i < sourceHeight; i++) {
                    long pos = dataPosition + ((i + sourceOffsetY) * width + sourceOffsetX) * 2;
                    stream.seek(pos);
                    destBuffer.readFrom(i * sourceWidth, sourceWidth, stream);
                }
            } else {
                //Debug.trace("Weirrrrd: " + destWidth + "x" + destHeight + " != " + sourceWidth + "x" + sourceHeight);
            }
        }
    }


    static Header readHeader(ImageInputStream stream) throws IOException {
        byte[] buffer = new byte[64];
        stream.read(buffer);
        String header = new String(buffer).split("\n")[0];
        return parseHeaderLine(header);
    }

    static Header parseHeaderLine(String headerLine) {
        //Get information from header
        StringTokenizer st = new StringTokenizer(headerLine);
        try {
            final int nTokens = st.countTokens();

            // Magic String
            if (!(st.nextToken()).equals("PG")) {
                return null;
            }

            // Endian Order
            final ByteOrder byteOrder;
            String tmp = st.nextToken();
            if (tmp.equals("LM")) {
                byteOrder = ByteOrder.LITTLE_ENDIAN;
            } else if (tmp.equals("ML")) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            } else {
                return null;
            }

            // Unsigned/signed if present in the header
            final boolean signed;
            if (nTokens == 6) {
                tmp = st.nextToken();
                if (tmp.equals("+")) {
                    signed = false;
                } else if (tmp.equals("-")) {
                    signed = true;
                } else {
                    return null;
                }
            } else {
                signed = false;
            }

            // bit-depth, width, height
            final int bitDepth;
            final int width;
            final int height;

            try {
                bitDepth = new Integer(st.nextToken());
                // bitDepth must be between 1 and 31
                if ((bitDepth <= 0) || (bitDepth > 31)) {
                    return null;
                }
                width = new Integer(st.nextToken());
                height = new Integer(st.nextToken());

                return new Header(byteOrder, signed, bitDepth, width, height);
            } catch (NumberFormatException e) {
                return null;
            }
        } catch (NoSuchElementException e) {
            return null;
        }
    }

}
