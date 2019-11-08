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
package org.esa.s1tbx.io.seasat;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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
import java.nio.file.Path;

/**
 * The product reader for SeaSat products.
 */
public class SeaSatProductReader extends SARReader {

    protected SeaSatProductDirectory dataDir = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public SeaSatProductReader(final ProductReaderPlugIn readerPlugIn) {
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
            final Path inputPath = getPathFromInput(getInput());
            dataDir = createDirectory(inputPath.toFile());
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();

            final MetadataElement absMeta = AbstractMetadata.getAbstractedMetadata(product);

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

    protected SeaSatProductDirectory createDirectory(final File fileFromInput) {
        return new SeaSatProductDirectory(fileFromInput);
    }

    private File getQuicklookFile() {
        try {
            final String[] files = dataDir.listFiles(dataDir.getRootFolder());
            for(String file : files) {
                if (file.endsWith(".jpg")) {
                    return dataDir.getFile(dataDir.getRootFolder() + file);
                }
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
            readRasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                           destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                           0, bandInfo.img, bandInfo.bandSampleOffset);
        }
    }

    public void readRasterBand(final int sourceOffsetX, final int sourceOffsetY,
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
            data = image.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
        }

        final int w = data.getWidth();
        final int h = data.getHeight();
        final DataBuffer dataBuffer = data.getDataBuffer();
        final SampleModel sampleModel = data.getSampleModel();
        final int sampleOffset = imageID + bandSampleOffset;

        sampleModel.getSamples(0, 0, w, h, sampleOffset, (float[]) destBuffer.getElems(), dataBuffer);
    }
}
