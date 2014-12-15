/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.terrasarx;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.dataio.SARReader;
import org.esa.nest.dataio.generic.GenericReader;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The product reader for TerraSarX products.
 */
public class TerraSarXProductReader extends SARReader {

    private TerraSarXProductDirectory dataDir = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public TerraSarXProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (dataDir != null) {
            dataDir.close();
            dataDir = null;
        }
        super.close();
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        Product product = null;
        try {
            final File fileFromInput = ReaderUtils.getFileFromInput(getInput());
            dataDir = new TerraSarXProductDirectory(fileFromInput);
            dataDir.readProductDirectory();
            product = dataDir.createProduct();
            product.setFileLocation(fileFromInput);
            product.setProductReader(this);
            setQuicklookBandName(product);
            /*if(dataDir.isComplex()) {
                product = product.createFlippedProduct(ProductFlipper.FLIP_HORIZONTAL, product.getName(), product.getDescription());
                product.setFileLocation(fileFromInput);
                product.setProductReader(this);
            }    */
            product.getGcpGroup();
            product.setModified(false);
        } catch (Throwable e) {
            handleReaderException(e);
        }

        return product;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        try {
            final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);
            if (bandInfo != null && bandInfo.img != null) {

                Product product = destBand.getProduct();

                if (OperatorUtils.isMapProjected(product)) {

                    bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                            destBuffer, destOffsetX, destOffsetY, destWidth, destHeight, bandInfo.imageID,
                            bandInfo.bandSampleOffset);

                } else {

                    MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
                    final boolean isAscending = absRoot.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
                    if (isAscending) {
                        readAscendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                                destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                                0, bandInfo.img, bandInfo.bandSampleOffset);
                    } else {
                        readDescendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                                destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                                0, bandInfo.img, bandInfo.bandSampleOffset);
                    }
                }

            } else {

                final ImageInputStream iiStream = dataDir.getCosarImageInputStream(destBand);
                final boolean isImaginary = destBand.getUnit() != null && destBand.getUnit().equals(Unit.IMAGINARY);
                readBandRasterDataSLC16Bit(sourceOffsetX, sourceOffsetY,
                        sourceWidth, sourceHeight,
                        sourceStepX, sourceStepY,
                        destWidth, destBuffer,
                        !isImaginary, iiStream, pm);
            }
        } catch (Exception e) {
            handleReaderException(e);
        }
    }

    public void readAscendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                        final int sourceStepX, final int sourceStepY,
                                        final ProductData destBuffer,
                                        final int destOffsetX, final int destOffsetY,
                                        final int destWidth, final int destHeight,
                                        final int imageID, final ImageIOFile img,
                                        final int bandSampleOffset) throws IOException {
        final Raster data;

        synchronized (dataDir) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = reader.readAsRenderedImage(0, param);
            Rectangle rect = new Rectangle(destOffsetX, Math.max(0, img.getSceneHeight() - destOffsetY - destHeight),
                    destWidth, destHeight);
            data = image.getData(rect);
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        final double[] dArray = new double[dataBuffer.getSize()];
        sampleModel.getSamples(0, 0, w, h, sampleOffset, dArray, dataBuffer);

        // flip the image upside down
        int is, id;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                is = r * w + c;
                id = (h - r - 1) * w + c;
                destBuffer.setElemDoubleAt(id, dArray[is]);
            }
        }
    }

    public void readDescendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                         final int sourceStepX, final int sourceStepY,
                                         final ProductData destBuffer,
                                         final int destOffsetX, final int destOffsetY,
                                         final int destWidth, final int destHeight,
                                         final int imageID, final ImageIOFile img,
                                         final int bandSampleOffset) throws IOException {

        final Raster data;

        synchronized (dataDir) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = reader.readAsRenderedImage(0, param);
            data = image.getData(new Rectangle(Math.max(0, img.getSceneWidth() - destOffsetX - destWidth),
                    destOffsetY, destWidth, destHeight));
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        final double[] dArray = new double[dataBuffer.getSize()];
        sampleModel.getSamples(0, 0, w, h, sampleOffset, dArray, dataBuffer);

        // flip the image left to right
        int is, id;
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                is = r * w + c;
                id = r * w + w - c - 1;
                destBuffer.setElemDoubleAt(id, dArray[is]);
            }
        }
    }

    private static float convert16BitsTo32BitFloat(final char halfFloat) {

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

    private static void copyToDataBuffer(final boolean isSSC, final char[] destLine, final ProductData destBuffer,
                                         final int currentLineIndex, final int destWidth) {

        if (isSSC) {
            // The data type of ProductData is INT16 which is short
            System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
        } else {
            for (int i = 0; i < destWidth; i++) {
                destBuffer.setElemFloatAt(i+currentLineIndex, convert16BitsTo32BitFloat(destLine[i]));
            }
        }
    }

    private static synchronized void readBandRasterDataSLC16Bit(final int sourceOffsetX, final int sourceOffsetY,
                                                                final int sourceWidth, final int sourceHeight,
                                                                final int sourceStepX, final int sourceStepY,
                                                                final int destWidth, final ProductData destBuffer, boolean oneOf2,
                                                                final ImageInputStream iiStream, final ProgressMonitor pm)
            throws IOException {

        //System.out.println("product type = " + destBuffer.getTypeString());

        iiStream.seek(0);
        final int bib = iiStream.readInt();
        final int rsri = iiStream.readInt();
        final int rs = iiStream.readInt();
        final int as = iiStream.readInt();
        final int bi = iiStream.readInt();
        final int rtnb = iiStream.readInt();
        final int tnl = iiStream.readInt();
        //System.out.print("bib"+bib+" rsri"+rsri+" rs"+rs+" as"+as+" bi"+bi+" rtbn"+rtnb+" tnl"+tnl);
        //System.out.println(" sourceOffsetX="+sourceOffsetX+" sourceOffsetY="+sourceOffsetY);

        final long imageRecordLength = (long) rtnb;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * 4;
        final int filler = 2;
        final int asri = rs;
        final int asfv = rs;
        final int aslv = rs;

        final int csar = iiStream.readInt();
        final int version = iiStream.readInt();

        if (version != 1 && version != 2) {
            throw new IOException("Unknown version = " + version);
        }

        //System.out.println("csar = " + csar + " version = " + version);

        final boolean isSSC = (version == 1); // true means it is SSC, false means it is CoSSC

        //final long xpos = rtnb + x + ((filler + asri +filler+ asfv +filler+ aslv +filler+filler)*4);
        final long xpos = rtnb + x + ((filler + asri + filler + asfv + filler + aslv + filler) * 4);
        iiStream.setByteOrder(ByteOrder.BIG_ENDIAN);

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        final char[] destLine = new char[destWidth];
        int y = 0;
        try {
            final char[] srcLine = new char[sourceWidth * 2];
            for (y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                //synchronized (iiStream) {
                iiStream.seek(imageRecordLength * y + xpos);
                iiStream.readFully(srcLine, 0, srcLine.length);
                //}

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (oneOf2)
                    GenericReader.copyLine1Of2(srcLine, destLine, sourceStepX);
                else
                    GenericReader.copyLine2Of2(srcLine, destLine, sourceStepX);

                copyToDataBuffer(isSSC, destLine, destBuffer, currentLineIndex, destWidth);

                pm.worked(1);
            }
        } catch (Exception e) {
            //System.out.println(e.toString());
            final int currentLineIndex = (y - sourceOffsetY) * destWidth;
            Arrays.fill(destLine, (char) 0);
            copyToDataBuffer(isSSC, destLine, destBuffer, currentLineIndex, destWidth);
        } finally {
            pm.done();
        }
    }
}