/*
 * Copyright (C) 2018 Skywatch. https://www.skywatch.co
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
package org.csa.rstb.io.rcm;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.datamodel.quicklooks.Quicklook;
import org.esa.snap.core.gpf.common.ImportVectorOp;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.File;
import java.io.IOException;

/**
 * The product reader for RCM products.
 */
public class RCMProductReader extends SARReader {

    protected RCMProductDirectory dataDir = null;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public RCMProductReader(final ProductReaderPlugIn readerPlugIn) {
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
            File fileFromInput = ReaderUtils.getFileFromInput(getInput());
            if (fileFromInput.isDirectory()) {
                fileFromInput = RCMProductReaderPlugIn.findMetadataFile(fileFromInput);
            }
            dataDir = createDirectory(fileFromInput);
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();

            product.getGcpGroup();
            product.setFileLocation(fileFromInput);
            product.setProductReader(this);

            addQuicklook(product, Quicklook.DEFAULT_QUICKLOOK_NAME, getQuicklookFile());

            return product;
        } catch (Exception e) {
            handleReaderException(e);
        }
        return null;
    }

    protected RCMProductDirectory createDirectory(final File fileFromInput) {
        return new RCMProductDirectory(fileFromInput);
    }

    private File getQuicklookFile() {
        try {
            final String folder = "preview";
            final String[] files = dataDir.findFilesContaining(dataDir.getRootFolder() + folder, ".png");
            if (files != null && files.length > 0) {
                return dataDir.getFile(folder +"/"+ files[0]);
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
            bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                    destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                    bandInfo.imageID, bandInfo.bandSampleOffset);
        }
    }
}
