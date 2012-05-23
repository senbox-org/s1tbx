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
package org.esa.beam.dataio.modis.productdb;

import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.CsvReader;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

//@todo 2 tb/tb implement reading of valid range attribute

public class ModisProductDb {

    private static final String DB_PATH = "/org/esa/beam/resources/modisdb";
    public static final int EXP_NUM_SDS_DEFAULT_RECORD = 9;
    public static final int EXP_NUM_SDS_SPECTRAL_RECORD = 12;
    private static final int EXP_NUM_GEO_RECORDS_MIN = 2;
    private static final int EXP_NUM_GEO_RECORDS_MAX = 3;
    private static final int EXP_NUM_FLIP_RECORDS = 2;
    private static final int EXP_NUM_TIEP_RECORDS = 5;

    private HashMap<String, String> _productTypes = null;
    private HashMap _productDescriptions = null;
    private Logger logger;
    private static final String META_KEY = "META";
    private static final String SDS_KEY = "SDS";
    private static final String GEO_KEY = "GEO";
    private static final String FLIP_KEY = "FLIP";
    private static final String TIEPOINT_KEY = "TIEP";

    /**
     * Retrieves the one and only instance of this class.
     *
     * @return the instance of this class
     */
    public static ModisProductDb getInstance() {
        return Holder.instance;
    }

    /**
     * Checks whether the given product type is suported by the reader - or not.
     *
     * @param typeString the product type
     * @return <code>true</code> if the given product type is suported, otherwise <code>false</code>.
     */
    public boolean isSupportedProduct(final String typeString) throws ProductIOException {
        ensureSupportedProductTypes();
        return _productTypes.get(typeString) != null;
    }

    public String[] getSupportetProductTypes() throws ProductIOException {
        ensureSupportedProductTypes();
        final Set<String> keySet = _productTypes.keySet();
        final String[] result = new String[keySet.size()];
        return keySet.toArray(result);
    }

    /**
     * Retrieves the names of the bands for the given product type.
     *
     * @param prodType the product type string
     * @return the names of the bands.
     */
    public String[] getBandNames(final String prodType) {
        String[] bandNames = null;

        try {
            final ModisProductDescription prod = getProductDescription(prodType);
            bandNames = prod.getBandNames();
        } catch (IOException e) {
            logger.severe("Unable to retrieve the band names for product of type '" + prodType + "'.");
        }

        return bandNames;
    }

    /**
     * Retrieves the band description for the given product type and band name
     *
     * @param prodType
     * @param bandName
     * @return the band description
     */
    public ModisBandDescription getBandDescription(final String prodType, final String bandName) {
        ModisBandDescription bandDesc = null;

        try {
            final ModisProductDescription prod = getProductDescription(prodType);
            bandDesc = prod.getBandDescription(bandName);
        } catch (IOException e) {
            logger.severe(
                    "Unable to retrieve information for band '" + bandName + "' of product type '" + prodType + "'.");
        }

        return bandDesc;
    }

    /**
     * Retrieves the names of the tie point grids for the given product type
     *
     * @param prodType
     * @return the names of the tie point grids
     */
    public String[] getTiePointNames(final String prodType) {
        String[] tpNames = null;

        try {
            final ModisProductDescription prod = getProductDescription(prodType);
            tpNames = prod.getTiePointNames();
        } catch (IOException e) {
            logger.severe("Unable to retrieve tie point names for product type '" + prodType + "'.");
        }

        return tpNames;
    }

    /**
     * Retrieves the tie point description for the given product type and tie point grid name
     *
     * @param prodType the product type
     * @param tpName   the name of the tie point grid
     * @return the tie point description
     */
    public ModisTiePointDescription getTiePointDescription(final String prodType, final String tpName) {
        ModisTiePointDescription tpDesc = null;

        try {
            final ModisProductDescription prod = getProductDescription(prodType);
            tpDesc = prod.getTiePointDescription(tpName);
        } catch (IOException e) {
            logger.severe(
                    "Unable to retrieve description for tie point grid '" + tpName + "' of product type '" + prodType + "'.");
        }

        return tpDesc;
    }

    /**
     * Retrieves the geolocation information for the given product type
     *
     * @param prodType
     * @return the geolocation information
     */
    public String[] getGeolocationInformation(final String prodType) {
        String[] strRet = null;

        try {
            final ModisProductDescription prod = getProductDescription(prodType);
            strRet = prod.getGeolocationDatasetNames();
        } catch (IOException e) {
            logger.severe("Unable to retrieve geolocation information for product type '" + prodType + "'.");
        }

        return strRet;
    }

    /**
     * Retrieves whether the product must be flipped top down or not
     *
     * @param prodType
     * @return <code>true</code> if the product must be flipped top down, otherwise <code>false</code>.
     */
    public boolean mustFlip(final String prodType) {
        boolean bRet = false;

        try {
            final ModisProductDescription prod = getProductDescription(prodType);
            bRet = prod.mustFlipTopDown();
        } catch (IOException e) {
            logger.severe("Unable to retrieve flipping information for product type '" + prodType + "'.");
        }

        return bRet;
    }

    ///////////////////////////////////////////////////////////////////////////
    ////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Singleton - private construction
     */
    private ModisProductDb() {
        _productDescriptions = new HashMap();
        logger = BeamLogManager.getSystemLogger();
    }

