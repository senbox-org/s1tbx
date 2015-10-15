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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.StringUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * The <code>AsarProductFile</code> is a specialization of the abstract <code>ProductFile</code> class for ENVISAT
 * ASAR data products.
 */
public class AsarXCAProductFile extends ForwardingProductFile {
    private final static String GADS_NAME = "auxiliary_data";

    enum IODD {
        VERSION_UNKNOWN, ASAR_4A
    }

    /**
     * The IODD version number.
     */
    private IODD _ioddVersion = IODD.VERSION_UNKNOWN;

    /**
     * Product type suffix for IODD-4A backward compatibility
     */
    private static final String IODD4A_SUFFIX = "_IODD_4A";

    /**
     * Constructs a <code>MerisProductFile</code> for the given seekable data input stream. Attaches the
     * <code>LogSink</code> passed in to the object created. The <code>LogSink</code> can might be null.
     *
     * @param file            the abstract file path representation.
     * @param dataInputStream the seekable data input stream which will be used to read data from the product file.
     * @throws java.io.IOException if an I/O error occurs
     */
    protected AsarXCAProductFile(File file, ImageInputStream dataInputStream) throws IOException {
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
        return GADS_NAME;
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
        _ioddVersion = IODD.VERSION_UNKNOWN;
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

        DSD[] mdsDsds = getValidDSDs(EnvisatConstants.DS_TYPE_GLOBAL_ANNOTATION);
        if (mdsDsds.length == 0) {
            throw new IllegalFileFormatException("no valid global annotation datasets found in this ASAR product");
        }

        setIODDVersion();
    }

    @Override
    public String getProductType() {
        return getProductId().substring(0, EnvisatConstants.PRODUCT_TYPE_STRLEN).toUpperCase();
    }

    @Override
    public boolean isValidDatasetName(String name) throws IOException {
        String[] datasetNames = getValidDatasetNames();
        return name.equalsIgnoreCase(getGADSName()) || StringUtils.containsIgnoreCase(datasetNames, name);
    }

    IODD getIODDVersion() {
        if (_ioddVersion == IODD.VERSION_UNKNOWN) {
            setIODDVersion();
        }
        return _ioddVersion;
    }

    /**
     * Sets the IODD version which is an indicator for the product format.
     * <p>
     * REF_DOC from version 3H on end with 3H, 3K, 4A, 4B
     * Software can be at least ASAR, NORUT, KSPT_L1B
     * <p>
     * 3H ASAR/3.05, ASAR/3.06, ASAR/3.08
     * 4A ASAR/4.01, ASAR/4.02, ASAR/4.04
     * 4B ASAR/4.05
     */
    private void setIODDVersion() {

        Header mph = getMPH();
        try {

            String refDoc = mph.getParamString("REF_DOC").toUpperCase().trim();
            if (refDoc.endsWith("4A") || refDoc.endsWith("4/A")) {
                _ioddVersion = IODD.ASAR_4A;
            } else {
                char issueCh = refDoc.charAt(refDoc.length() - 2);
                if (Character.isDigit(issueCh)) {
                    int issue = Character.getNumericValue(issueCh);
                    if (issue >= 4) {
                        _ioddVersion = IODD.ASAR_4A;                             // catch future versions
                    }
                }
            }
        } catch (HeaderEntryNotFoundException e) {
            _ioddVersion = IODD.VERSION_UNKNOWN;
        }
    }

    /**
     * Gets the product type string as used within the DDDB, e.g. "MER_FR__1P_IODD5". This implementation considers
     * format changes in IODD 6.
     *
     * @return the product type string
     */
    @Override
    protected String getDddbProductType() {
        final String productType = getDddbProductTypeReplacement(getProductType(), getIODDVersion());
        return productType != null ? productType : super.getDddbProductType();
    }

    static String getDddbProductTypeReplacement(final String productType, final IODD ioddVersion) {
        String resource = productType;
        if (ioddVersion == IODD.ASAR_4A) {
            if (productDDExists(productType + IODD4A_SUFFIX))
                resource = productType + IODD4A_SUFFIX;
        }
        return resource;
    }

    private static boolean productDDExists(String productType) {
        String productInfoFilePath = "products/" + productType + ".dd";
        return DDDB.databaseResourceExists(productInfoFilePath);
    }

    @Override
    public void setInvalidPixelExpression(Band band) {
        band.setNoDataValueUsed(false);
        band.setNoDataValue(0);
    }

    /**
     * Allow the productFile to add any other metadata not defined in dddb
     *
     * @param product the product
     * @throws IOException if reading from files
     */
    @Override
    protected void addCustomMetadata(Product product) throws IOException {

        MetadataElement root = product.getMetadataRoot();
        Record gads = getGADS();

        MetadataElement elem = EnvisatProductReader.createMetadataGroup("XCA", gads);
        root.addElement(elem);

    }
}
