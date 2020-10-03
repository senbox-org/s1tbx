/*
 * Copyright (C) 2020 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.saocom;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;

import javax.imageio.ImageReadParam;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The product reader for TerraSarX products.
 */
public class SaocomProductReader extends SARReader {

    private SaocomProductDirectory dataDir = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public SaocomProductReader(final ProductReaderPlugIn readerPlugIn) {
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
            final Path inputPath = getPathFromInput(getInput());
            dataDir = createProductDirectory(inputPath.toFile());
            dataDir.readProductDirectory();
            product = dataDir.createProduct();

            product.setFileLocation(inputPath.toFile());
            product.setProductReader(this);
            addCommonSARMetadata(product);

            setQuicklookBandName(product);
            addQuicklooks(product);

            product.getGcpGroup();
            product.setModified(false);
        } catch (Throwable e) {
            handleReaderException(e);
        }

        return product;
    }

    protected SaocomProductDirectory createProductDirectory(final File fileFromInput) {
        return new SaocomProductDirectory(fileFromInput);
    }

    private void addQuicklooks(final Product product) {
        addQuicklook(product, Quicklook.DEFAULT_QUICKLOOK_NAME, getQuicklookFile("PREVIEW/BROWSE.tif"));
    }

    private File getQuicklookFile(final String relativeFilePath) {
        try {
            if (dataDir.exists(dataDir.getRootFolder() + relativeFilePath)) {
                return dataDir.getFile(dataDir.getRootFolder() + relativeFilePath);
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
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        try {
            final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);

            final boolean isSLC = dataDir.isSLC();
            final boolean isImaginary = destBand.getUnit().contains(Unit.IMAGINARY);

            final ImageReadParam param = bandInfo.img.getReader().getDefaultReadParam();
            param.setSourceSubsampling(sourceStepX, sourceStepY,
                    sourceOffsetX % sourceStepX,
                    sourceOffsetY % sourceStepY);

            final RenderedImage image = bandInfo.img.getReader().readAsRenderedImage(0, param);
            final Raster data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));

            final DataBuffer dataBuffer = data.getDataBuffer();
            final SampleModel sampleModel = data.getSampleModel();
            final int dataBufferType = dataBuffer.getDataType();
            final int sampleOffset = bandInfo.imageID + bandInfo.bandSampleOffset;
            final Object dest = destBuffer.getElems();

            try {
                if (dest instanceof int[] && (dataBufferType == DataBuffer.TYPE_USHORT || dataBufferType == DataBuffer.TYPE_SHORT
                        || dataBufferType == DataBuffer.TYPE_INT)) {
                    sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (int[]) dest, dataBuffer);
                } else if (dataBufferType == DataBuffer.TYPE_FLOAT && dest instanceof float[]) {
                    sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (float[]) dest, dataBuffer);
                    //} else if (dataBufferType == DataBuffer.TYPE_DOUBLE && dest instanceof double[]) {
                    //    sampleModel.getSamples(0, 0, destWidth, destHeight, sampleOffset, (double[]) dest, dataBuffer);
                } else {
                    final double[] dArray = new double[destWidth * destHeight];
                    sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), sampleOffset, dArray, dataBuffer);

                    int i = 0;
                    for (double value : dArray) {
                        if (isSLC) {
                            long bits = Double.doubleToLongBits(value);
                            if (isImaginary) {
                                int firstHalf = (int) (bits >> 32);
                                destBuffer.setElemDoubleAt(i, Float.intBitsToFloat(firstHalf));
                            } else {
                                int secondHalf = (int) (bits & 0xffffffff);
                                destBuffer.setElemDoubleAt(i, Float.intBitsToFloat(secondHalf));
                            }
                            ++i;
                        } else {
                            destBuffer.setElemDoubleAt(i++, value);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading " +destBand.getName()+" "+ e.getMessage());

                final double[] dArray = new double[destWidth * destHeight];
                sampleModel.getSamples(0, 0, data.getWidth(), data.getHeight(), sampleOffset, dArray, dataBuffer);

                int i = 0;
                for (double value : dArray) {
                    destBuffer.setElemDoubleAt(i++, value);
                }
            }
        } catch (Exception e2) {
            System.out.println("Error reading " +destBand.getName()+" "+ e2.getMessage());
            int size = destWidth * destHeight;
            for (int i = 0; i < size; ++i) {
                destBuffer.setElemDoubleAt(i++, 0);
            }
        }
    }
}
