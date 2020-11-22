/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.risat1;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The product reader for RISAT-1 products.
 */
public class Risat1ProductReader extends SARReader {

    protected Risat1ProductDirectory dataDir = null;

    private static final String lutsigma = "lutSigma";
    private static final String lutgamma = "lutGamma";
    private static final String lutbeta = "lutBeta";

    private boolean isAscending;
    private boolean isAntennaPointingRight;

    private static final boolean flipToSARGeometry = System.getProperty(SystemUtils.getApplicationContextId() +
            ".flip.to.sar.geometry", "false").equals("true");

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Risat1ProductReader(final ProductReaderPlugIn readerPlugIn) {
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
     * @throws IOException if an I/O error occurs
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
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        try {
            Path inputPath = getPathFromInput(getInput());
            if(Files.isDirectory(inputPath)) {
                inputPath = inputPath.resolve(Risat1Constants.BAND_HEADER_NAME);
            }
            dataDir = createDirectory(inputPath.toFile());
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();

            final MetadataElement absMeta = AbstractMetadata.getAbstractedMetadata(product);
            isAscending = absMeta.getAttributeString(AbstractMetadata.PASS).equals("ASCENDING");
            isAntennaPointingRight = absMeta.getAttributeString(AbstractMetadata.antenna_pointing).equals("right");

            product.getGcpGroup();
            product.setFileLocation(inputPath.toFile());
            product.setProductReader(this);

            setQuicklookBandName(product);
            addQuicklook(product, Quicklook.DEFAULT_QUICKLOOK_NAME, getQuicklookFile());

            return product;
        } catch (Exception e) {
            handleReaderException(e);
        }
        return null;
    }

    protected Risat1ProductDirectory createDirectory(final File fileFromInput) {
        return new Risat1ProductDirectory(fileFromInput);
    }

    private File getQuicklookFile() {
        try {
            if(dataDir.exists(dataDir.getRootFolder() + "BrowseImage.tif")) {
                return dataDir.getFile(dataDir.getRootFolder() + "BrowseImage.tif");
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
            if (isAscending) {
                readAscendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        0, bandInfo.img, bandInfo.bandSampleOffset, isAntennaPointingRight);
            } else {
                readDescendingRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                        0, bandInfo.img, bandInfo.bandSampleOffset, isAntennaPointingRight);
            }
        }
    }

    public void readAscendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                        final int sourceStepX, final int sourceStepY,
                                        final ProductData destBuffer,
                                        final int destOffsetX, final int destOffsetY,
                                        final int destWidth, final int destHeight,
                                        final int imageID, final ImageIOFile img,
                                        final int bandSampleOffset,
                                        final boolean isAntennaPointingRight) throws IOException {

        final Raster data;

        synchronized (dataDir) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = reader.readAsRenderedImage(0, param);
            if (flipToSARGeometry) {
                if (isAntennaPointingRight) { // flip the image up side down
                    data = image.getData(new Rectangle(destOffsetX,
                            Math.max(0, img.getSceneHeight() - destOffsetY - destHeight),
                            destWidth, destHeight));
                } else { // flip the image upside down, then flip it left to right
                    data = image.getData(new Rectangle(Math.max(0, img.getSceneWidth() - destOffsetX - destWidth),
                            Math.max(0, img.getSceneHeight() - destOffsetY - destHeight),
                            destWidth, destHeight));
                }
            } else {
                data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
            }
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        if (flipToSARGeometry) {
            final int[] dArray = new int[dataBuffer.getSize()];
            sampleModel.getSamples(0, 0, w, h, imageID + bandSampleOffset, dArray, dataBuffer);

            int srcStride, destStride;
            if (isAntennaPointingRight) { // flip the image upside down
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = (h - r - 1) * w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemIntAt(destStride + c, dArray[srcStride + c]);
                    }
                }
            } else { // flip the image upside down, then flip it left to right
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = (h - r) * w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemIntAt(destStride - c - 1, dArray[srcStride + c]);
                    }
                }
            }
        } else { // no flipping is needed
            sampleModel.getSamples(0, 0, w, h, sampleOffset, (int[]) destBuffer.getElems(), dataBuffer);
        }
    }

    public void readDescendingRasterBand(final int sourceOffsetX, final int sourceOffsetY,
                                         final int sourceStepX, final int sourceStepY,
                                         final ProductData destBuffer,
                                         final int destOffsetX, final int destOffsetY,
                                         final int destWidth, final int destHeight,
                                         final int imageID, final ImageIOFile img,
                                         final int bandSampleOffset,
                                         final boolean isAntennaPointingRight) throws IOException {

        final Raster data;
    try {
        synchronized (dataDir) {
            final ImageReader reader = img.getReader();
            final ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = reader.readAsRenderedImage(0, param);
            if (flipToSARGeometry && isAntennaPointingRight) {  // flip the image left to right
                data = image.getData(new Rectangle(Math.max(0, img.getSceneWidth() - destOffsetX - destWidth),
                        destOffsetY, destWidth, destHeight));
            } else {
                data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
            }
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        if(destBuffer.getType() == ProductData.TYPE_FLOAT32) {
            if (flipToSARGeometry && isAntennaPointingRight) { // flip the image left to right
                final float[] dArray = new float[dataBuffer.getSize()];
                sampleModel.getSamples(0, 0, w, h, sampleOffset, dArray, dataBuffer);

                int srcStride, destStride;
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = r * w + w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemFloatAt(destStride - c - 1, dArray[srcStride + c]);
                    }
                }
            } else { // no flipping is needed
                sampleModel.getSamples(0, 0, w, h, sampleOffset, (float[]) destBuffer.getElems(), dataBuffer);
            }
        } else {
            if (flipToSARGeometry && isAntennaPointingRight) { // flip the image left to right
                final int[] dArray = new int[dataBuffer.getSize()];
                sampleModel.getSamples(0, 0, w, h, sampleOffset, dArray, dataBuffer);

                int srcStride, destStride;
                for (int r = 0; r < h; r++) {
                    srcStride = r * w;
                    destStride = r * w + w;
                    for (int c = 0; c < w; c++) {
                        destBuffer.setElemIntAt(destStride - c - 1, dArray[srcStride + c]);
                    }
                }
            } else { // no flipping is needed
                sampleModel.getSamples(0, 0, w, h, sampleOffset, (int[]) destBuffer.getElems(), dataBuffer);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

}
