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
import org.esa.beam.dataio.modis.bandreader.ModisBandReader;
import org.esa.beam.dataio.modis.hdf.HdfAttributeContainer;
import org.esa.beam.dataio.modis.hdf.HdfAttributes;
import org.esa.beam.dataio.modis.hdf.HdfUtils;
import org.esa.beam.dataio.modis.hdf.IHDF;
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.dataio.modis.productdb.ModisProductDb;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

public class ModisProductReader extends AbstractProductReader {

    private HdfAttributes _globalHdfAttrs;
    private final HashMap _bandReader;
    private final Logger _logger;
    private int _fileId;
    private int _sdStart;
    private ModisFileReader _fileReader;
    private ModisGlobalAttributes _globalAttributes;

    /**
     * Constructs a new MODIS product reader.
     *
     * @param plugin the plug-in which created this reader instance
     */
    public ModisProductReader(ModisProductReaderPlugIn plugin) {
        super(plugin);

        _fileId = HDFConstants.FAIL;
        _sdStart = HDFConstants.FAIL;

        _bandReader = new HashMap();

        _logger = BeamLogManager.getSystemLogger();
    }

    /**
     * Closes the access to all currently opened resources such as file input streams.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (_fileId != HDFConstants.FAIL) {

            // close all band readers
            // ----------------------
            Collection readers = _bandReader.values();
            Iterator it = readers.iterator();
            ModisBandReader reader;
            while (it.hasNext()) {
                reader = (ModisBandReader) it.next();
                reader.close();
            }
            _bandReader.clear();

            _fileReader.close();

            // and finish file access
            // ----------------------
            HDF.getWrap().SDend(_sdStart);
            _sdStart = HDFConstants.FAIL;
            HDF.getWrap().Hclose(_fileId);
            _fileId = HDFConstants.FAIL;
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

        ModisBandReader reader = _fileReader.getBandReader(destBand);

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
        final IHDF ihdf = HDF.getWrap();
        try {
            final File inFile = getInputFile();
            final String path = inFile.getPath();
            _fileId = ihdf.Hopen(path, HDFConstants.DFACC_RDONLY);
            _sdStart = ihdf.SDstart(path, HDFConstants.DFACC_RDONLY);

            readGlobalMetaData(inFile);
            checkProductType();
            //checkDayNightMode();
            _fileReader = createFileReader();

            final Dimension productDim = _globalAttributes.getProductDimensions();
            final Product product;
            product = new Product(_globalAttributes.getProductName(), _globalAttributes.getProductType(),
                    productDim.width, productDim.height, this);
            product.setFileLocation(inFile);
            _fileReader.addRastersAndGeocoding(_sdStart, _globalAttributes, product);

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
            final Date sensingStart = _globalAttributes.getSensingStart();
            if (sensingStart != null) {
                product.setStartTime(ProductData.UTC.create(sensingStart, 0));
            }
            final Date sensingStop = _globalAttributes.getSensingStop();
            if (sensingStop != null) {
                product.setEndTime(ProductData.UTC.create(sensingStop, 0));
            }
            return product;
        } finally {
            ihdf.Hclose(_fileId);
        }
    }

    private ModisFileReader createFileReader() {
        return new ModisFileReader();
//        if (isImappFormat()) {
//            return new ModisImappFileReader();
//        } else {
//            return new ModisDaacFileReader();
//        }
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
     * @throws IOException
     */
    private void readGlobalMetaData(File inFile) throws IOException {
        _globalHdfAttrs = HdfUtils.readAttributes(_sdStart);

        // check wheter daac or imapp
        if (isImappFormat()) {
            _globalAttributes = new ModisImappAttributes(inFile, _sdStart, _globalHdfAttrs);
        } else {
            _globalAttributes = new ModisDaacAttributes(_globalHdfAttrs);
        }
    }

    private boolean isImappFormat() {
        return _globalHdfAttrs.getStringAttributeValue(ModisConstants.STRUCT_META_KEY) == null;
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

        for (int n = 0; n < _globalHdfAttrs.getNumAttributes(); n++) {
            container = _globalHdfAttrs.getAttributeAt(n);
            globalElem.addAttribute(container.toMetadataAttribute());
        }

        mdElem.addElement(globalElem);
    }

    /**
     * Checks the product type against the list of known types. Throws ProductIOException if it doesn't fit.
     */
    private void checkProductType() throws ProductIOException {
        final String productType = _globalAttributes.getProductType();
        final ModisProductDb db = ModisProductDb.getInstance();
        if (!db.isSupportedProduct(productType)) {
            throw new ProductIOException("Unsupported product of type '" + productType + '\'');
        }
    }

    /**
     * Checks whether the product is in daymode or in night mode
     */
    private void checkDayNightMode() {
        int[] numDayScans = _globalHdfAttrs.getIntAttributeValue(ModisConstants.NUM_OF_DAY_SCANS_KEY);
        int[] numNightScans = _globalHdfAttrs.getIntAttributeValue(ModisConstants.NUM_OF_NIGHT_SCANS_KEY);

        if ((numNightScans == null) || (numDayScans == null) ||
                (numNightScans.length <= 0) || (numDayScans.length <= 0)) {
            _logger.warning("Unable to retrieve day mode or night mode scan number. Assuming day mode");
            //_dayMode = true;
        } else {
            //_dayMode = numDayScans[0] > numNightScans[0];
        }
    }
}
