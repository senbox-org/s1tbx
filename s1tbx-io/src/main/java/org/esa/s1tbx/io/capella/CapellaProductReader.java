/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.capella;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.S1TBXProductReaderPlugIn;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.DataCache;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.engine_utilities.datamodel.Unit;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * The product reader for Capella products.
 */
public class CapellaProductReader extends SARReader {

    private CapellaProductDirectory dataDir;
    private final DataCache cache;
    private final S1TBXProductReaderPlugIn readerPlugIn;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public CapellaProductReader(final S1TBXProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.cache = new DataCache();
        this.readerPlugIn = readerPlugIn;
    }

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
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            Object input = getInput();
            if (input instanceof InputStream) {
                throw new IOException("InputStream not supported");
            }

            final Path path = getPathFromInput(input);
            File metadataFile = readerPlugIn.findMetadataFile(path);

            dataDir = new CapellaProductDirectory(metadataFile);
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();

            addCommonSARMetadata(product);
            product.getGcpGroup();
            product.setFileLocation(metadataFile);
            product.setProductReader(this);

            return product;
        } catch (Throwable e) {
            handleReaderException(e);
        }
        return null;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (dataDir == null) {
            return;
        }

        final int[] srcArray;
        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);

        final Rectangle destRect = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);
        final DataCache.DataKey datakey = new DataCache.DataKey(bandInfo.img, destRect);
        DataCache.Data cachedData = cache.get(datakey);
        if (cachedData != null && cachedData.valid) {
            srcArray = cachedData.intArray;
        } else {
            cachedData = readRect(datakey, bandInfo, sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destRect);

            srcArray = cachedData.intArray;
        }

        final boolean isSLC = dataDir.isSLC();
        final boolean isImaginary = destBand.getUnit().contains(Unit.IMAGINARY);
        final float nodatavalue = (float)destBand.getNoDataValue();
        final double scaleFactor = dataDir.getScaleFactor();
        final float[] elems = (float[]) destBuffer.getElems();
        final int numElems = elems.length;

        if (isSLC) {
            for (int i = 0; i < numElems; ++i) {

                if (isImaginary) {
                    double secondHalf = (short) (srcArray[i] & 0xffff);
                    elems[i] = (float) (secondHalf * scaleFactor);
                } else {
                    double firstHalf = (short) (srcArray[i] >> 16);
                    elems[i] = (float) (firstHalf * scaleFactor);
                }
            }
        } else {
            for (int i = 0; i < numElems; ++i) {
                if (srcArray[i] != nodatavalue) {
                    elems[i] = (float) Math.sqrt(srcArray[i] * scaleFactor);
                } else {
                    elems[i] = nodatavalue;
                }
            }
        }
    }

    private synchronized DataCache.Data readRect(final DataCache.DataKey datakey, final ImageIOFile.BandInfo bandInfo,
                                                 int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY,
                                                 final Rectangle destRect) {
        try {
            final ImageReader imageReader = bandInfo.img.getReader();
            final ImageReadParam readParam = imageReader.getDefaultReadParam();
            if (sourceStepX == 1 && sourceStepY == 1) {
                readParam.setSourceRegion(destRect);
            }
            readParam.setSourceSubsampling(sourceStepX, sourceStepY, sourceOffsetX % sourceStepX, sourceOffsetY % sourceStepY);
            final RenderedImage subsampledImage = imageReader.readAsRenderedImage(0, readParam);

            final Raster data = subsampledImage.getData(destRect);

            final SampleModel sampleModel = data.getSampleModel();
            final int destWidth = Math.min((int) destRect.getWidth(), sampleModel.getWidth());
            final int destHeight = Math.min((int) destRect.getHeight(), sampleModel.getHeight());

            final int length = destWidth * destHeight;
            final int[] srcArray = new int[length];
            sampleModel.getSamples(0, 0, destWidth, destHeight, bandInfo.bandSampleOffset, srcArray, data.getDataBuffer());

            DataCache.Data cachedData = new DataCache.Data(srcArray);
            if (datakey != null) {
                cache.put(datakey, cachedData);
            }
            return cachedData;
        } catch (Exception e) {
            final int[] srcArray = new int[(int) destRect.getWidth() * (int) destRect.getHeight()];
            DataCache.Data cachedData = new DataCache.Data(srcArray);
            if (datakey != null) {
                cache.put(datakey, cachedData);
            }
            return cachedData;
        }
    }
}
