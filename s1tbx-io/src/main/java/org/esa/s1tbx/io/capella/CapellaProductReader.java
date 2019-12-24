/*
 * Copyright (C) 2020 Skywatch. https://www.skywatch.com
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
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


/**
 * The product reader for Capella products.
 */
public class CapellaProductReader extends SARReader {

    private CapellaProductDirectory dataDir;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public CapellaProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }


    @Override
    public void close() throws IOException {
        dataDir.close();
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

        Object input = getInput();
        if (input instanceof InputStream) {
            throw new IOException("InputStream not supported");
        }

        final Path path = getPathFromInput(input);
        File metadataFile = path.toFile();
        if (metadataFile.isDirectory()) {
            metadataFile = CapellaProductReaderPlugIn.findMetadataFile(path);
        }

        dataDir = new CapellaProductDirectory(metadataFile);
        dataDir.readProductDirectory();
        final Product product = dataDir.createProduct();

        product.getGcpGroup();
        product.setFileLocation(metadataFile);
        product.setProductReader(this);

        return product;
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
        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);

        bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                bandInfo.imageID, bandInfo.bandSampleOffset);

        final double nodatavalue = destBand.getNoDataValue();
        final double scaleFactor = dataDir.getScaleFactor();
        for(int i=0; i< destBuffer.getNumElems(); ++i) {
            double val = destBuffer.getElemDoubleAt(i);
            if(val != nodatavalue) {
                destBuffer.setElemDoubleAt(i, val * scaleFactor);
            }
        }
    }
}
