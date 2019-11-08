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
package org.esa.s1tbx.io.snaphu;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.envi.EnviConstants;
import org.esa.snap.dataio.envi.EnviProductReader;
import org.esa.snap.dataio.envi.Header;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by lveci on 17/12/2014.
 */
public class SNAPHUReader extends EnviProductReader {

    private Header header;
    private boolean isComplex = false;
    private ImageInputStream inStream;

    public SNAPHUReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected Product readProductNodesImpl() throws IOException {
        final Path inputPath = ReaderUtils.getPathFromInput(getInput());
        final File inputFile = inputPath.toFile();

        try (BufferedReader headerReader = getHeaderReader(inputFile)) {
            header = new Header(headerReader);
        }

        final int dataType = header.getDataType();
        if (dataType == EnviConstants.TYPE_ID_COMPLEXFLOAT32) {

            return createComplexProduct(inputFile, header, ProductData.TYPE_FLOAT32);
        } else if (dataType == EnviConstants.TYPE_ID_COMPLEXFLOAT64) {

            return createComplexProduct(inputFile, header, ProductData.TYPE_FLOAT64);
        } else {

            return super.readProductNodesImpl();
        }
    }

    private Product createComplexProduct(final File inputFile, final Header header, final int bandType) throws IOException {
        final int width = header.getNumSamples();
        final int height = header.getNumLines();
        final Product product = new Product(inputFile.getName(), "RAT", width, height);
        product.setProductReader(this);
        product.setFileLocation(inputFile);
        product.setDescription(header.getDescription());
        product.getMetadataRoot().addElement(header.getAsMetadata());
        initGeoCoding(product, header);

        applyBeamProperties(product, header.getBeamProperties());

        final Band tgtBandI = new Band("i_band", bandType, width, height);
        tgtBandI.setUnit("real");
        product.addBand(tgtBandI);

        final Band tgtBandQ = new Band("q_band", ProductData.TYPE_FLOAT32, width, height);
        tgtBandQ.setUnit("imaginary");
        product.addBand(tgtBandQ);

        ReaderUtils.createVirtualIntensityBand(product, tgtBandI, tgtBandQ, "_band");

        isComplex = true;
        String ratFilePath = inputFile.getPath().substring(0, inputFile.getPath().length()-4);
        inStream = new FileImageInputStream(new File(ratFilePath));
        inStream.setByteOrder(header.getJavaByteOrder());

        return product;
    }

    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY,
                                          int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY,
                                          Band destBand,
                                          int destOffsetX, int destOffsetY,
                                          int destWidth, int destHeight,
                                          ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (isComplex) {

            final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
            Product product = destBand.getProduct();
            final int elemSize = destBuffer.getElemSize();

            final int headerOffset = header.getHeaderOffset();
            final int bandIndex = product.getBandIndex(destBand.getName());

            // band interleaved by pixel
            int numBands = 2;
            final long lineSizeInBytes = header.getNumSamples() * numBands * elemSize;
            ProductData lineData = ProductData.createInstance(destBuffer.getType(), sourceWidth * numBands);

            pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceMaxY - sourceOffsetY);
            try {
                int destPos = 0;
                for (int sourceY = sourceOffsetY; sourceY <= sourceMaxY; sourceY += sourceStepY) {
                    if (pm.isCanceled()) {
                        break;
                    }
                    synchronized (inStream) {
                        long lineStartPos = headerOffset + sourceY * lineSizeInBytes;
                        inStream.seek(lineStartPos + elemSize * sourceOffsetX * numBands);
                        lineData.readFrom(0, sourceWidth * numBands, inStream);
                    }
                    for (int x = 0; x < sourceWidth; x++) {
                        destBuffer.setElemDoubleAt(destPos++, lineData.getElemDoubleAt(x * numBands + bandIndex));
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }

        } else {
            super.readBandRasterDataImpl(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX, sourceStepY,
                                         destBand, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
        }
    }

    @Override
    public void close() throws IOException {
        if (inStream != null) {
            inStream.close();
        }
        super.close();
    }
}
