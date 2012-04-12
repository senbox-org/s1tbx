/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.atsr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.io.File;
import java.io.IOException;

/**
 * The product reader for ERS ATSR products.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
public class AtsrProductReader extends AbstractProductReader {

    private AtsrFile _file;

    /**
     * Constructs a new ATSR product reader.
     *
     * @param plugin the plug-in which created this reader instance
     */
    public AtsrProductReader(AtsrProductReaderPlugIn plugin) {
        super(plugin);
    }


    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (_file != null) {
            _file.close();
        }
        super.close();
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END PF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

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
        AtsrFileFactory factory = AtsrFileFactory.getInstance();
        Object input = getInput();

        if (input instanceof String) {
            _file = factory.open((String) input);
        } else if (input instanceof File) {
            _file = factory.open((File) input);
        } else if (input instanceof ImageInputStream) {
            _file = factory.open((ImageInputStream) input, null);
        }
        return createProduct();
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
        AtsrBandReader reader = _file.getReader(destBand);

        if (reader == null) {
            throw new IOException("No band reader for band '" + destBand.getName() + "' available!");  /*I18N*/
        }

        reader.readBandData(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX, sourceStepY,
                            destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);


    }

    /**
     * Creates a product from the product file.
     */
    private Product createProduct() {
        String productName = _file.getFileName();

        productName = StringUtils.createValidName(productName, new char[]{'.', '_'}, '_');
        Product prodRet = new Product(productName, _file.getSensorType(),
                                      AtsrConstants.ATSR_SCENE_RASTER_WIDTH,
                                      AtsrConstants.ATSR_SCENE_RASTER_HEIGHT,
                                      this);
        prodRet.setFileLocation(_file.getFile());

        // add all metadata if required
        // ----------------------------
        if (!isMetadataIgnored()) {
            // add the metadata
            addMetadata(prodRet);

            // add the tie point grids
            addTiePointGrids(prodRet);

            addGeoCoding(prodRet);
        }

        // add the bands to the product
        addBandsToProduct(prodRet);
        addActiveFireBitmaskDefs(prodRet);

        return prodRet;
    }

    private void addActiveFireBitmaskDefs(Product prodRet) {
        // add bitmasks for ATSR active fires, see http://dup.esrin.esa.it/ionia/wfa/algorithm.asp
        final String nadirBand = AtsrGBTConstants.NADIR_370_BT_NAME;
        final String fwardBand = AtsrGBTConstants.FORWARD_370_BT_NAME;

        if (prodRet.containsBand(nadirBand)) {
            prodRet.addMask("fire_nadir_1", "ATSR active fire (ALGO1)", nadirBand + " > 312.0", Color.RED, 0.5f);
            prodRet.addMask("fire_nadir_2", "ATSR active fire (ALGO2)", nadirBand + " > 308.0", Color.RED.darker(), 0.5f);
        }
        if (prodRet.containsBand(fwardBand)) {
            prodRet.addMask("fire_fward_1", "ATSR active fire (ALGO1)", fwardBand + " > 312.0", Color.RED, 0.5f);
            prodRet.addMask("fire_fward_2", "ATSR active fire (ALGO2)", fwardBand + " > 308.0", Color.RED.darker(), 0.5f);
        }
    }

    /**
     * Adds all tie point grids the the product
     */
    private void addTiePointGrids(Product prodRet) {
        for (int n = 0; n < _file.getNumTiePointGrids(); n++) {
            prodRet.addTiePointGrid(_file.getTiePointGridAt(n));
        }
    }

    /**
     * Adds the metadat to the product.
     */
    private void addMetadata(Product prodRet) {
        _file.getMetadata(prodRet.getMetadataRoot());
    }

    /**
     * Adds the geocoding to the product
     */
    private void addGeoCoding(Product prodRet) {
        prodRet.setGeoCoding(_file.getGeoCoding());
    }

    /**
     * Adds all geophysical bands to the product
     */
    private void addBandsToProduct(Product prodRet) {
        Band band;
        FlagCoding flagCoding;
        for (int n = 0; n < _file.getNumBands(); n++) {
            band = _file.getBandAt(n);
            prodRet.addBand(band);

            flagCoding = band.getFlagCoding();
            if (flagCoding != null) {
                prodRet.getFlagCodingGroup().add(flagCoding);
            }
        }
    }
}
