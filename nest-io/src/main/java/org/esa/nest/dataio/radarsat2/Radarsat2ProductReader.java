/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.radarsat2;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.esa.nest.dataio.imageio.ImageIOFile;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.XMLSupport;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;

/**
 * The product reader for Radarsat2 products.
 *
 */
public class Radarsat2ProductReader extends AbstractProductReader {

    protected Radarsat2ProductDirectory dataDir = null;

    private static final String lutsigma = "lutSigma";
    private static final String lutgamma = "lutGamma";
    private static final String lutbeta = "lutBeta";

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Radarsat2ProductReader(final ProductReaderPlugIn readerPlugIn) {
       super(readerPlugIn);
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
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {

        Product product;
        try {
            final File fileFromInput = ReaderUtils.getFileFromInput(getInput());
            dataDir = createDirectory(fileFromInput);
            dataDir.readProductDirectory();
            product = dataDir.createProduct();
            addCalibrationLUT(product, fileFromInput.getParentFile());
            product.getGcpGroup();
            product.setFileLocation(fileFromInput);
            product.setProductReader(this);
            product.setModified(false);
        } catch (Exception e) {
            Debug.trace(e.toString());
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }

        return product;
    }

    protected Radarsat2ProductDirectory createDirectory(final File fileFromInput) {
        return new Radarsat2ProductDirectory(fileFromInput, fileFromInput.getParentFile());
    }

    /**
     * Read the LUT for use in calibration
     * @param product the target product
     * @param folder the folder containing the input
     * @throws IOException if can't read lut
     */
    private static void addCalibrationLUT(final Product product, final File folder) throws IOException {
        final File sigmaLUT = new File(folder, lutsigma+".xml");
        final File gammaLUT = new File(folder, lutgamma+".xml");
        final File betaLUT = new File(folder, lutbeta+".xml");

        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);

        readCalibrationLUT(sigmaLUT, lutsigma, origProdRoot);
        readCalibrationLUT(gammaLUT, lutgamma, origProdRoot);
        readCalibrationLUT(betaLUT, lutbeta, origProdRoot);
    }

    private static void readCalibrationLUT(final File file, final String lutName, final MetadataElement root) throws IOException {
        if(!file.exists())
            return;
        final org.jdom.Document xmlDoc = XMLSupport.LoadXML(file.getAbsolutePath());
        final Element rootElement = xmlDoc.getRootElement();

        final Element offsetElem = rootElement.getChild("offset");
        final double offset = Double.parseDouble(offsetElem.getValue());

        final Element gainsElem = rootElement.getChild("gains");
        final double[] gainsArray = StringUtils.toDoubleArray(gainsElem.getValue().trim(), " ");

        final MetadataElement lut = new MetadataElement(lutName);
        root.addElement(lut);

        final MetadataAttribute offsetAttrib = new MetadataAttribute("offset", ProductData.TYPE_FLOAT64);
        offsetAttrib.getData().setElemDouble(offset);
        lut.addAttribute(offsetAttrib);

        final MetadataAttribute gainsAttrib = new MetadataAttribute("gains", ProductData.TYPE_FLOAT64, gainsArray.length);
        gainsAttrib.getData().setElems(gainsArray);
        lut.addAttribute(gainsAttrib);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        final ImageIOFile.BandInfo bandInfo = dataDir.getBandInfo(destBand);
        if(bandInfo != null && bandInfo.img != null) {
            bandInfo.img.readImageIORasterBand(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceStepX, sourceStepY,
                                                destBuffer, destOffsetX, destOffsetY, destWidth, destHeight,
                                                bandInfo.imageID, bandInfo.bandSampleOffset);
        }
    }
}