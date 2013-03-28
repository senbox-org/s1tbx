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

package org.esa.beam.processor.binning;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.algorithm.Algorithm;
import org.esa.beam.processor.binning.algorithm.AlgorithmCreator;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.processor.binning.database.BinLocator;
import org.esa.beam.processor.binning.database.LatLonBinLocator;
import org.esa.beam.processor.binning.database.SeaWiFSBinLocator;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3Context {

    private final static String _PROPERTY_KEY_BAND_NAME = L3Constants.BAND_NAME_PARAMETER_NAME;
    private final static String _PROPERTY_KEY_BITMASK = L3Constants.BITMASK_PARAMETER_NAME;
    private final static String _PROPERTY_KEY_ALGORITHM_TYPE = L3Constants.ALGORITHM_PARAMETER_NAME;
    private final static String _PROPERTY_KEY_ALGORITHM_PARAMETERS = L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME;

    private float _gridCellSize;
    private File _databaseDir;
    private String _storageType;
    private String _resamplingType;
    private float _latMin;
    private float _latMax;
    private float _lonMin;
    private float _lonMax;

    private List _processedProducts;
    private BinLocator locator = null;
    private final ArrayList _bandDefinitionList;
    private AlgorithmCreator algoCreator;

    public L3Context() {
        _processedProducts = new LinkedList();
        _bandDefinitionList = new ArrayList();
    }

    /**
     * Sets the algorithm creator to be used.
     *
     * @param creator
     */
    public void setAlgorithmCreator(AlgorithmCreator creator) {
        algoCreator = creator;
    }

    public void setMainParameter(final File databaseDir,
                                 final String resamplingType, final float gridCellSize) {
        _databaseDir = databaseDir;
        _gridCellSize = gridCellSize;
        _resamplingType = resamplingType;
    }

    public void save() throws IOException {
        Properties contextProps = new Properties();

        contextProps.put(BinDatabaseConstants.STORAGE_TYPE_KEY, _storageType);
        contextProps.put(BinDatabaseConstants.CELL_SIZE_KEY, String.valueOf(_gridCellSize));
        contextProps.put(BinDatabaseConstants.RESAMPLING_TYPE_KEY, _resamplingType);

        // the band definitions
        for (int idx = 0; idx < _bandDefinitionList.size(); idx++) {
            final BandDefinition bandDef = (BandDefinition) _bandDefinitionList.get(idx);
            contextProps.put(_PROPERTY_KEY_BAND_NAME + "." + idx, bandDef.getBandName());
            contextProps.put(_PROPERTY_KEY_BITMASK + "." + idx, bandDef.getBitmaskExp());
            final Algorithm algo = bandDef.getAlgorithm();
            contextProps.put(_PROPERTY_KEY_ALGORITHM_TYPE + "." + idx, algo.getTypeString());
            contextProps.put(_PROPERTY_KEY_ALGORITHM_PARAMETERS + "." + idx, bandDef.getAlgorithmParams());
        }

        // processed products
        contextProps.put(BinDatabaseConstants.PRODUCT_COUNT_KEY, String.valueOf(_processedProducts.size()));
        for (int i = 0; i < _processedProducts.size(); i++) {
            final String productName = (String) _processedProducts.get(i);
            contextProps.put(BinDatabaseConstants.PROCESSED_PRODUCT_BASE_KEY + "." + i, productName);
        }

        // the border coordinates
        contextProps.put(BinDatabaseConstants.LAT_MIN_KEY, String.valueOf(_latMin));
        contextProps.put(BinDatabaseConstants.LAT_MAX_KEY, String.valueOf(_latMax));
        contextProps.put(BinDatabaseConstants.LON_MIN_KEY, String.valueOf(_lonMin));
        contextProps.put(BinDatabaseConstants.LON_MAX_KEY, String.valueOf(_lonMax));

        // and out to disk
        saveProperties(contextProps, BinDatabaseConstants.CONTEXT_PROPERTIES_FILE);
    }

    public void load(File dbDir) throws IOException, ProcessorException {
        _databaseDir = dbDir;
        Properties props = loadProperties(BinDatabaseConstants.CONTEXT_PROPERTIES_FILE);

        _storageType = props.getProperty(BinDatabaseConstants.STORAGE_TYPE_KEY);
        if (_storageType == null) {
            throw new ProcessorException("Corrupted database. Unable to load storage type parameter");
        }

        String cellWidthString = props.getProperty(BinDatabaseConstants.CELL_SIZE_KEY);
        if (cellWidthString == null) {
            throw new ProcessorException("Corrupted database. Unable to load cell size parameter");
        }
        _gridCellSize = Float.parseFloat(cellWidthString);

        _resamplingType = props.getProperty(BinDatabaseConstants.RESAMPLING_TYPE_KEY);
        if (_resamplingType == null) {
            throw new ProcessorException("Corrupted database. Unable to load composite type parameter");
        }

        _bandDefinitionList.clear();
        int bandIndex = 0;
        while (props.getProperty(_PROPERTY_KEY_BAND_NAME + "." + Integer.toString(bandIndex)) != null) {
            final String postfix = "." + Integer.toString(bandIndex);
            final String bandName = props.getProperty(_PROPERTY_KEY_BAND_NAME + postfix);
            final String bitmask = props.getProperty(_PROPERTY_KEY_BITMASK + postfix);
            if (bitmask == null) {
                throw new ProcessorException(
                        "Corrupted database. Unable to load bitmask parameter at index " + bandIndex);
            }
            final String algoName = props.getProperty(_PROPERTY_KEY_ALGORITHM_TYPE + postfix);
            if (algoName == null) {
                throw new ProcessorException("Corrupted database. Unable to load algorithm type at index " + bandIndex);
            }
            final String algoParams = props.getProperty(_PROPERTY_KEY_ALGORITHM_PARAMETERS + postfix);
            if (algoParams == null) {
                throw new ProcessorException(
                        "Corrupted database. Unable to load algorithm parameters at index " + bandIndex);
            }
            addBandDefinition(bandName, bitmask, algoName, algoParams);
            bandIndex++;
        }

        String processedProdString = props.getProperty(BinDatabaseConstants.PRODUCT_COUNT_KEY);
        int processedProds = 0;
        if (processedProdString != null) {
            processedProds = Integer.parseInt(processedProdString);
        }
        for (int i = 0; i < processedProds; i++) {
            String product = props.getProperty(BinDatabaseConstants.PROCESSED_PRODUCT_BASE_KEY + "." + i);
            if (product == null) {
                throw new ProcessorException("Corrupted database. Unable to load processed products(" + i + ")");
            }
            _processedProducts.add(product);
        }

        String latLonString = props.getProperty(BinDatabaseConstants.LAT_MIN_KEY);
        if (latLonString == null) {
            throw new ProcessorException("Corrupted database. Unable to load latitude minimum parameter");
        }
        _latMin = Float.parseFloat(latLonString);

        latLonString = props.getProperty(BinDatabaseConstants.LAT_MAX_KEY);
        if (latLonString == null) {
            throw new ProcessorException("Corrupted database. Unable to load latitude maximum parameter");
        }
        _latMax = Float.parseFloat(latLonString);

        latLonString = props.getProperty(BinDatabaseConstants.LON_MIN_KEY);
        if (latLonString == null) {
            throw new ProcessorException("Corrupted database. Unable to load longitude minimum parameter");
        }
        _lonMin = Float.parseFloat(latLonString);

        latLonString = props.getProperty(BinDatabaseConstants.LON_MAX_KEY);
        if (latLonString == null) {
            throw new ProcessorException("Corrupted database. Unable to load longitude maximum parameter");
        }
        _lonMax = Float.parseFloat(latLonString);
    }

    public void saveProperties(Properties properties, String fileName) throws IOException {
        File propFile = new File(_databaseDir, fileName);
        propFile.createNewFile();
        FileOutputStream outStream = new FileOutputStream(propFile);
        properties.store(outStream, null);
        outStream.close();
    }

    public Properties loadProperties(String fileName) throws IOException {
        File propFile = new File(_databaseDir, fileName);
        Properties props = new Properties();
        FileInputStream inStream = new FileInputStream(propFile);
        props.load(inStream);
        inStream.close();
        return props;
    }

    public void deleteProperties(String fileName) {
        File propFile = new File(_databaseDir, fileName);
        propFile.delete();
    }

    public int getNumBands() {
        return _bandDefinitionList.size();
    }

    public File getDatabaseDir() {
        return _databaseDir;
    }

    public String getResamplingType() {
        return _resamplingType;
    }

    public float getGridCellSize() {
        return _gridCellSize;
    }

    public Rectangle2D getBorder() {
        return new Rectangle2D.Float(_lonMin, _latMin, _lonMax - _lonMin, _latMax - _latMin);
    }

    /**
     * Returns an array that contains for every inputBand the number of
     * variables that are required for accumulating.
     *
     * @return an array
     */
    public int[] getNumberOfAccumulatingVarsPerBand() {
        final int[] numberOfAccumulatingVarsPerBand = new int[getNumBands()];
        for (int bandIndex = 0; bandIndex < _bandDefinitionList.size(); bandIndex++) {
            final BandDefinition bandDef = (BandDefinition) _bandDefinitionList.get(bandIndex);
            numberOfAccumulatingVarsPerBand[bandIndex] = bandDef.getAlgorithm().getNumberOfAccumulatedVariables();
        }
        return numberOfAccumulatingVarsPerBand;
    }

    /**
     * Returns an array that contains for every inputBand the number of
     * variables that are required for interpretation.
     *
     * @return an array
     */
    public int[] getNumberOfInterpretedVarsPerBand() {
        final int[] numberOfInterpreredVarsPerBand = new int[getNumBands()];
        for (int bandIndex = 0; bandIndex < _bandDefinitionList.size(); bandIndex++) {
            final BandDefinition bandDef = (BandDefinition) _bandDefinitionList.get(bandIndex);
            numberOfInterpreredVarsPerBand[bandIndex] = bandDef.getAlgorithm().getNumberOfInterpretedVariables();
        }
        return numberOfInterpreredVarsPerBand;
    }

    public BinLocator getLocator() {
        if (locator == null) {
            if (_resamplingType.equals(L3Constants.RESAMPLING_TYPE_VALUE_FLUX_CONSERVING)) {
                Rectangle border = new Rectangle((int) _lonMin, (int) _latMin, (int) (_lonMax - _lonMin),
                        (int) (_latMax - _latMin));
                locator = new LatLonBinLocator(border, Math.round(_gridCellSize));
            } else {
                locator = new SeaWiFSBinLocator(_gridCellSize);
            }
        }
        return locator;
    }

    public boolean algorithmNeedsFinishSpatial() {
        boolean needsFinishing = false;
        for (int bandIndex = 0; bandIndex < getNumBands(); bandIndex++) {
            needsFinishing |= ((BandDefinition) _bandDefinitionList.get(bandIndex)).getAlgorithm().needsFinishSpatial();
        }
        return needsFinishing;
    }

    public boolean algorithmNeedsInterpretation() {
        boolean needsFinishing = false;
        for (int bandIndex = 0; bandIndex < getNumBands(); bandIndex++) {
            needsFinishing |= ((BandDefinition) _bandDefinitionList.get(bandIndex)).getAlgorithm().needsInterpretation();
        }
        return needsFinishing;
    }

    /**
     * Add the given productName to the processed products.
     *
     * @param productName the name of the product
     */
    public void addProductProcessed(String productName) {
        _processedProducts.add(productName);
    }

    /**
     * Returns an array which contains the names of all processed products.
     *
     * @return an array
     */
    public String[] getProcessedProducts() {
        return (String[]) _processedProducts.toArray(new String[0]);
    }

    public void setBorder(float latMin, float latMax, float lonMin, float lonMax) {
        _latMin = latMin;
        _latMax = latMax;
        _lonMin = lonMin;
        _lonMax = lonMax;
    }

    public void addBandDefinition(final String bandName, final String bitmaskExp,
                                  final String algorithmName, final String algorithmParams) throws ProcessorException {
        final BandDefinition bandDefinition = new BandDefinition(bandName, bitmaskExp, algorithmName, algorithmParams);
        _bandDefinitionList.add(bandDefinition);
    }

    public BandDefinition[] getBandDefinitions() {
        return (BandDefinition[]) _bandDefinitionList.toArray(new BandDefinition[_bandDefinitionList.size()]);
    }

    public class BandDefinition {

        private final String _bandName;
        private final String _bitmaskExp;
        private final String _algorithmParams;
        private final Algorithm _algorithm;

        public BandDefinition(String bandName, String bitmaskExp, String algorithmName, String algorithmParams) throws ProcessorException {
            _bandName = bandName;
            _bitmaskExp = bitmaskExp;
            _algorithmParams = algorithmParams;
            _algorithm = algoCreator.getAlgorithm(algorithmName);
            _algorithm.init(algorithmParams);
        }

        public String getBandName() {
            return _bandName;
        }

        public String getBitmaskExp() {
            return _bitmaskExp;
        }

        public Algorithm getAlgorithm() {
            return _algorithm;
        }

        public String getAlgorithmParams() {
            return _algorithmParams;
        }
    }

    public String getStorageType() {
        return _storageType;
    }

    public void setStorageType(String storageType) {
        _storageType = storageType;
    }
}
