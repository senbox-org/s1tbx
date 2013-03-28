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

import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.param.editors.ComboBoxEditor;
import org.esa.beam.framework.param.validators.BooleanExpressionValidator;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.processor.binning.database.AbstractBinDatabase;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3RequestElementFactory implements RequestElementFactory {

    protected final Map _paramInfoMap = new HashMap();
    private final DefaultRequestElementFactory _defaultFactory = DefaultRequestElementFactory.getInstance();

    /**
     * Singleton interface.
     */
    public static L3RequestElementFactory getInstance() {
        return Holder.instance;
    }

    /**
     * Creates a new reference to an input product for the current processing request.
     *
     * @param file       the input product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the element could not be created
     */
    public ProductRef createInputProductRef(File file, String fileFormat, String typeId) throws RequestElementFactoryException {
        return  _defaultFactory.createInputProductRef(file, fileFormat, typeId);
    }

    /**
     * Creates a new reference to an output product for the current processing request.
     *
     * @param file       the output product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the element could not be created
     */
    public ProductRef createOutputProductRef(File file, String fileFormat, String typeId) throws RequestElementFactoryException {
        return _defaultFactory.createOutputProductRef(file, fileFormat, typeId);
    }

    /**
     * Creates a new processing parameter for the current processing request.
     *
     * @param name  the parameter name, must not be <code>null</code> or empty
     * @param value the parameter value, can be <code>null</code> if yet not known
     *
     * @throws java.lang.IllegalArgumentException
     *          if <code>name</code> is <code>null</code> or empty
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *          if the parameter could not be created or is invalid
     */
    public Parameter createParameter(String name, String value) throws RequestElementFactoryException {
        Guardian.assertNotNullOrEmpty("name", name);
        Guardian.assertNotNull("value", value);
        Parameter param;

        try {
            param = createParamWithDefaultValueSet(name);
            param.setValueAsText(value, null);
        } catch (IllegalArgumentException e) {
            throw new RequestElementFactoryException(e.getMessage());
        }

        return param;
    }

    /**
     * Creates a parameter for the default input product path - which is the current user's home directory.
     */
    public Parameter createDefaultInputProductParameter() {
        return _defaultFactory.createDefaultInputProductParameter();
    }

    /**
     * Creates a default logging pattern parameter set to the prefix passed in.
     *
     * @param prefix the default setting for the logging pattern
     *
     * @return a logging pattern Parameter conforming the system settings
     */
    public Parameter createDefaultLogPatternParameter(String prefix) {
        return _defaultFactory.createDefaultLogPatternParameter(prefix);
    }

    /**
     * Creates a logging to output product parameter set to true.
     *
     * @return the created logging to output product parameter
     */
    public Parameter createLogToOutputParameter(final String value) throws ParamValidateException {
        final Parameter logToOutputParameter = _defaultFactory.createLogToOutputParameter(value);
        logToOutputParameter.getProperties().setDefaultValue(false);
        return logToOutputParameter;
    }

    /**
     * Creates a parameter with the name passed in by searching the parameter properties for the given name. Once found,
     * creates the parameter according to the properties and initializes it with the default value
     */
    public Parameter createParamWithDefaultValueSet(String paramName) {
        final boolean nameWithIndex = paramName.contains(".");
        ParamProperties paramProps;
        if (nameWithIndex) {
            paramProps = getParamProperties(paramName.substring(0, paramName.lastIndexOf(".")));
        } else {
            paramProps = getParamProperties(paramName);
        }
        Parameter param = new Parameter(paramName, paramProps);
        param.setDefaultValue();
        return param;
    }

    /**
     * Generates a default database location in the user's home directory
     */
    public Parameter generateDefaultDbLocation() throws ProcessorException {
        ParamProperties paramProps = getParamProperties(L3Constants.DATABASE_PARAM_NAME);
        Parameter param = new Parameter(L3Constants.DATABASE_PARAM_NAME, paramProps);
        // check if the database exists, if so create a new unique name
        File dbLocation = (File) param.getValue();
        if (dbLocation.exists()) {
            int index = 1;
            while (dbLocation.exists()) {
                String path = dbLocation.getParent();
                String dbName = FileUtils.getFilenameWithoutExtension(dbLocation);
                int stripIndex = dbName.lastIndexOf('-');
                if (stripIndex > 0) {
                    // must strip old #num extension
                    dbName = dbName.substring(0, stripIndex);
                }
                dbName += ("-" + index + BinDatabaseConstants.FILE_EXTENSION);
                dbLocation = new File(path, dbName);
                ++index;
            }

            try {
                param.setValue(dbLocation);
            } catch (ParamValidateException e) {
                throw new ProcessorException("Illegal processing parameter assignment:\n" +
                                             param.getName()+ " = " + dbLocation, e);
            }
        }
        return param;
    }

    /**
     * Creates an input product parameter set to the default path.
     */
    public ProductRef createDefaultInputProductRef() {
        return new ProductRef(SystemUtils.getUserHomeDir());
    }

    /**
     * Creates an output product parameter set to the default path.
     */
    public Parameter createDefaultOutputProductParameter() {
        File defaultOutProduct = new File(SystemUtils.getUserHomeDir(), L3Constants.DEFAULT_FILE_NAME);
        int fsm = ParamProperties.FSM_FILES_ONLY;
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(fsm, defaultOutProduct);
        paramProps.setLabel(L3Constants.OUTPUT_PRODUCT_LABELTEXT);
        paramProps.setDescription(L3Constants.OUTPUT_PRODUCT_DESCRIPTION);
        Parameter _param = new Parameter(L3Constants.OUTPUT_PRODUCT_PARAM_NAME, paramProps);
        _param.setDefaultValue();
        return _param;
    }

    ///////////////////////////////////////////////////////////////////////////
    //////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs the object. Initializes the parameter map.
     */
    protected L3RequestElementFactory() {
        fillParamInfoMap();
    }

    /**
     * Fills the internal <code>HashMap</code> with the valid parameter infos for all parameter supported by this class
     */
    private void fillParamInfoMap() {
        _paramInfoMap.put(L3Constants.RESAMPLING_TYPE_PARAM_NAME, createParamInfoResamplingType());
        _paramInfoMap.put(L3Constants.TAILORING_PARAM_NAME, createParamInfoTailoring());
        _paramInfoMap.put(L3Constants.PROCESS_TYPE_PARAM_NAME, createParamInfoProcessType());
        _paramInfoMap.put(L3Constants.DATABASE_PARAM_NAME, createParamInfoDatabase());
        _paramInfoMap.put(L3Constants.GRID_CELL_SIZE_PARAM_NAME, createParamInfoGridCellSize());
        _paramInfoMap.put(L3Constants.CELLS_PER_DEGREE_PARAM_NAME, createParamInfoCellsPerDegree());
        _paramInfoMap.put(L3Constants.BAND_NAME_PARAMETER_NAME, createParamInfoBandName());
        _paramInfoMap.put(L3Constants.ALGORITHM_PARAMETER_NAME, createParamInfoBinningAlgorithm());
        _paramInfoMap.put(L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME, createParamInfoWeightCoefficient());
        _paramInfoMap.put(L3Constants.BITMASK_PARAMETER_NAME, createParamInfoBitmask());
        _paramInfoMap.put(L3Constants.DELETE_DB_PARAMETER_NAME, createParamInfoDeleteDB());
        _paramInfoMap.put(L3Constants.LAT_MIN_PARAMETER_NAME, createParamInfoLatMin());
        _paramInfoMap.put(L3Constants.LAT_MAX_PARAMETER_NAME, createParamInfoLatMax());
        _paramInfoMap.put(L3Constants.LON_MIN_PARAMETER_NAME, createParamInfoLonMin());
        _paramInfoMap.put(L3Constants.LON_MAX_PARAMETER_NAME, createParamInfoLonMax());

    }

    /**
     * Gets a default <code>ParamProperties</code> for the parameter with given name.
     *
     * @param name the parameter name
     *
     * @throws java.lang.IllegalArgumentException
     *          when the parameter name is not specified as valid name
     */
    private ParamProperties getParamProperties(String name) throws IllegalArgumentException {
        ParamProperties paramProps = (ParamProperties) _paramInfoMap.get(name);
        if (paramProps == null) {
            if(name.equals(L3Constants.BAND_NUM_PARAMETER_NAME)) {
                paramProps = new ParamProperties(Integer.class, 0);
            }else if(name.startsWith("band_")) {
                paramProps = new ParamProperties(String.class, "");
            }else{
                throw new IllegalArgumentException("invalid parameter name");
            }
        }
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter composite type
     * added by TLankester 26/04/05
     */
    private ParamProperties createParamInfoResamplingType() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(L3Constants.RESAMPLING_TYPE_VALUE_BINNING);
        paramProps.setValueSet(new String[]{L3Constants.RESAMPLING_TYPE_VALUE_BINNING,
                                            L3Constants.RESAMPLING_TYPE_VALUE_FLUX_CONSERVING});
        paramProps.setValueSetBound(true);
        paramProps.setLabel(L3Constants.RESAMPLING_TYPE_PARAM_LABEL);
        paramProps.setDescription(L3Constants.RESAMPLING_TYPE_PARAM_DESC);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter bondary mode
     */
    private ParamProperties createParamInfoTailoring() {
        final ParamProperties paramProperties = _defaultFactory.createBooleanParamProperties();
        paramProperties.setLabel(L3Constants.TAILORING_PARAM_LABEL);
        paramProperties.setDescription(L3Constants.TAILORING_PARAM_DESC);
        paramProperties.setDefaultValue(L3Constants.TAILORING_DEFAULT_VALUE);
        return paramProperties;
    }

    /**
     * Creates the parameter information for the parameter process type
     */
    private ParamProperties createParamInfoProcessType() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(L3Constants.PROCESS_TYPE_INIT);
        paramProps.setValueSet(new String[]{L3Constants.PROCESS_TYPE_INIT,
                                            L3Constants.PROCESS_TYPE_UPDATE,
                                            L3Constants.PROCESS_TYPE_FINALIZE});
        paramProps.setValueSetBound(true);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter database_dir
     */
    private ParamProperties createParamInfoDatabase() {
        File defaultDbPath = new File(SystemUtils.getUserHomeDir(), L3Constants.DEFAULT_DATABASE_NAME);
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(ParamProperties.FSM_DIRECTORIES_ONLY,
                                                                               defaultDbPath);
        paramProps.setLabel(L3Constants.DATABASE_LABEL);
        paramProps.setDescription(L3Constants.DATABASE_DESCRIPTION);
        paramProps.setCurrentFileFilter(AbstractBinDatabase.createDbFileFilter());
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter grid_cell_size
     */
    private ParamProperties createParamInfoGridCellSize() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(L3Constants.GRID_CELL_SIZE_DEFAULT);
        paramProps.setMinValue(L3Constants.GRID_CELL_SIZE_MIN_VALUE);
        paramProps.setMaxValue(L3Constants.GRID_CELL_SIZE_MAX_VALUE);
        paramProps.setLabel(L3Constants.GRID_CELL_SIZE_LABEL);
        paramProps.setDescription(L3Constants.GRID_CELL_SIZE_DESCRIPTION);
        paramProps.setPhysicalUnit(L3Constants.GRID_CELL_SIZE_UNIT);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter cells_per_degree
     */
    private ParamProperties createParamInfoCellsPerDegree() {
        ParamProperties paramProps = _defaultFactory.createIntegerParamProperties();
        paramProps.setDefaultValue(L3Constants.CELLS_PER_DEGREE_DEFAULT);
        paramProps.setMinValue(L3Constants.CELLS_PER_DEGREE_MIN_VALUE);
        paramProps.setMaxValue(L3Constants.CELLS_PER_DEGREE_MAX_VALUE);
        paramProps.setLabel(L3Constants.CELLS_PER_DEGREE_LABEL);
        paramProps.setDescription(L3Constants.CELLS_PER_DEGREE_DESCRIPTION);
        paramProps.setPhysicalUnit(L3Constants.CELLS_PER_DEGREE_UNIT);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter band_name
     */
    private ParamProperties createParamInfoBandName() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setLabel(L3Constants.BAND_NAME_LABEL);
        paramProps.setDescription(L3Constants.BAND_NAME_DESCRIPTION);
        paramProps.setEditorClass(ComboBoxEditor.class);
        paramProps.setNullValueAllowed(true);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter bining_algorithm
     */
    private ParamProperties createParamInfoBinningAlgorithm() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setValueSet(L3Constants.ALGORITHM_VALUE_SET);
        paramProps.setValueSetBound(true);
        paramProps.setDefaultValue(L3Constants.ALGORITHM_DEFAULT_VALUE);
        paramProps.setLabel(L3Constants.ALGORITHM_LABEL);
        paramProps.setDescription(L3Constants.ALGORITHM_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter weight_coefficient
     */
    private ParamProperties createParamInfoWeightCoefficient() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setDefaultValue(L3Constants.WEIGHT_COEFFICIENT_DEFAULT_VALUE);
        paramProps.setLabel(L3Constants.WEIGHT_COEFFICIENT_LABEL);
        paramProps.setDescription(L3Constants.WEIGHT_COEFFICIENT_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter bitmask
     */
    private ParamProperties createParamInfoBitmask() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setValidatorClass(BooleanExpressionValidator.class);
        paramProps.setEditorClass(BooleanExpressionEditor.class);
        paramProps.setDefaultValue("");
        paramProps.setLabel(L3Constants.BITMASK_LABEL);
        paramProps.setDescription(L3Constants.BITMASK_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter delete_db
     */
    private ParamProperties createParamInfoDeleteDB() {
        ParamProperties paramProps = _defaultFactory.createBooleanParamProperties();
        paramProps.setDefaultValue(L3Constants.DELETE_DB_DEFAULT_VALUE);
        paramProps.setLabel(L3Constants.DELETE_DB_LABEL);
        paramProps.setDescription(L3Constants.DELETE_DB_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter latMin
     */
    private ParamProperties createParamInfoLatMin() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setDefaultValue(L3Constants.LAT_MIN_DEFAULT_VALUE);
        paramProps.setMinValue(L3Constants.LAT_MINIMUM_VALUE);
        paramProps.setMaxValue(L3Constants.LAT_MAXIMUM_VALUE);
        paramProps.setLabel(L3Constants.LAT_MIN_LABEL);
        paramProps.setDescription(L3Constants.LAT_MIN_DESCRIPTION);
        paramProps.setPhysicalUnit(L3Constants.LAT_LON_PHYS_UNIT);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter latMax
     */
    private ParamProperties createParamInfoLatMax() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setDefaultValue(L3Constants.LAT_MAX_DEFAULT_VALUE);
        paramProps.setMinValue(L3Constants.LAT_MINIMUM_VALUE);
        paramProps.setMaxValue(L3Constants.LAT_MAXIMUM_VALUE);
        paramProps.setLabel(L3Constants.LAT_MAX_LABEL);
        paramProps.setDescription(L3Constants.LAT_MAX_DESCRIPTION);
        paramProps.setPhysicalUnit(L3Constants.LAT_LON_PHYS_UNIT);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter lonMin
     */
    private ParamProperties createParamInfoLonMin() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setDefaultValue(L3Constants.LON_MIN_DEFAULT_VALUE);
        paramProps.setMinValue(L3Constants.LON_MINIMUM_VALUE);
        paramProps.setMaxValue(L3Constants.LON_MAXIMUM_VALUE);
        paramProps.setLabel(L3Constants.LON_MIN_LABEL);
        paramProps.setDescription(L3Constants.LON_MIN_DESCRIPTION);
        paramProps.setPhysicalUnit(L3Constants.LAT_LON_PHYS_UNIT);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter lonMax
     */
    private ParamProperties createParamInfoLonMax() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setDefaultValue(L3Constants.LON_MAX_DEFAULT_VALUE);
        paramProps.setMinValue(L3Constants.LON_MINIMUM_VALUE);
        paramProps.setMaxValue(L3Constants.LON_MAXIMUM_VALUE);
        paramProps.setLabel(L3Constants.LON_MAX_LABEL);
        paramProps.setDescription(L3Constants.LON_MAX_DESCRIPTION);
        paramProps.setPhysicalUnit(L3Constants.LAT_LON_PHYS_UNIT);
        return paramProps;
    }

    public Parameter createOutputFormatParameter() {
        return _defaultFactory.createOutputFormatParameter();
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final L3RequestElementFactory instance = new L3RequestElementFactory();
    }
}
