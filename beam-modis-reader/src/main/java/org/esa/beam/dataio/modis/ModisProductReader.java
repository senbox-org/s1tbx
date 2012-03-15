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
import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.bandreader.ModisBandReader;
import org.esa.beam.dataio.modis.hdf.HdfAttributeContainer;
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.dataio.modis.productdb.ModisProductDb;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ModisProductReader extends AbstractProductReader {

    private int _fileId;
    private int _sdStart;
    private ModisFileReader fileReader;
    private ModisGlobalAttributes globalAttributes;
    private NetcdfFile netcdfFile;

    /**
     * Constructs a new MODIS product reader.
     *
     * @param plugin the plug-in which created this reader instance
     */
    public ModisProductReader(ModisProductReaderPlugIn plugin) {
        super(plugin);

        netcdfFile = null;

        _fileId = HDFConstants.FAIL;
        _sdStart = HDFConstants.FAIL;
    }

    /**
     * Closes the access to all currently opened resources such as file input streams.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }

        try {
            // @todo 1 tb/tb remove try/catch
            fileReader.close();
        } catch (HDFException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // @todo 1 tb/tb remove this code
        if (_fileId != HDFConstants.FAIL) {
            try {
                // and finish file access
                // ----------------------
                HDF.getWrap().SDend(_sdStart);
                _sdStart = HDFConstants.FAIL;
                HDF.getWrap().Hclose(_fileId);
                _fileId = HDFConstants.FAIL;

            } catch (HDFException e) {
                throw new ProductIOException(e.getMessage());
            }
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

        try {
            reader.readBandData(sourceOffsetX, sourceOffsetY,
                                sourceWidth, sourceHeight,
                                sourceStepX, sourceStepY,
                                destBuffer, pm);
        } catch (InvalidRangeException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
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
        final File inputFile = getInputFile();
        netcdfFile = NetcdfFile.open(inputFile.getPath());

        readGlobalMetaData(inputFile, netcdfFile);
        checkProductType();


        final Dimension productDimensions = globalAttributes.getProductDimensions();
        final Product product = new Product(globalAttributes.getProductName(),
                                            globalAttributes.getProductType(),
                                            productDimensions.width,
                                            productDimensions.height,
                                            this);
        product.setFileLocation(inputFile);

        readGlobalMetaData(inputFile, netcdfFile);
        fileReader = new ModisFileReader();
        try {
            fileReader.addRastersAndGeocoding(netcdfFile, product, globalAttributes);
        } catch (HDFException e) {
            throw new ProductIOException(e.getMessage());
        }

        // @todo 1 tb/tb remove this code
        try {
            try {
                final File inFile = getInputFile();
                final String path = inFile.getPath();
                _fileId = HDF.getWrap().Hopen(path, HDFConstants.DFACC_RDONLY);
                _sdStart = HDF.getWrap().SDstart(path, HDFConstants.DFACC_RDONLY);


                // @todo ---------------------------------


                // add all metadata if required
                // ----------------------------
                if (!isMetadataIgnored()) {
                    // add the metadata
                    addMetadata(product);
                }

                // ModisProductDb db = ModisProductDb.getInstance();
// Remarked by sabine because the product flipper uses a TiePointGeoCcoding
// and makes not an instance of ModisTiePointGeoCoding
//                if (!(_dayMode ^ db.mustFlip(prod.getProductType()))) {
//                    prod = ProductFlipper.createFlippedProduct(prod, ProductFlipper.FLIP_BOTH, prod.getName(),
//                            prod.getDescription());
//                    prod.setFileLocation(inFile);
//                }
                final Date sensingStart = globalAttributes.getSensingStart();
                if (sensingStart != null) {
                    product.setStartTime(ProductData.UTC.create(sensingStart, 0));
                }
                final Date sensingStop = globalAttributes.getSensingStop();
                if (sensingStop != null) {
                    product.setEndTime(ProductData.UTC.create(sensingStop, 0));
                }
                return product;
            } finally {
                HDF.getWrap().Hclose(_fileId);
            }
        } catch (HDFException e) {
            throw new ProductIOException(e.getMessage());
        }
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

    /**
     * Reads the global metadata and extracts some basic constants (width, height etc ...)
     *
     * @throws ProductIOException on product access failures
     */
    private void readGlobalMetaData(File inFile, NetcdfFile netcdfFile) throws ProductIOException {
        if (ModisProductReaderPlugIn.isImappFormat(netcdfFile)) {
            globalAttributes = new ModisImappAttributes(inFile, netcdfFile);
        } else {
            globalAttributes = new ModisDaacAttributes(netcdfFile);
        }
    }


    /**
     * Adds the metadata to the product passed in
     *
     * @param prod
     */
    private void addMetadata(Product prod) {
        HdfAttributeContainer container;
        MetadataElement mdElem = null;
        MetadataElement globalElem = null;

        mdElem = prod.getMetadataRoot();
        if (mdElem == null) {
            return;
        }

        globalElem = new MetadataElement(ModisConstants.GLOBAL_META_NAME);

        for (int n = 0; n < globalAttributes.getNumGlobalAttributes(); n++) {
            globalElem.addAttribute(globalAttributes.getMetadataAttributeAt(n));
        }

        mdElem.addElement(globalElem);
    }

    /**
     * Checks the product type against the list of known types. Throws ProductIOException if it doesn't fit.
     */
    private void checkProductType() throws ProductIOException {
        final String productType = globalAttributes.getProductType();
        final ModisProductDb db = ModisProductDb.getInstance();
        if (!db.isSupportedProduct(productType)) {
            throw new ProductIOException("Unsupported product of type '" + productType + '\'');
        }
    }
}
