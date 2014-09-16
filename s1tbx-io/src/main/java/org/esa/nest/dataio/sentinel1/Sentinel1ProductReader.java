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
package org.esa.nest.dataio.sentinel1;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.nest.dataio.SARReader;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.snap.gpf.ReaderUtils;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;

/**
 * The product reader for Sentinel1 products.
 */
public class Sentinel1ProductReader extends SARReader {

    protected Sentinel1Directory dataDir = null;
    private DataCache cache = new DataCache();

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

        try {
            final File fileFromInput = ReaderUtils.getFileFromInput(getInput());
            if (Sentinel1ProductReaderPlugIn.isLevel1(fileFromInput)) {
                dataDir = new Sentinel1Level1Directory(fileFromInput);
            } else if(Sentinel1ProductReaderPlugIn.isLevel2(fileFromInput)) {
                dataDir = new Sentinel1Level2Directory(fileFromInput);
            } else if (Sentinel1ProductReaderPlugIn.isLevel0(fileFromInput)) {
                dataDir = new Sentinel1Level0Directory(fileFromInput);
            }
            if(dataDir == null) {
                Sentinel1ProductReaderPlugIn.validateInput(fileFromInput);
            }
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();
            product.getGcpGroup();
            product.setFileLocation(fileFromInput);
            product.setProductReader(this);
            setQuicklookBandName(product);
            product.setModified(false);

            return product;
        } catch (Exception e) {
            handleReaderException(e);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);
        if (bandInfo != null && bandInfo.img != null) {
            if (dataDir.isSLC()) {

                readSLCRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight, bandInfo);
            } else {
                bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        bandInfo.imageID, bandInfo.bandSampleOffset);
            }
        } else if (dataDir instanceof Sentinel1Level2Directory) {

            final Sentinel1Level2Directory s1L1Dir = (Sentinel1Level2Directory) dataDir;
            synchronized (s1L1Dir) {
                readLevel2OCNBand(s1L1Dir.getOCNReader(),
                        sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                        sourceStepX, sourceStepY, destBand, destOffsetX,
                        destOffsetY, destWidth, destHeight, destBuffer);
            }
        }
    }

    public void readSLCRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                  final int sourceStepX, final int sourceStepY,
                                  final ProductData destBuffer,
                                  final int destOffsetX, final int destOffsetY,
                                  int destWidth, int destHeight,
                                  final ImageIOFile.BandInfo bandInfo) throws IOException {

        int length;
        int[] srcArray;

        final Rectangle rect = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
        final DataCache.DataKey datakey = new DataCache.DataKey(bandInfo.img, rect);
        final DataCache.Data cachedData = cache.get(datakey);
        if (cachedData != null && cachedData.valid) {
            srcArray = cachedData.intArray;
            length = srcArray.length;
        } else {
            synchronized (dataDir) {
                final ImageReader reader = bandInfo.img.getReader();
                final ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(sourceStepX, sourceStepY, sourceOffsetX % sourceStepX, sourceOffsetY % sourceStepY);

                final RenderedImage image = reader.readAsRenderedImage(0, param);
                final Raster data = image.getData(rect);

                final SampleModel sampleModel = data.getSampleModel();
                destWidth = Math.min(destWidth, sampleModel.getWidth());
                destHeight = Math.min(destHeight, sampleModel.getHeight());

                length = destWidth * destHeight;
                srcArray = new int[length];
                sampleModel.getSamples(0, 0, destWidth, destHeight, bandInfo.bandSampleOffset, srcArray, data.getDataBuffer());

                cache.put(datakey, new DataCache.Data(srcArray));
            }
        }

        final short[] destArray = (short[]) destBuffer.getElems();
        if (!bandInfo.isImaginary) {
            if (sourceStepX == 1) {
                //System.arraycopy(srcArray, 0, destArray, 0, length);
                int i = 0;
                for (int srcVal : srcArray) {
                    destArray[i++] = (short) srcVal;
                }
            } else {
                for (int i = 0; i < length; i += sourceStepX) {
                    destArray[i] = (short) srcArray[i];
                }
            }
        } else {
            if (sourceStepX == 1) {
                int i = 0;
                for (int srcVal : srcArray) {
                    destArray[i++] = (short) (srcVal >> 16);
                }
            } else {
                for (int i = 0; i < length; i += sourceStepX) {
                    destArray[i] = (short) (srcArray[i] >> 16);
                }
            }
        }
    }

    public void readLevel2OCNBand(Sentinel1OCNReader OCNReader,
                                  int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                  int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                  int destOffsetY, int destWidth, int destHeight, ProductData destBuffer) throws IOException {

        if (OCNReader == null) {

            throw new IOException("Sentinel1OCNReader not found");
        }

        OCNReader.readData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                sourceStepX, sourceStepY, destBand, destOffsetX,
                destOffsetY, destWidth, destHeight, destBuffer);
    }
}