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
package org.esa.s1tbx.io.spacety;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.S1TBXProductReaderPlugIn;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.s1tbx.io.DataCache;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.util.SystemUtils;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The product reader for Spacety products.
 */
public class SpacetyProductReader extends SARReader {

    protected SpacetyDirectory dataDir = null;
    private final DataCache cache;
    private final boolean useCache = true;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public SpacetyProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        cache = useCache ? new DataCache() : null;
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
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        super.close();
        if (dataDir != null) {
            dataDir.close();
            dataDir = null;
        }
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
            Path inputPath = getPathFromInput(getInput());
            if (Files.isDirectory(inputPath)) {
                inputPath = inputPath.resolve(SpacetyProductReaderPlugIn.PRODUCT_HEADER_NAME);
            }
            if (!Files.exists(inputPath)) {
                throw new IOException(inputPath.toString() + " not found");
            }

            dataDir = new SpacetyDirectory(inputPath.toFile(), (S1TBXProductReaderPlugIn)getReaderPlugIn());
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();
            product.setFileLocation(inputPath.toFile());
            product.setProductReader(this);

            addCommonSARMetadata(product);

            setQuicklookBandName(product);
            addQuicklook(product, Quicklook.DEFAULT_QUICKLOOK_NAME, getQuicklookFile());

            product.setModified(false);

            return product;
        } catch (Exception e) {
            handleReaderException(e);
        }

        return null;
    }

    private File getQuicklookFile() {
        try {
            if (dataDir.exists(dataDir.getRootFolder() + "preview/quick-look.png")) {
                return dataDir.getFile(dataDir.getRootFolder() + "preview/quick-look.png");
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to load quicklook " + dataDir.getProductName());
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
        }
    }

    private void readSLCRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                   final int sourceStepX, final int sourceStepY,
                                   final ProductData destBuffer,
                                   final int destOffsetX, final int destOffsetY,
                                   int destWidth, int destHeight,
                                   final ImageIOFile.BandInfo bandInfo) {

        final int length;
        final int[] srcArray;
        final Rectangle destRect = new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight);

        if (useCache) {
            final DataCache.DataKey datakey = new DataCache.DataKey(bandInfo.img, destRect);
            DataCache.Data cachedData = cache.get(datakey);
            if (cachedData != null && cachedData.valid) {
                srcArray = cachedData.intArray;
                length = srcArray.length;
            } else {
                cachedData = readRect(datakey, bandInfo,
                        sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destRect);

                srcArray = cachedData.intArray;
                length = srcArray.length;
            }
        } else {

            DataCache.Data cachedData = readRect(null, bandInfo,
                    sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY, destRect);

            srcArray = cachedData.intArray;
            length = srcArray.length;
        }

        if (destBuffer.getElemSize() > 2) {
            final short[] destArray = (short[]) destBuffer.getElems();
            if (!bandInfo.isImaginary || !bandInfo.isComplexSample) {
                if (sourceStepX == 1) {
                    int i = 0;
                    for (int srcVal : srcArray) {
                        destArray[i++] = (short)srcVal;
                    }
                } else {
                    for (int i = 0; i < length; i += sourceStepX) {
                        destArray[i] = (short)srcArray[i];
                    }
                }
            } else {
                if (sourceStepX == 1) {
                    int i = 0;
                    for (int srcVal : srcArray) {
                        destArray[i++] = (short)(srcVal >> 16);
                    }
                } else {
                    for (int i = 0; i < length; i += sourceStepX) {
                        destArray[i] = (short)(srcArray[i] >> 16);
                    }
                }
            }
        } else {
            final short[] destArray = (short[]) destBuffer.getElems();
            if (!bandInfo.isImaginary || !bandInfo.isComplexSample) {
                int i = 0;
                for (int srcVal : srcArray) {
                    destArray[i++] = (short) srcVal;
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
