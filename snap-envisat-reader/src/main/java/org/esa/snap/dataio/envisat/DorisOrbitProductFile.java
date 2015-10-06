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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * The <code>AsarProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * ASAR data products.
 */
public class DorisOrbitProductFile extends ProductFile {

    private final static String ORBIT_RECORD_NAME = "DORIS_Orbit";

    /**
     * Constructs a <code>MerisProductFile</code> for the given seekable data input stream. Attaches the
     * <code>LogSink</code> passed in to the object created. The <code>LogSink</code> can might be null.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @throws java.io.IOException if an I/O error occurs
     */
    protected DorisOrbitProductFile(File file, ImageInputStream dataInputStream) throws IOException {
        super(file, dataInputStream);
    }

    /**
     * Returns the name of the GADS for this ENVISAT product file.
     *
     * @return the GADS name "VISIBLE_CALIB_COEFS_GADS", or <code>null</code> if this product file does not have a
     *         GADS.
     */
    @Override
    public String getGADSName() {
        return null;
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p> When this method is called, the <code>getMPH()</code> method returns a non-null value.
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     * @throws java.io.IOException if a header format error was detected or if an I/O error occurs
     */
    @Override
    protected void postProcessMPH(Map parameters) throws IOException {
    }

    /**
     * This method is called after the main product header has been read in successfully.
     * <p> Sub-classes should set product specific parameters in the <code>parameters</code> argument. The parameters
     * can be referenced in DDDB in order to implement dynamic field length, such as 'LINE_WIDTH'.
     * <p> When this method is called, the <code>getMPH()</code> method returns a non-null value.
     * <p> The default implementation is empty.
     *
     * @param parameters product specific parameters (possibly referenced within in the DDDB
     */
    @Override
    protected void postProcessSPH(Map parameters) throws IOException {

        final DSD dsd = getDSDAt(0);
        if(dsd != null) {
            final int num = dsd.getNumRecords();
            parameters.put("NUM_DSR", num);
        }
    }

    public Record readOrbitData() throws IOException
    {
        return getRecordReader(ORBIT_RECORD_NAME).readRecord();
    }

    @Override
    public String getProductType() {
        return getProductId().substring(0, EnvisatConstants.PRODUCT_TYPE_STRLEN).toUpperCase();
    }

    @Override
    public boolean isValidDatasetName(String name) throws IOException {
        final String[] datasetNames = getValidDatasetNames();
        return name.equalsIgnoreCase(getGADSName()) || StringUtils.containsIgnoreCase(datasetNames, name);
    }


    @Override
    public void setInvalidPixelExpression(Band band) {
        band.setNoDataValueUsed(false);
        band.setNoDataValue(0);
    }

    /**
     * Returns an array containing the center wavelengths for all bands in the AATSR product (in nm).
     */
    @Override
    public float[] getSpectralBandWavelengths() {
        return null;
    }

    /**
     * Returns an array containing the bandwidth for each band in nm.
     */
    @Override
    public float[] getSpectralBandBandwidths() {
        return null;
    }

    /**
     * Returns an array containing the solar spectral flux for each band.
     */
    @Override
    public float[] getSpectralBandSolarFluxes() {
        return null;
    }

    /**
     * Returns a new default set of mask definitions for this product file.
     *
     * @param dsName the name of the flag dataset
     * @return a new default set, an empty array if no default set is given for this product type, never
     *         <code>null</code>.
     */
    @Override
    public Mask[] createDefaultMasks(String dsName) {
        return new Mask[0];
    }

    /**
     * Gets the (sensing) start time associated with the first raster data line.
     *
     * @return the sensing start time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStartTime() {
        return null;
    }

    /**
     * Gets the (sensing) stop time associated with the first raster data line.
     *
     * @return the sensing stop time, can be null e.g. for non-swath products
     */
    @Override
    public ProductData.UTC getSceneRasterStopTime() {
        return null;
    }

    @Override
    public int getSceneRasterWidth() {
        return 0;
    }

    @Override
    public int getSceneRasterHeight() {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointGridOffsetY(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingX(int gridWidth) {
        return 0;
    }

    @Override
    public float getTiePointSubSamplingY(int gridWidth) {
        return 0;
    }

    @Override
    public boolean storesPixelsInChronologicalOrder() {
        return false;
    }

    /**
     * Allow the productFile to add any other metadata not defined in dddb
     * @param product the product
     * @throws IOException if reading from files
     */
    @Override
    protected void addCustomMetadata(Product product) throws IOException {

        final MetadataElement root = product.getMetadataRoot();
        final Record orbitRecord = readOrbitData();

        final MetadataElement elem = EnvisatProductReader.createMetadataGroup("Orbit Vectors", orbitRecord);
        root.addElement(elem);
    }
}
