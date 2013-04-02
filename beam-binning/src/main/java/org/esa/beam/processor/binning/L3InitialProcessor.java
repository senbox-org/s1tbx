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
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.algorithm.AlgorithmFactory;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.processor.binning.database.TemporalBinDatabase;

import java.io.File;
import java.io.IOException;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3InitialProcessor extends L3SubProcessor {

    /**
     * Creates the object with given parent processor and logging sink.
     *
     * @param parent the parent processor running this sub-processor
     */
    public L3InitialProcessor(L3Processor parent) {
        super(parent);
    }

    /**
     * Processes a request
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {

        try {
            L3Context context = new L3Context();
            context.setAlgorithmCreator(new AlgorithmFactory());
            loadRequestParameter(context);
            createBinDatabase(context);
            context.save();
        } catch (IOException e) {
            throw new ProcessorException("An I/O error occurred:\n" + e.getMessage(), e);
        }
    }

    /**
     * Loads all parameter from the request that are needed for the initial processing.
     */
    protected void loadRequestParameter(L3Context context) throws ProcessorException {
        getLogger().info(ProcessorConstants.LOG_MSG_LOAD_REQUEST);

        loadMainParamter(context);
        loadBandParameter(context);
        loadLatLonBorders(context);

        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Reads the database_dir, grid_cell_size and (optionaly) the composite_type from the request.
     *
     * @param context
     *
     * @throws ProcessorException
     */
    protected void loadMainParamter(L3Context context) throws ProcessorException {
        Parameter param = null;

        // database directory
        param = getParameter(L3Constants.DATABASE_PARAM_NAME, L3Constants.MSG_MISSING_BINDB);
        File databaseDir = (File) param.getValue();
        ensureDBLocationForCreate(databaseDir);

        //composite type
        final String resamplingType = getStringParamterSafe(L3Constants.RESAMPLING_TYPE_PARAM_NAME,
                                                            L3Constants.RESAMPLING_TYPE_VALUE_BINNING);

        final float gridCellSize;
        if (resamplingType.equals(L3Constants.RESAMPLING_TYPE_VALUE_BINNING)) {
            // grid cell size
            param = getParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME, L3Constants.MSG_MISSING_CELL_SIZE);
            gridCellSize = (Float) param.getValue();
        } else {
            // cells per degree
            param = getParameter(L3Constants.CELLS_PER_DEGREE_PARAM_NAME, L3Constants.MSG_MISSING_CELL_SIZE);
            gridCellSize = (Integer) param.getValue();
        }

        context.setMainParameter(databaseDir, resamplingType, gridCellSize);
    }

    /**
     * Reads the parameter describing the band(s) that should be used for the binning.
     *
     * @param context
     *
     * @throws ProcessorException
     */
    protected void loadBandParameter(L3Context context) throws ProcessorException {
        final Parameter[] allParameters = getRequest().getAllParameters();
        for (int i = 0; i < allParameters.length; i++) {
            final Parameter param = allParameters[i];
            final String paramName = param.getName();
            if (paramName.startsWith(L3Constants.BAND_NAME_PARAMETER_NAME)) {
                final String postFix;
                if (paramName.contains(".")) {
                    postFix = paramName.substring(paramName.lastIndexOf("."));
                } else {
                    postFix = "";
                }

                final Parameter bandNameParam = param;
                final Parameter bitmaskParam = getParameter(L3Constants.BITMASK_PARAMETER_NAME + postFix, "");
                final Parameter algoritmParam = getParameter(L3Constants.ALGORITHM_PARAMETER_NAME + postFix, "");
                final Parameter coefficientParam = getParameter(L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME + postFix,
                                                                "");

                final String bandName = bandNameParam.getValueAsText();

                final String bitmaskExp;
                if (bitmaskParam != null) {
                    bitmaskExp = bitmaskParam.getValueAsText();
                } else {
                    bitmaskExp = "";
                }

                final String algorithmName;
                if (algoritmParam != null) {
                    algorithmName = algoritmParam.getValueAsText();
                } else {
                    algorithmName = L3Constants.ALGORITHM_DEFAULT_VALUE;
                }

                final String algorithmParams;
                if (coefficientParam != null) {
                    algorithmParams = coefficientParam.getValueAsText();
                } else {
                    algorithmParams = "0.5";
                }

                context.addBandDefinition(bandName, bitmaskExp, algorithmName, algorithmParams);
            }
        }
    }

    /**
     * Creates a new bin database using the parameters read from the request.
     */
    protected void createBinDatabase(L3Context context) throws ProcessorException, IOException {
        getLogger().info(L3Constants.LOG_MSG_CREATE_BIN_DB);

        TemporalBinDatabase temporalDB = new TemporalBinDatabase(context, BinDatabaseConstants.TEMP_DB_NAME);
        temporalDB.setNumVarsPerBand(context.getNumberOfAccumulatingVarsPerBand());
        temporalDB.create();
        context.setStorageType(temporalDB.getStorageType());
        temporalDB.close();

        getLogger().info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Loads the lat/lon coordinates of binning borders (if set). When one or all of the parameters are missing, it sets
     * the default values (whole world).
     */
    protected void loadLatLonBorders(L3Context context) throws ProcessorException {
        final float latMin = getFloatParameterSafe(L3Constants.LAT_MIN_PARAMETER_NAME,
                                                   L3Constants.LAT_MIN_DEFAULT_VALUE);
        final float latMax = getFloatParameterSafe(L3Constants.LAT_MAX_PARAMETER_NAME,
                                                   L3Constants.LAT_MAX_DEFAULT_VALUE);
        final float lonMin = getFloatParameterSafe(L3Constants.LON_MIN_PARAMETER_NAME,
                                                   L3Constants.LON_MIN_DEFAULT_VALUE);
        final float lonMax = getFloatParameterSafe(L3Constants.LON_MAX_PARAMETER_NAME,
                                                   L3Constants.LON_MAX_DEFAULT_VALUE);

        if ((latMin >= latMax) || (lonMin >= lonMax)) {
            throw new ProcessorException("Illegal geometric boundary: latMin = " + latMin + " latMax = " + latMax +
                                         " lonMin = " + lonMin + " lonMax = " + lonMax);
        } else {
            context.setBorder(latMin, latMax, lonMin, lonMax);
        }
    }
}