    /**
     * ensure that the list of supported product types is loaded
     */
    private void ensureSupportedProductTypes() throws ProductIOException {
        if (_productTypes == null) {
            try {
                loadProductTypes();
            } catch (IOException e) {
                throw new ProductIOException(e.getMessage());
            }
        }
    }

    /**
     * Loads the list of supported product types.
     */
    private void loadProductTypes() throws IOException {
        _productTypes = new HashMap<String, String>();

        final CsvReader reader = getCsvReader("products.dd");

        for (String[] records = reader.readRecord(); records != null; records = reader.readRecord()) {
            if (records.length == 2) {
                _productTypes.put(records[0], records[1]);
            } else {
                logger.severe(
                        "Invalid number of records in MODISDB - please check the resources for correctness.");
            }
        }

        reader.close();
    }

    /**
     * Retrieves the product description fo the given product type
     *
     * @param prodType the product type
     * @return the product description, never null
     */
    public ModisProductDescription getProductDescription(final String prodType) throws MalformedURLException,
            IOException {
        Guardian.assertNotNull("prodType", prodType);
        ModisProductDescription description = (ModisProductDescription) _productDescriptions.get(prodType);
        if (description == null) {
            description = loadProductDescription(prodType);
        }
        return description;
    }

    /**
     * Loads the product description from the associated disc file - if it exists
     *
     * @param prodType
     * @return never null
     */
    private ModisProductDescription loadProductDescription(final String prodType) throws MalformedURLException,
            IOException {
        ensureSupportedProductTypes();

        final ModisProductDescription description = new ModisProductDescription();
        final String productFile = _productTypes.get(prodType);
        final CsvReader reader = getCsvReader(productFile);

        String[] records;
        while ((records = reader.readRecord()) != null) {
            // filter out unused fields
            for (int n = 0; n < records.length; n++) {
                if (records[n].equalsIgnoreCase("*")) {
                    records[n] = null;
                }
            }
            if (records[0].equalsIgnoreCase(META_KEY)) {
                // @todo 4 tb/tb - implement
            } else if (records[0].equalsIgnoreCase(SDS_KEY)) {
                if (records.length == ModisProductDb.EXP_NUM_SDS_SPECTRAL_RECORD) {
                    description.addBand(
                            records[1], records[2], records[3], records[4],
                            records[5], records[6], records[7], records[8],
                            records[9], records[10], records[11]);
                } else if (records.length == ModisProductDb.EXP_NUM_SDS_DEFAULT_RECORD) {
                    description.addBand(
                            records[1], records[2], records[3], records[4],
                            records[5], records[6], records[7], records[8]);
                } else {
                    logger.severe("Invalid number of records in SDS description for product type '" + prodType + "'.");
                }
            } else if (records[0].equalsIgnoreCase(GEO_KEY)) {
                if ((records.length < EXP_NUM_GEO_RECORDS_MIN) || (records.length > EXP_NUM_GEO_RECORDS_MAX)) {
                    logger.severe("Invalid number of records in GEO description for product type '" + prodType + "'.");
                    continue;
                }
                if (records.length == EXP_NUM_GEO_RECORDS_MAX) {
                    description.setGeolocationDatasetNames(records[1], records[2]);
                } else {
                    description.setExternalGeolocationPattern(records[1]);
                }
            } else if (records[0].equalsIgnoreCase(FLIP_KEY)) {
                if (records.length == EXP_NUM_FLIP_RECORDS) {
                    description.setTopDownFlip(Boolean.valueOf(records[1]));
                } else {
                    logger.severe(
                            "Invalid number of records in FLIP description for product type '" + prodType + "'.");
                }
            } else if (records[0].equalsIgnoreCase(TIEPOINT_KEY)) {
                if (records.length == EXP_NUM_TIEP_RECORDS) {
                    description.addTiePointGrid(new ModisTiePointDescription(
                            records[1], records[2], records[3], records[4]));
                } else {
                    logger.severe(
                            "Invalid number of records in TIEP description for product type '" + prodType + "'.");
                }
            }
        }

        reader.close();

        _productDescriptions.put(prodType, description);

        return description;
    }

    /**
     * Retrieves a csv reader opened at the first line of the file pased in
     *
     * @param filename
     * @return
     * @throws java.io.IOException
     */
    private CsvReader getCsvReader(final String filename) throws IOException {
        final URL dbResource = getDatabaseResource(filename);

        final CsvReader reader;
        try {
            reader = new CsvReader(new InputStreamReader(dbResource.openStream()),
                    ModisConstants.FIELD_SEPARATORS, true, "#");
        } catch (MalformedURLException e) {
            throw new IOException(e.getMessage());
        }

        return reader;
    }

    /**
     * Retrievs a resource file from the modis db
     *
     * @param resourcePath
     * @return
     * @throws IOException
     */
    private URL getDatabaseResource(final String resourcePath) throws IOException {
        final String databasePath = DB_PATH + "/" + resourcePath;
        Debug.trace("MODISDB: searching for resource file '" + databasePath + "'"); /*I18N*/

        final URL url = ModisProductDb.class.getResource(databasePath);
        if (url == null) {
            throw new IOException("MODISDB resource not found: missing file " + url); /*I18N*/
        }

        return url;
    }

    // Initialization on demand holder idiom
    private static class Holder {
        private static final ModisProductDb instance = new ModisProductDb();
    }
}
