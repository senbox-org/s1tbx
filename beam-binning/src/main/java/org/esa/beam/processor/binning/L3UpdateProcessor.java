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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.ParseException;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.processor.binning.algorithm.Algorithm;
import org.esa.beam.processor.binning.algorithm.AlgorithmFactory;
import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.processor.binning.database.ClippingResampler;
import org.esa.beam.processor.binning.database.SpatialBinDatabase;
import org.esa.beam.processor.binning.database.TemporalBinDatabase;
import org.esa.beam.util.ProductUtils;

import java.awt.Point;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3UpdateProcessor extends L3SubProcessor {

    protected File databaseDir;
    protected Vector<ProductRef> inputProductRefs;

    protected TemporalBinDatabase temporalDB;

    protected L3Context context;

    /**
     * Creates the object with given parent processor and logging sink.
     *
     * @param parent the parent processor running this sub-processor
     */
    public L3UpdateProcessor(L3Processor parent) {
        super(parent);
    }

    /**
     * Processes a request
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {

        try {
            // check that we have the product vectors we need and that they are cleared
            assureValidVectors();

            // retrieve parameters needed from request
            loadRequestParameter();

            loadContext();

            // load the database
            loadBinDatabase();

            // checks that all products referenced in the processing request are existing
            validateInputProductReferences();

            // an now process all of them
            processInputProducts(pm);

            // and finally close the database
            closeBinDatabase();
            storeContext();

        } catch (IOException e) {
            throw new ProcessorException("An I/O error occurred:\n" + e.getMessage(), e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Loads all parameter from the request needed for the update process
     */
    protected void loadRequestParameter() throws ProcessorException {
        Parameter param = null;
        getLogger().info(ProcessorConstants.LOG_MSG_LOAD_REQUEST);

        // database directory
        // ------------------
        param = getParameter(L3Constants.DATABASE_PARAM_NAME, L3Constants.MSG_MISSING_BINDB);
        databaseDir = (File) param.getValue();
        ensureDBLocationForLoad(databaseDir);

        // the list of input products
        // --------------------------
        Request request = getRequest();
        int numInputProds = request.getNumInputProducts();
        ProductRef prodRef = null;
        for (int n = 0; n < numInputProds; n++) {
            prodRef = request.getInputProductAt(n);
            if (prodRef == null) {
                // flag to the parent processor
                raiseErrorFlag();
                String message = L3Constants.LOG_MSG_INPUT_NOT_FOUND_1 + n + L3Constants.LOG_MSG_INPUT_NOT_FOUND_2;
                getLogger().warning(message);
                addWarningMessage(message);
                continue;
            }
            inputProductRefs.add(prodRef);
        }

        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    protected void loadContext() throws IOException,
                                        ProcessorException {
        context = new L3Context();
        context.setAlgorithmCreator(new AlgorithmFactory());
        context.load(databaseDir);
    }

    protected void storeContext() throws IOException {
        context.save();
    }

    /**
     * Validates all input product references stored in the processing request. All non existent files are excluded and
     * the list is shrinked to contain only existing files.
     */
    protected void validateInputProductReferences() {
        final int numProds = inputProductRefs.size();
        Vector<ProductRef> validRefs = new Vector<ProductRef>();
        File prodFile = null;
        ProductRef prodRef = null;

        // loop over all products, check if the files are on disk
        // ------------------------------------------------------
        for (int n = 0; n < numProds; n++) {
            prodRef = inputProductRefs.elementAt(n);
            prodFile = new File(prodRef.getFilePath());
            if (!prodFile.exists()) {
                // flag to the parent processor
                raiseErrorFlag();
                String message = L3Constants.LOG_MSG_INPUT_NOT_EXIST_1 + prodFile.toString() + L3Constants.LOG_MSG_INPUT_NOT_EXIST_2;
                getLogger().warning(message);
                addWarningMessage(message);
                continue;
            }
            validRefs.add(prodRef);
        }

        // copy all valid product references to the member vector
        // ------------------------------------------------------
        inputProductRefs.clear();
        inputProductRefs.addAll(validRefs);
    }

    /**
     * Tries to load the product referenced by the productRef passed in. Validates it in terms of the required
     * geophysical parameter (band) and the bitmask expression.
     *
     * @param ref the <code>ProductRef</code> designating the product to be loaded
     *
     * @return the loaded and validated product
     */
    protected Product loadValidatedProduct(ProductRef ref) {
        try {
            Product prod = ProductIO.readProduct(ref.getFile());
            if (prod == null) {
                handleError(ref, "Unknown type of product.");
            } else if (!productContainsBands(prod)) {
                handleError(ref, "The product does not contain all the bands expected.");
            } else if (!bitmasksAreApplicable(prod)) {
                handleError(ref, "The bitmasks are not applicable to this product");
            } else if (!productIsInArea(prod)) {
                handleError(ref, "The product does not intersect area of interest.");
            } else {
                return prod;
            }
        } catch (Exception e) {
            e.printStackTrace();
            handleError(ref, e.getMessage());
        }
        return null;
    }

    private void handleError(ProductRef ref, String messagePart2) {
        raiseErrorFlag();
        final String messagePart1 = "Unable to use product '" + ref.getFilePath() + "'. ";
        final String message = messagePart1 + messagePart2;
        getLogger().warning(message);
        addWarningMessage(message);
    }


    /*
     * This is actual L3 spatial and temporal binning routine.
     */
    protected void processInputProducts(ProgressMonitor pm) throws IOException, ProcessorException {

        // loop over all product references
        // --------------------------------
        // step is times 2 so we can trace the subprocedures
        pm.beginTask("Processing input product...", inputProductRefs.size() * 2);
        try {
            for (int i = 0; i < inputProductRefs.size(); i++) {
                ProductRef prodRef = inputProductRefs.elementAt(i);

                // get the product according - can return "null" if the product is not valid.
                // When this happens, the appropriate error message is already logged by the
                // 'loadValidatedProduct' method.
                Product product = loadValidatedProduct(prodRef);
                if (product != null) {
                    getLogger().info(L3Constants.LOG_MSG_PROCESS_PROD_1 + product.getName() +
                                     L3Constants.LOG_MSG_PROCESS_PROD_2);

                    pm.setSubTaskName("Processing input product " + product.getName());
                    final SpatialBinDatabase spatialDB = createSpatialDatabase(product);
                    ProcessorException exception = null;
                    try {
                        try {
                            spatialDB.processSpatialBinning();
                        } catch (RuntimeException e) {
                            // we catch and convert RuntimeException here, because we don't want that a single corrupt
                            // product breaks the complete processing
                            String msg = String.format("Spatial processing failed for product '%s'.",
                                                       product.getName());
                            throw new ProcessorException(msg, e);
                        }
                        if (pm.isCanceled()) {
                            setCurrentState(L3Constants.STATUS_ABORTED);
                            break;
                        }
                        pm.worked(1);

                        // perform the temporal binning
                        processTemporal(spatialDB, SubProgressMonitor.create(pm, 1));

                        // add this successfully processed product to the database
                        context.addProductProcessed(product.getName());
                    } catch (ProcessorException e) {
                        exception = e;
                        getLogger().warning(e.getMessage());
                        getLogger().info("Continuing with the next input product");
                        addWarningMessage("Unable to process the input product '" +
                                          product.getName() +
                                          "' because an exception occurred.");
                        addWarningMessage(e.getMessage());
                    } finally {
                        // delete the spatial database - not needed anymore
                        spatialDB.delete();

                        if (!isAborted() && exception == null) {
                            getLogger().info(L3Constants.LOG_MSG_PROC_SUCCESS);
                        }
                    }
                }
            }
        } finally {
            pm.done();
        }
    }

    /*
     * Processes the temporal binning
     */
    protected void processTemporal(SpatialBinDatabase spatialDB, ProgressMonitor pm) throws IOException {
        if (isAborted()) {
            return;
        }
        getLogger().info(L3Constants.LOG_MSG_TEMP_BINNING);
        final int width = spatialDB.getWidth();
        final int height = spatialDB.getHeight();
        final int rowOffset = spatialDB.getRowOffset();
        final int colOffset = spatialDB.getColOffset();
        Point rowCol = new Point();
        Bin spatialBin = spatialDB.createBin();
        Bin temporalBin = temporalDB.createBin();

        pm.beginTask(L3Constants.LOG_MSG_TEMP_BINNING, height - rowOffset);
        try {
            final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
            for (rowCol.y = rowOffset; rowCol.y < rowOffset + height; rowCol.y++) {
                for (rowCol.x = colOffset; rowCol.x < colOffset + width; rowCol.x++) {
                    spatialDB.read(rowCol, spatialBin);
                    temporalDB.read(rowCol, temporalBin);
                    for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
                        final Algorithm algorithm = bandDefs[bandIndex].getAlgorithm();
                        temporalBin.setBandIndex(bandIndex);
                        spatialBin.setBandIndex(bandIndex);
                        algorithm.accumulateTemporal(spatialBin, temporalBin);
                    }
                    temporalDB.write(rowCol, temporalBin);
                }

                // update progressbar
                pm.worked(1);
                if (pm.isCanceled()) {
                    getLogger().warning(L3Constants.LOG_MSG_PROC_CANCELED);
                    setCurrentState(L3Constants.STATUS_ABORTED);
                    break;
                }
            }
        } finally {
            // get it to disk
            temporalDB.flush();
            pm.done();
        }


        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Creates a spatial database for the current product
     *
     * @param product the <code>Product</code> that shall be spatially binned
     */
    protected SpatialBinDatabase createSpatialDatabase(Product product) {
        SpatialBinDatabase spatialDB;

        getLogger().info(L3Constants.LOG_MSG_CREATE_BIN_DB);
        if (context.getResamplingType().equals(L3Constants.RESAMPLING_TYPE_VALUE_FLUX_CONSERVING)) {
            spatialDB = new ClippingResampler(context, product, getLogger());
        } else {
            spatialDB = new SpatialBinDatabase(context, product, getLogger());
        }
        spatialDB.setNumVarsPerBand(context.getNumberOfAccumulatingVarsPerBand());
        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);

        return spatialDB;
    }

    /**
     * Trigger base class to load the bin database provided with the request file
     */
    protected void loadBinDatabase() throws IOException,
                                            ProcessorException {
        temporalDB = new TemporalBinDatabase(context, BinDatabaseConstants.TEMP_DB_NAME);
        temporalDB.setNumVarsPerBand(context.getNumberOfAccumulatingVarsPerBand());
        temporalDB.open();
    }

    protected void closeBinDatabase() throws IOException {
        getLogger().info(L3Constants.LOG_MSG_CLOSE_BINDB);
        temporalDB.close();
        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Assures that the vectors used are constructed and that the're empty.
     */
    protected void assureValidVectors() {
        if (inputProductRefs == null) {
            inputProductRefs = new Vector<ProductRef>();
        }
        inputProductRefs.clear();
    }

    /**
     * Checks whether the product contains the bands needed for binning
     */
    protected boolean productContainsBands(Product prod) {
        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
        for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
            final String bandName = bandDefs[bandIndex].getBandName();
            if (!prod.containsBand(bandName)) {
                raiseErrorFlag();
                final String message = L3Constants.LOG_MSG_INPUT_NOT_EXIST_1 + prod.getName()
                                       + L3Constants.LOG_MSG_NO_REQ_BAND + bandName + "'!";
                getLogger().warning(message);
                addWarningMessage(message);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the product can handle the bitmask expressions needed
     */
    protected boolean bitmasksAreApplicable(Product prod) {
        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
        for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
            final L3Context.BandDefinition bandDef = bandDefs[bandIndex];
            final String bitmaskExpression = bandDef.getBitmaskExp();
            if ((bitmaskExpression != null) && (bitmaskExpression.length() > 0)) {
                try {
                    prod.parseExpression(bitmaskExpression);
                } catch (ParseException e) {
                    raiseErrorFlag();
                    String message = L3Constants.LOG_MSG_INPUT_NOT_EXIST_1 + prod.getName() + L3Constants.LOG_MSG_NO_REQ_FLAG + bitmaskExpression + "'!";
                    getLogger().warning(message);
                    addWarningMessage(message);
                    return false;
                } catch (IllegalStateException e) {
                    raiseErrorFlag();
                    String message = L3Constants.LOG_MSG_INPUT_NOT_EXIST_1 + prod.getName() + L3Constants.LOG_MSG_NO_REQ_FLAG + bitmaskExpression + "'!";
                    getLogger().warning(message);
                    addWarningMessage(message);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the product is in the lat/lon range defined by the database
     */
    protected boolean productIsInArea(Product prod) {
        final Area border = new Area(context.getBorder());
        final GeneralPath[] geoBoundaryPaths = ProductUtils.createGeoBoundaryPaths(prod);

        for (int i = 0; i < geoBoundaryPaths.length; i++) {
            final Area geoBoundary = new Area(geoBoundaryPaths[i]);
            geoBoundary.intersect(border);
            if (!geoBoundary.isEmpty()) {
                return true;
            }
        }

        getLogger().warning(L3Constants.LOG_MSG_INPUT_NOT_EXIST_1 + prod.getName() + L3Constants.LOG_MSG_NO_REQ_COORDS);
        getLogger().warning(L3Constants.LOG_MSG_EXCLUDED);
        return false;
    }
}
