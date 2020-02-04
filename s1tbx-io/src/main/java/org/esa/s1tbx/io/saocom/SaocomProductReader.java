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
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);

        bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                bandInfo.imageID, bandInfo.bandSampleOffset);

//        final boolean isSLC = dataDir.isSLC();
//        final boolean isImaginary = destBand.getUnit().contains(Unit.IMAGINARY);
//        final double nodatavalue = destBand.getNoDataValue();
//        for(int i=0; i< destBuffer.getNumElems(); ++i) {
//            int val = destBuffer.getElemIntAt(i);
//            if(isSLC) {
//                if(isImaginary) {
//                    double secondHalf = (short) (val & 0xffff);
//                    destBuffer.setElemDoubleAt(i, secondHalf);
//                } else {
//                    double firstHalf = (short) (val >> 16);
//                    destBuffer.setElemDoubleAt(i, firstHalf);
//                }
//            } else {
//                if (val != nodatavalue) {
//                    destBuffer.setElemDoubleAt(i, Math.sqrt(val));
//                }
//            }
//        }
    }
}
