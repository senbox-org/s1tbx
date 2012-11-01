/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.nest.dataio.generic.GenericReader;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.ReaderUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The product reader for TerraSarX products.
 *
 */
public class TerraSarXProductReader extends AbstractProductReader {

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
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
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
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        Product product;
        try {
            final File fileFromInput = ReaderUtils.getFileFromInput(getInput());
            dataDir = new TerraSarXProductDirectory(fileFromInput, new File(fileFromInput.getParentFile(), "IMAGEDATA"));
            dataDir.readProductDirectory();
            product = dataDir.createProduct();
            product.setFileLocation(fileFromInput);
            product.setProductReader(this);
            /*if(dataDir.isComplex()) {
                product = product.createFlippedProduct(ProductFlipper.FLIP_HORIZONTAL, product.getName(), product.getDescription());
                product.setFileLocation(fileFromInput);
                product.setProductReader(this);
            }    */
            product.getGcpGroup();
            product.setModified(false);
        } catch (Exception e) {
            Debug.trace(e.toString());
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
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
            if(bandInfo != null && bandInfo.img != null) {
                bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        bandInfo.imageID, bandInfo.bandSampleOffset);
            } else {
                boolean oneOfTwo = true;
                if(destBand.getUnit().equals(Unit.IMAGINARY))
                    oneOfTwo = false;

                final ImageInputStream iiStream = dataDir.getCosarImageInputStream(destBand);
                readBandRasterDataSLCShort(sourceOffsetX, sourceOffsetY,
                                                 sourceWidth, sourceHeight,
                                                 sourceStepX, sourceStepY,
                                                 destWidth, destBuffer,
                                                 oneOfTwo, iiStream, pm);
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static synchronized void readBandRasterDataSLCShort(final int sourceOffsetX, final int sourceOffsetY,
                                      final int sourceWidth, final int sourceHeight,
                                      final int sourceStepX, final int sourceStepY,
                                      final int destWidth, final ProductData destBuffer, boolean oneOf2,
                                      final ImageInputStream iiStream, final ProgressMonitor pm)
                                        throws IOException
    {
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

        final long imageRecordLength = (long)rtnb;
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        final int x = sourceOffsetX * 4;
        final int filler = 2;
        final int asri = rs;
        final int asfv = rs;
        final int aslv = rs;
        final long xpos = rtnb + x + ((filler + asri +filler+ asfv +filler+ aslv +filler+filler)*4);
        iiStream.setByteOrder(ByteOrder.BIG_ENDIAN);

        pm.beginTask("Reading band...", sourceMaxY - sourceOffsetY);
        final short[] destLine = new short[destWidth];
        int y=0;
        try {
            final short[] srcLine = new short[sourceWidth*2];
            for (y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                synchronized (iiStream) {
                    iiStream.seek(imageRecordLength * y + xpos);
                    iiStream.readFully(srcLine, 0, srcLine.length);
                }

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (oneOf2)
                    GenericReader.copyLine1Of2(srcLine, destLine, sourceStepX);
                else
                    GenericReader.copyLine2Of2(srcLine, destLine, sourceStepX);

                System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);

                pm.worked(1);
            }
        } catch(Exception e) {
            System.out.println(e.toString());  
            final int currentLineIndex = (y - sourceOffsetY) * destWidth;
            Arrays.fill(destLine, (short)0);
            System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
        } finally {
            pm.done();
        }
    }   
}