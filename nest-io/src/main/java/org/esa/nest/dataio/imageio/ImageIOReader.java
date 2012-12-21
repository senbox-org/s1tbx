/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.imageio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.ReaderUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The product reader for ImageIO products.
 *
 */
public class ImageIOReader extends AbstractProductReader {

    private ImageIOFile imgIOFile = null;
    private String productType = "productType";

    private final transient Map<Band, ImageIOFile.BandInfo> bandMap = new HashMap<Band, ImageIOFile.BandInfo>(3);

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public ImageIOReader(final ProductReaderPlugIn readerPlugIn) {
       super(readerPlugIn);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = ReaderUtils.getFileFromInput(getInput());

        imgIOFile = new ImageIOFile(inputFile, ImageIOFile.getIIOReader(inputFile));

        productType = imgIOFile.getReader().getFormatName();

        final Product product = new Product(imgIOFile.getName(),
                                            productType,
                                            imgIOFile.getSceneWidth(), imgIOFile.getSceneHeight());
        product.setFileLocation(inputFile);

        int bandCnt = 1;
        for(int i=0; i < imgIOFile.getNumImages(); ++i) {

            for(int b=0; b < imgIOFile.getNumBands(); ++b) {
                final Band band = new Band("band"+ bandCnt++, imgIOFile.getDataType(),
                                   imgIOFile.getSceneWidth(), imgIOFile.getSceneHeight());
                product.addBand(band);
                bandMap.put(band, new ImageIOFile.BandInfo(imgIOFile, i, b));

                if(imgIOFile.isIndexed()) {
                    band.setImageInfo(imgIOFile.getImageInfo());
                    band.setSampleCoding(imgIOFile.getIndexCoding());
                    product.getIndexCodingGroup().add(imgIOFile.getIndexCoding());
                }
            }
        }

        //product.setDescription(getProductDescription());

        //addGeoCoding(product);
        addMetaData(product, inputFile);

        product.getGcpGroup();
        product.setProductReader(this);
        product.setModified(false);
        product.setFileLocation(inputFile);

        return product;
    }

    @Override
    public void close() throws IOException {
        super.close();

        imgIOFile.close();
    }

    static DecodeQualification checkProductQualification(File file) {
        return DecodeQualification.SUITABLE;
    }

    private void addMetaData(final Product product, final File inputFile) throws IOException {
        final MetadataElement root = product.getMetadataRoot();

        final MetadataElement absRoot = AbstractMetadata.addAbstractedMetadataHeader(root);

        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT, imgIOFile.getName());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.PRODUCT_TYPE, productType);
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_samples_per_line, imgIOFile.getSceneWidth());
        AbstractMetadata.setAttribute(absRoot, AbstractMetadata.num_output_lines, imgIOFile.getSceneHeight());

        AbstractMetadata.loadExternalMetadata(product, absRoot, inputFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand,
                                          int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                          ProductData destBuffer, ProgressMonitor pm) throws IOException {

        ImageIOFile.BandInfo bandInfo = bandMap.get(destBand);

        imgIOFile.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceStepX, sourceStepY,
                                        destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                                        bandInfo.imageID, bandInfo.bandSampleOffset);
    }

}
