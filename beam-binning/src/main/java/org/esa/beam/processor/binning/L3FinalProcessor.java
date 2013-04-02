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
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.processor.binning.algorithm.Algorithm;
import org.esa.beam.processor.binning.algorithm.AlgorithmFactory;
import org.esa.beam.processor.binning.database.Bin;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.processor.binning.database.ProductExporter;
import org.esa.beam.processor.binning.database.TemporalBinDatabase;

import java.awt.Point;
import java.io.File;
import java.io.IOException;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3FinalProcessor extends L3SubProcessor {

    protected File databaseDir;
    protected ProductRef outputProductRef;

    protected TemporalBinDatabase temporalDB;
    protected TemporalBinDatabase finalDB;
    protected boolean deleteDb;
    protected boolean tailorOutputProduct;

    protected L3Context context;

    /**
     * Creates the object with given parent processor and logging sink.
     *
     * @param parent the parent processor running this sub-processor
     */
    public L3FinalProcessor(L3Processor parent) {
        super(parent);
    }

    /**
     * Processes a request
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        pm.beginTask("Finalizing L3 Product...", 3);
        try {
            try {
                loadRequestParameter();
                loadContext();
                loadTemporalDatabase();
                pm.worked(1);
                if (context.algorithmNeedsInterpretation()) {
                    try {
                        interpreteAlgorithm(SubProgressMonitor.create(pm, 1));
                    } finally {
                        deleteFinalDatabase();
                    }
                } else {
                    exportBinDatabase(temporalDB, createProjection(), outputProductRef, context.getBandDefinitions(),
                                      getMetadata(), SubProgressMonitor.create(pm, 1));
                }
            } finally {
                deleteTemporalDatabase();
                pm.worked(1);
            }
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage(), e);
        } finally {
            pm.done();
        }
    }

    private void interpreteAlgorithm(ProgressMonitor pm) throws IOException, ProcessorException {
        pm.beginTask("Interpreting algorithm...", 2);
        try {
            createFinalDatabase();
            processBinIterpretation(SubProgressMonitor.create(pm, 1));

            exportBinDatabase(finalDB, createProjection(), outputProductRef, context.getBandDefinitions(),
                              getMetadata(), SubProgressMonitor.create(pm, 1));
        } finally {
            pm.done();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Tries to read all required parameter from the request.
     */
    protected void loadRequestParameter() throws ProcessorException {
        getLogger().info(ProcessorConstants.LOG_MSG_LOAD_REQUEST);
        Parameter param = null;

        // database directory
        param = getParameter(L3Constants.DATABASE_PARAM_NAME, L3Constants.MSG_MISSING_BINDB);
        databaseDir = (File) param.getValue();
        ensureDBLocationForLoad(databaseDir);

        // output product
        loadOutputProductFromRequest();

        // keep or remove database
        param = getParameter(L3Constants.DELETE_DB_PARAMETER_NAME, L3Constants.MSG_MISSING_DELETE_BINDB);
        deleteDb = (Boolean) param.getValue();

        final Parameter tailoringParam = getRequest().getParameter(L3Constants.TAILORING_PARAM_NAME);
        if (tailoringParam != null) {
            tailorOutputProduct = (Boolean) tailoringParam.getValue();
        } else {
            tailorOutputProduct = L3Constants.TAILORING_DEFAULT_VALUE;
        }

        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Tries to load the <code>ProductRef</code> for the output product from the request.
     */
    protected void loadOutputProductFromRequest() throws ProcessorException {
        Request request;
        request = getRequest();

        if (request.getNumOutputProducts() > 0) {
            ProductRef ref = request.getOutputProductAt(0);
            if (ref == null) {
                handleError(ProcessorConstants.LOG_MSG_NO_OUTPUT_IN_REQUEST);
            }
            outputProductRef = ref;
        } else {
            handleError(ProcessorConstants.LOG_MSG_NO_OUTPUT_IN_REQUEST);
        }
    }

    protected void loadContext() throws IOException,
                                        ProcessorException {
        context = new L3Context();
        context.setAlgorithmCreator(new AlgorithmFactory());
        context.load(databaseDir);
    }

    /**
     * Trigger base class to load the temporal bin database provided with the request file
     */
    protected void loadTemporalDatabase() throws IOException,
                                                 ProcessorException {
        getLogger().info(L3Constants.LOG_MSG_LOAD_TEMP_DB);

        temporalDB = new TemporalBinDatabase(context, BinDatabaseConstants.TEMP_DB_NAME);
        temporalDB.setNumVarsPerBand(context.getNumberOfAccumulatingVarsPerBand());
        temporalDB.open();

        if (context.getProcessedProducts().length < 1) {
            handleError(L3Constants.LOG_MSG_EMPTY_DB);
        }
        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Deletes the temporal database if specified to do so
     */
    protected void deleteTemporalDatabase() throws IOException {
        if (deleteDb) {
            getLogger().info(L3Constants.LOG_MSG_DELETE_TEMP_DB);
            temporalDB.delete();
            getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
            File databaseDir = context.getDatabaseDir();
            databaseDir.delete();
        }
    }

    /**
     * Deletes the final database
     */
    protected void deleteFinalDatabase() throws IOException {
        if (finalDB != null && deleteDb) {
            getLogger().info(L3Constants.LOG_MSG_DELETE_FINAL_DB);
            finalDB.delete();
            getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
        }
    }

    /**
     * Creates the final database if the algorithm needs this step to be performed.
     */
    protected void createFinalDatabase() throws IOException,
                                                ProcessorException {
        getLogger().info(L3Constants.LOG_MSG_CREATE_FINAL_DB);

        finalDB = new TemporalBinDatabase(context, BinDatabaseConstants.FINAL_DB_NAME);
        finalDB.setNumVarsPerBand(context.getNumberOfInterpretedVarsPerBand());
        finalDB.create();

        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Interpretes the bins of the temporal database if algorithm needs this to be done.
     */
    protected void processBinIterpretation(ProgressMonitor pm) throws IOException {
        getLogger().info(L3Constants.LOG_MSG_INTERPRETE_BIN_CONTENT);

        final int rowOffset = temporalDB.getRowOffset();
        final int colOffset = temporalDB.getColOffset();
        final int width = temporalDB.getWidth();
        final int height = temporalDB.getHeight();
        Point rowcol = new Point();
        Bin tempBin = temporalDB.createBin();
        Bin finalBin = finalDB.createBin();

        pm.beginTask(L3Constants.LOG_MSG_INTERPRETE_BIN_CONTENT, height - rowOffset);
        try {
            for (int row = rowOffset; row < rowOffset + height; row++) {
                rowcol.y = row;
                for (int col = colOffset; col < colOffset + width; col++) {
                    rowcol.x = col;

                    temporalDB.read(rowcol, tempBin);
                    final L3Context.BandDefinition[] bandDefinitions = context.getBandDefinitions();
                    for (int bandIndex = 0; bandIndex < bandDefinitions.length; bandIndex++) {
                        final L3Context.BandDefinition bandDef = bandDefinitions[bandIndex];
                        final Algorithm algo = bandDef.getAlgorithm();
                        tempBin.setBandIndex(bandIndex);
                        finalBin.setBandIndex(bandIndex);
                        algo.interprete(tempBin, finalBin);
                    }
                    finalDB.write(rowcol, finalBin);
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
            pm.done();
            finalDB.flush();
        }

        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Exports the given bin database into the specified product.
     *
     * @param binDatabase the bin database to be exported
     * @param projection  the projection, that should be used.
     * @param productRef  the productRef, that describes the product to which to export to
     * @param bandDefinitions
     *@param metadata    an array with the metadata.
     * @param pm          a monitor to inform the user about progress
 * @throws IOException
     * @throws ProcessorException
     */
    protected void exportBinDatabase(TemporalBinDatabase binDatabase, L3ProjectionRaster projection,
                                     ProductRef productRef, L3Context.BandDefinition[] bandDefinitions, MetadataElement[] metadata,
                                     ProgressMonitor pm)
            throws IOException,
                   ProcessorException {
        ProductExporter exporter = new ProductExporter(binDatabase, getLogger());

        final float stepsPerDegree;
        if (L3Constants.RESAMPLING_TYPE_VALUE_BINNING.equals(context.getResamplingType())) {
            final float kmPerDegree = BinDatabaseConstants.PI_EARTH_RADIUS / 180.f;
            final float gridCellSizeInKm = context.getGridCellSize();
            stepsPerDegree = kmPerDegree / gridCellSizeInKm;
        } else {
            stepsPerDegree = context.getGridCellSize();
        }
        exporter.setProjection(projection, stepsPerDegree);

        pm.beginTask("Exporting bin database...", tailorOutputProduct ? 2 : 1);
        boolean aborted;
        try {
            if (tailorOutputProduct) {
                exporter.estimateExportRegion(SubProgressMonitor.create(pm, 1));
            } else {
                exporter.setExportRegion(context.getBorder());
            }
            aborted = false;
            try {
                exporter.createOutputProduct(productRef, bandDefinitions, metadata);
                aborted = exporter.outputBinDatabase(context.getLocator(), SubProgressMonitor.create(pm, 1));
            } catch (IOException e) {
                throw new ProcessorException("Couldn't export product: " + e.getMessage(), e);
            } finally {
                exporter.close();
            }
        } finally {
            pm.done();
        }
        if (aborted) {
            setCurrentState(L3Constants.STATUS_ABORTED);
        }
    }

    /**
     * Creates a projection raster.
     *
     * @return a projection ratser.
     */
    protected L3ProjectionRaster createProjection() {
        L3ProjectionRaster projection;
//        if (context.getResamplingType().equals(L3Constants.RESAMPLING_TYPE_VALUE_FLUX_CONSERVING)) {
//            projection = new L3PlateCarreRaster();
//        } else {
        projection = new L3ProjectionRaster();
//        }
        return projection;
    }

    /**
     * Creates Metadata nodes for all the necessary information.
     */
    protected MetadataElement[] getMetadata() {
        MetadataElement[] metadata = new MetadataElement[2];

        MetadataElement fileList = new MetadataElement("Input_Products");
        final String[] productList = context.getProcessedProducts();
        String keyString;

        for (int n = 0; n < productList.length; n++) {
            keyString = "Product." + n;
            fileList.addAttribute(new MetadataAttribute(keyString, ProductData.createInstance(productList[n]), true));
        }
        metadata[0] = fileList;

        MetadataElement binParams = new MetadataElement("Binning_Parameter");
        binParams.addAttribute(new MetadataAttribute("Resampling_Type",
                                                     ProductData.createInstance(context.getResamplingType()),
                                                     true));
        final String cellSizeName;
        if (L3Constants.RESAMPLING_TYPE_VALUE_BINNING.equals(context.getResamplingType())) {
            cellSizeName = "Bin_Size_In_Km";
        } else {
            cellSizeName = "Bins_Per_Degree";
        }
        binParams.addAttribute(new MetadataAttribute(cellSizeName,
                                                     ProductData.createInstance(new float[]{context.getGridCellSize()}),
                                                     true));
        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
        for (int bandIndex = 0; bandIndex < bandDefs.length; bandIndex++) {
            final L3Context.BandDefinition bandDef = bandDefs[bandIndex];
            binParams.addAttribute(new MetadataAttribute("Geophysical_Parameter_" + bandIndex,
                                                         ProductData.createInstance(bandDef.getBandName()),
                                                         true));
            binParams.addAttribute(new MetadataAttribute("Bitmask_" + bandIndex,
                                                         ProductData.createInstance(bandDef.getBitmaskExp()),
                                                         true));
            binParams.addAttribute(new MetadataAttribute("Algorithm_" + bandIndex,
                                                         ProductData.createInstance(
                                                                 bandDef.getAlgorithm().getTypeString()),
                                                         true));
        }
        metadata[1] = binParams;

        return metadata;
    }
}