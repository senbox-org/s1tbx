/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.modis;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.modis.attribute.DaacAttributes;
import org.esa.beam.dataio.modis.attribute.ImappAttributes;
import org.esa.beam.dataio.modis.bandreader.ModisBandReader;
import org.esa.beam.dataio.modis.netcdf.NetCDFAttributes;
import org.esa.beam.dataio.modis.netcdf.NetCDFUtils;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.dataio.modis.productdb.ModisProductDb;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductFlipper;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ModisProductReader extends AbstractProductReader {

    private ModisFileReader fileReader;
    private ModisGlobalAttributes globalAttributes;
    private NetcdfFile netcdfFile;
    private NetCDFAttributes netCDFAttributes;
    private NetCDFVariables netCDFVariables;

    /**
     * Constructs a new MODIS product reader.
     *
     * @param plugin the plug-in which created this reader instance
     */
    public ModisProductReader(ModisProductReaderPlugIn plugin) {
        super(plugin);
    }

    /**
     * Closes the access to all currently opened resources such as file input streams.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (fileReader != null) {
            fileReader.close();
            fileReader = null;
        }

        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }

        super.close();
    }

    ///////////////////////////////////////////////////////////////////////////
    ////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * The template method which is called by the method after an optional spatial subset has
     * been applied to the input parameters.
     * <p/>
     * <p>The destination band, buffer and region parameters are exactly the ones passed to
     * the original  call. Since the <code>destOffsetX</code> and <code>destOffsetY</code>
     * parameters are already taken into acount in the <code>sourceOffsetX</code> and
     * <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destBand      the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     * @throws IOException if an I/O error occurs
     * @see #readBandRasterData
     * @see #getSubsetDef
     */
    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                                       int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                                       int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                                       ProgressMonitor pm) throws IOException {

        final ModisBandReader reader = fileReader.getBandReader(destBand);

        if (reader == null) {
            throw new IOException("No band reader for band '" + destBand.getName() + "' available!");
        }

        reader.readBandData(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            destBuffer, pm);
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inFile = getInputFile();
        final String inputFilePath = inFile.getPath();

        netcdfFile = NetcdfFile.open(inputFilePath, null);

        readGlobalMetaData(inFile);
        checkProductType();

        fileReader = createFileReader();

        final Dimension productDim = globalAttributes.getProductDimensions(netcdfFile.getDimensions());
        Product product = new Product(globalAttributes.getProductName(),
                                      globalAttributes.getProductType(),
                                      productDim.width,
                                      productDim.height,
                                      this);
        product.setFileLocation(inFile);

        final NetCDFVariables netCDFVariables = new NetCDFVariables();
        netCDFVariables.add(netcdfFile.getVariables());
        fileReader.addRastersAndGeoCoding(product, globalAttributes, netCDFVariables);

        // add all metadata if required
        // ----------------------------
        if (!isMetadataIgnored()) {
            addMetadata(product);
        }

        final Date sensingStart = globalAttributes.getSensingStart();
        if (sensingStart != null) {
            product.setStartTime(ProductData.UTC.create(sensingStart, 0));
        }
        final Date sensingStop = globalAttributes.getSensingStop();
        if (sensingStop != null) {
            product.setEndTime(ProductData.UTC.create(sensingStop, 0));
        }

        //if (!(_dayMode ^ db.mustFlip(prod.getProductType()))) {
//                    prod = ProductFlipper.createFlippedProduct(prod, ProductFlipper.FLIP_BOTH, prod.getName(),
//                            prod.getDescription());
//                    prod.setFileLocation(inFile);
//                }
        final ModisProductDb productDb = ModisProductDb.getInstance();
        if (productDb.mustFlip(globalAttributes.getProductType())) {
            // @todo 1 tb/tb this creates weired geo-codings!!!!!
//            product = ProductFlipper.createFlippedProduct(product,
//                                                          ProductFlipper.FLIP_BOTH,
//                                                          product.getName(),
//                                                          product.getDescription());
//                    prod.setFileLocation(inFile);
            System.out.println("FLIP_IT!!!!!!!!!!!!!!!!!!");
        }
        return product;
    }

    private ModisFileReader createFileReader() {
        return new ModisFileReader();
    }

    private File getInputFile() {
        File inFile;
        if (getInput() instanceof String) {
            inFile = new File((String) getInput());
        } else if (getInput() instanceof File) {
            inFile = (File) getInput();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + getInput());
        }
        return inFile;
    }

    private void readGlobalMetaData(File inFile) throws IOException {
        netCDFAttributes = new NetCDFAttributes();
        netCDFAttributes.add(netcdfFile.getGlobalAttributes());

        netCDFVariables = new NetCDFVariables();
        netCDFVariables.add(netcdfFile.getVariables());

        // check wheter daac or imapp
        if (isImappFormat()) {
            globalAttributes = new ImappAttributes(inFile, netCDFVariables, netCDFAttributes);
        } else {
            globalAttributes = new DaacAttributes(netCDFVariables);
        }
    }

    private boolean isImappFormat() {
        return netCDFVariables.get(ModisConstants.STRUCT_META_KEY) == null;
    }

    /**
     * Adds the metadata to the product passed in
     *
     * @param prod the product
     */
    private void addMetadata(Product prod) {
        final MetadataElement mdElem = prod.getMetadataRoot();
        if (mdElem == null) {
            return;
        }

        final MetadataElement globalElem = new MetadataElement(ModisConstants.GLOBAL_META_NAME);

        final Attribute[] attributes = netCDFAttributes.getAll();
        for (final Attribute attribute : attributes) {
            globalElem.addAttribute(NetCDFUtils.toMetadataAttribute(attribute));
        }

        mdElem.addElement(globalElem);
    }

    /**
     * Checks the product type against the list of known types. Throws ProductIOException if it doesn't fit.
     */
    private void checkProductType() throws IOException {
        final String productType = globalAttributes.getProductType();
        final ModisProductDb db = ModisProductDb.getInstance();
        if (!db.isSupportedProduct(productType)) {
            throw new ProductIOException("Unsupported product of type '" + productType + '\'');
        }
    }
}
