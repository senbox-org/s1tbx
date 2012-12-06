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
package org.esa.nest.dataio.sentinel1;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.ReaderUtils;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;

/**
 * The product reader for Sentinel1 products.
 *
 */
public class Sentinel1ProductReader extends AbstractProductReader {

    protected Sentinel1ProductDirectory dataDir = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Sentinel1ProductReader(final ProductReaderPlugIn readerPlugIn) {
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
            dataDir = new Sentinel1ProductDirectory(fileFromInput, new File(fileFromInput.getParentFile(), "measurement"));
            dataDir.readProductDirectory();
            product = dataDir.createProduct();
            product.getGcpGroup();
            product.setFileLocation(fileFromInput);
            product.setProductReader(this);
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
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);
        if(bandInfo != null && bandInfo.img != null) {
            if(dataDir.isSLC()) {
                boolean oneOfTwo = true;
                if(destBand.getUnit().equals(Unit.IMAGINARY))
                    oneOfTwo = false;

                readSLCRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        bandInfo.bandSampleOffset, bandInfo.img, oneOfTwo);
            } else {
                bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                                                destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                                                bandInfo.imageID, bandInfo.bandSampleOffset);
            }
        }
    }

    public void readSLCRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                                   final int sourceStepX, final int sourceStepY,
                                                   final ProductData destBuffer,
                                                   final int destOffsetX, final int destOffsetY,
                                                   int destWidth, int destHeight,
                                                   final int imageID, final ImageIOFile img,
                                                   final boolean oneOfTwo) throws IOException {
        final Raster data;
        synchronized(dataDir) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY, sourceOffsetX % sourceStepX, sourceOffsetY % sourceStepY);

            final RenderedImage image = img.getReader().readAsRenderedImage(0, param);
            data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
        }

        final SampleModel sampleModel = data.getSampleModel();
        destWidth = Math.min(destWidth, sampleModel.getWidth());
        destHeight = Math.min(destHeight, sampleModel.getHeight());

        try {
            final double[] srcArray = new double[destWidth * destHeight];
            sampleModel.getSamples(0, 0, destWidth, destHeight, imageID, srcArray, data.getDataBuffer());

            if (oneOfTwo)
                copyLine1Of2(srcArray, (short[])destBuffer.getElems(), sourceStepX);
            else
                copyLine2Of2(srcArray, (short[])destBuffer.getElems(), sourceStepX);

        } catch(Exception e) {
            //e.printStackTrace();
        }
    }

    public static void copyLine1Of2(final double[] srcArray, final short[] destArray, final int sourceStepX) {
        final int length = srcArray.length;
        for (int i = 0; i < length; i += sourceStepX) {
            destArray[i] = (short)srcArray[i];
        }
    }

    public static void copyLine2Of2(final double[] srcArray, final short[] destArray, final int sourceStepX) {
        final int length = srcArray.length;
        for (int i = 0; i < length; i += sourceStepX) {
            destArray[i] = (short)((int)srcArray[i] >> 16);
        }
    }
}