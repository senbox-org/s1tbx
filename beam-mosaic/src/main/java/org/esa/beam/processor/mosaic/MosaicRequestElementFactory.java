/*
 * $Id: MosaicRequestElementFactory.java,v 1.3 2007/04/13 14:33:02 norman Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.processor.mosaic;

import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.validators.NumberValidator;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.Guardian;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MosaicRequestElementFactory implements RequestElementFactory {

    private static MosaicRequestElementFactory _instance;
    private DefaultRequestElementFactory _defaultFactory;
    private Map<String, ParamProperties> _paramInfoMap;

    /**
     * Retrieves the one and only instance of this class. Singleton interface
     *
     * @return the instance
     */
    public static MosaicRequestElementFactory getInstance() {
        if (_instance == null) {
            _instance = new MosaicRequestElementFactory();
        }
        return _instance;
    }

    /**
     * Creates a new processing parameter for the current processing request.
     *
     * @param name  the parameter name, must not be <code>null</code> or empty
     * @param value the parameter value, can be <code>null</code> if yet not known
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code> or empty
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the parameter could not be created or is invalid
     */
    public Parameter createParameter(String name, String value) throws RequestElementFactoryException {
        Guardian.assertNotNullOrEmpty("name", name);

        Parameter param;
        try {
            param = createParamWithDefaultValueSet(name);
            if (value != null) {
                param.setValueAsText(value, null);
            }
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
     * @return a logging pattern Parameter conforming the system settings
     */
    public Parameter createDefaultLogPatternParameter(String prefix) {
        return _defaultFactory.createDefaultLogPatternParameter(prefix);
    }

    /**
     * Creates an output product parameter set to the default path.
     */
    public Parameter createDefaultOutputProductParameter() {
        Parameter defaultOutputProductParameter = _defaultFactory.createDefaultOutputProductParameter();
        ParamProperties properties = defaultOutputProductParameter.getProperties();
        Object defaultValue = properties.getDefaultValue();
        if (defaultValue instanceof File) {
            properties.setDefaultValue(new File((File) defaultValue, MosaicConstants.DEFAULT_OUTPUT_PRODUCT_NAME));
        }
        defaultOutputProductParameter.setDefaultValue();
        // @todo 1 nf/he - also set default output format here, so that it fits to SmileConstants.DEFAULT_FILE_NAME's extension (.dim)
        return defaultOutputProductParameter;
    }

    /**
     * Creates a new reference to an input product for the current processing request.
     *
     * @param file       the input product's URL, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the element could not be created
     */
    public ProductRef createInputProductRef(File file, String fileFormat, String typeId) throws
            RequestElementFactoryException {
        return _defaultFactory.createInputProductRef(file, fileFormat, typeId);
    }

    /**
     * Creates a logging to output product parameter set to true.
     *
     * @return the created logging to output product parameter
     */
    public Parameter createLogToOutputParameter(final String value) throws ParamValidateException {
        return _defaultFactory.createLogToOutputParameter(value);
    }

    /**
     * Creates a new reference to an output product for the current processing request.
     *
     * @param file       the output product's URL, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     * @throws IllegalArgumentException if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *                                  if the element could not be created
     */
    public ProductRef createOutputProductRef(File file, String fileFormat, String typeId) throws
            RequestElementFactoryException {
        return _defaultFactory.createOutputProductRef(file, fileFormat, typeId);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs the object with default parameters.
     */
    private MosaicRequestElementFactory() {
        // private constructor - singleton interface
        _defaultFactory = DefaultRequestElementFactory.getInstance();
        _paramInfoMap = new HashMap<String, ParamProperties>();
        initParamInfoMap();
    }

    /**
     * Stores all parameter properties needed by the processor into the map.
     */
    private void initParamInfoMap() {
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_UPDATE_MODE, createParamInfoUpdateMode());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_WEST_LON, createParamInfoWestLon());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_EAST_LON, createParamInfoEastLon());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_NORTH_LAT, createParamInfoNorthLat());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_SOUTH_LAT, createParamInfoSouthLat());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_PIXEL_SIZE_X, createParamInfoPixelSizeX());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_PIXEL_SIZE_Y, createParamInfoPixelSizeY());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_PROJECTION_NAME, createParamInfoProjectionName());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_PROJECTION_PARAMETERS, createParamInfoProjectionParameters());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_CONDITION_OPERATOR, createParamInfoConditionOperator());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_FIT_OUTPUT, createParamInfoFitOutput());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_NORTHING, createParamInfoNorthing());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_EASTING, createParamInfoEasting());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_OUTPUT_WIDTH, createParamInfoOutputWidth());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_OUTPUT_HEIGHT, createParamInfoOutputHeight());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_BANDS, createParamInfoBands());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_INCLUDE_TIE_POINT_GRIDS, createParamIncludeTiePontGrids());

        _paramInfoMap.put(MosaicConstants.PARAM_NAME_ORTHORECTIFY_INPUT_PRODUCTS, createParamInfoOrthorectification());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_ELEVATION_MODEL_FOR_ORTHORECTIFICATION,
                          createParamInfoElevation());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_RESAMPLING_METHOD, createParamInfoResamplingMethod());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_NO_DATA_VALUE, createNoDataValueParamProperties());

        _paramInfoMap.put(MosaicConstants.PARAM_NAME_GEOCODING_LATITUDES, createStringParamProperties());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_GEOCODING_LONGITUDES, createStringParamProperties());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_GEOCODING_VALID_MASK, createStringParamProperties());
        _paramInfoMap.put(MosaicConstants.PARAM_NAME_GEOCODING_SEARCH_RADIUS, createParamSourceSearchRadius());
    }

    private ParamProperties createParamIncludeTiePontGrids() {
        ParamProperties paramProps = new ParamProperties(Boolean.class);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_INCLUDE_TIE_POINT_GRIDS);
        return paramProps;
    }

    private ParamProperties createNoDataValueParamProperties() {
        ParamProperties paramProps = new ParamProperties(Double.class);
        paramProps.setDefaultValue(MapInfo.DEFAULT_NO_DATA_VALUE);
        return paramProps;
    }

    private ParamProperties createParamSourceSearchRadius() {
        ParamProperties paramProps = _defaultFactory.createIntegerParamProperties();
        paramProps.setDefaultValue(MosaicConstants.DEFAULT_GEOCODING_SEARCH_RADIUS);
        paramProps.setMinValue(1);
        paramProps.setMaxValue(99);
        return paramProps;
    }

    private ParamProperties createParamInfoOrthorectification() {
        final ParamProperties paramProps = _defaultFactory.createBooleanParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_ORTHORECTIFY_INPUT_PRODUCTS);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_ORTHORECTIFY_INPUT_PRODUCTS);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_ORTHORECTIFY_INPUT_PRODUCTS);
        return paramProps;
    }

    private ParamProperties createParamInfoElevation() {
        final ElevationModelDescriptor[] descriptors = ElevationModelRegistry.getInstance().getAllDescriptors();
        final String[] demValueSet = new String[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            demValueSet[i] = descriptors[i].getName();
        }
        ParamProperties paramProps = _defaultFactory.createStringArrayParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_ELEVATION_MODEL_FOR_ORTHORECTIFICATION);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_ELEVATION_MODEL_FOR_ORTHORECTIFICATION);
        paramProps.setValueSet(demValueSet);
        paramProps.setValueSetBound(true);
        paramProps.setEditorClass(org.esa.beam.framework.param.editors.ComboBoxEditor.class);
        paramProps.setDefaultValue(demValueSet[0]);
        return paramProps;
    }

    private ParamProperties createParamInfoResamplingMethod() {
        ParamProperties paramProps = _defaultFactory.createStringArrayParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_RESAMPLING_METHOD);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_RESAMPLING_METHOD);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_RESAMPLING_METHOD);
        paramProps.setValueSet(MosaicConstants.PARAM_VALUESET_RESAMPLING_METHOD);
        paramProps.setValueSetBound(true);
        paramProps.setEditorClass(org.esa.beam.framework.param.editors.ComboBoxEditor.class);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter "BANDS_PARAM_NAME"
     */
    private ParamProperties createParamInfoBands() {
        ParamProperties paramProps = _defaultFactory.createStringArrayParamProperties();
        paramProps.setNullValueAllowed(true);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter is update mode.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoUpdateMode() {
        final ParamProperties paramProps = _defaultFactory.createBooleanParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_UPDATE_MODE);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_UPDATE_MODE);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_UPDATE_MODE);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter upper left longitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoWestLon() {
        ParamProperties paramProps = createLongitudeProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_WEST_LON);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_WEST_LON);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_WEST_LON);
        paramProps.setPhysicalUnit(MosaicConstants.PARAM_UNIT_DEGREES);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter upper left latitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoNorthing() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_NORTHING);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_NORTHING);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_NORTHING);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter upper left latitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoOutputWidth() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_OUTPUT_WIDTH);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_OUTPUT_WIDTH);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_OUTPUT_WIDTH);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter upper left latitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoOutputHeight() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_OUTPUT_HEIGHT);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_OUTPUT_HEIGHT);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_OUTPUT_HEIGHT);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter upper left latitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoEasting() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_EASTING);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_EASTING);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_EASTING);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter upper left latitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoNorthLat() {
        ParamProperties paramProps = createLatitudeProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_NORTH_LAT);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_NORTH_LAT);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_NORTH_LAT);
        paramProps.setPhysicalUnit(MosaicConstants.PARAM_UNIT_DEGREES);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter lower right longitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoEastLon() {
        ParamProperties paramProps = createLongitudeProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_EAST_LON);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_EAST_LON);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_EAST_LON);
        paramProps.setPhysicalUnit(MosaicConstants.PARAM_UNIT_DEGREES);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter lower right lattitude.
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoSouthLat() {
        ParamProperties paramProps = createLatitudeProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_SOUTH_LAT);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_SOUTH_LAT);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_SOUTH_LAT);
        paramProps.setPhysicalUnit(MosaicConstants.PARAM_UNIT_DEGREES);
        return paramProps;
    }

    private ParamProperties createLongitudeProperties() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setMinValue(-180f);
        paramProps.setMaxValue(180f);
        return paramProps;
    }

    private ParamProperties createLatitudeProperties() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setMinValue(-90f);
        paramProps.setMaxValue(90f);
        return paramProps;
    }

    /**
     * Creates the parameter information for the pixel size parameter in x direction
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoPixelSizeX() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setValidatorClass(PixelSizeValidator.class);
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_PIXEL_SIZE_X);
        paramProps.setPhysicalUnit(MosaicConstants.PARAM_UNIT_METERS);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_PIXEL_SIZE_X);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_PIXEL_SIZE_X);
        return paramProps;
    }

    /**
     * Creates the parameter information for the pixel size parameter in y direction
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoPixelSizeY() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setValidatorClass(PixelSizeValidator.class);
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_PIXEL_SIZE_Y);
        paramProps.setPhysicalUnit(MosaicConstants.PARAM_UNIT_METERS);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_PIXEL_SIZE_Y);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_PIXEL_SIZE_Y);
        return paramProps;
    }

    public static class PixelSizeValidator extends NumberValidator {

        @Override
        public void validate(final Parameter parameter, final Object value) throws ParamValidateException {
            super.validate(parameter, value);
            if (value instanceof Number) {
                final Number number = (Number) value;
                final double size = number.doubleValue();
                if (size <= 0.00) {
                    throw new ParamValidateException(parameter, "Value must be greater than zero.");
                }
            }
        }
    }

    /**
     * Creates the parameter information for the projection name parameter
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoProjectionName() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_PROJECTION_NAME);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_PROJECTION_NAME);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_PROJECTION_NAME);
        return paramProps;
    }

    /**
     * Creates the parameter information for the projection values parameter
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoFitOutput() {
        final ParamProperties paramProps = _defaultFactory.createBooleanParamProperties();
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_FIT_OUTPUT);
        return paramProps;
    }

    /**
     * Creates the parameter information for the projection values parameter
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoProjectionParameters() {
        final ParamProperties paramProps = _defaultFactory.createStringArrayParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_PROJECTION_PARAMETERS);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_PROJECTION_PARAMETERS);
        return paramProps;
    }

    /**
     * Creates the parameter information for the condition operator
     *
     * @return parameter information
     */
    private ParamProperties createParamInfoConditionOperator() {
        final ParamProperties paramProps = _defaultFactory.createStringArrayParamProperties();
        paramProps.setLabel(MosaicConstants.PARAM_LABEL_CONDITION_OPERATOR);
        paramProps.setDescription(MosaicConstants.PARAM_DESCRIPTION_CONDITION_OPERATOR);
        paramProps.setValueSet(MosaicConstants.PARAM_VALUESET_CONDITIONS_OPERATOR);
        paramProps.setValueSetBound(true);
        paramProps.setEditorClass(org.esa.beam.framework.param.editors.ComboBoxEditor.class);
        paramProps.setDefaultValue(MosaicConstants.PARAM_DEFAULT_VALUE_CONDITION_OPERATOR);
        return paramProps;
    }

    /**
     * Creates a parameter with given name, set to it's default value specified in the ParameterProperties.
     *
     * @param paramName the patameter name
     * @return the parameter
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code> or empty
     */
    private Parameter createParamWithDefaultValueSet(String paramName) {
        ParamProperties paramProps = getParamInfo(paramName);
        Parameter param = new Parameter(paramName, paramProps.createCopy());
        param.setDefaultValue();
        return param;
    }

    /**
     * Creates the parameter information for the parameter with the given name. Returns <code>null</code> when no
     * parameter properties for the name can be found.
     *
     * @param parameterName the parameter name
     * @return the parameter properties
     */
    private ParamProperties getParamInfo(String parameterName) {
        ParamProperties paramProps = _paramInfoMap.get(parameterName);
        if (paramProps == null) {
            if (parameterName.endsWith(MosaicConstants.PARAM_SUFFIX_EXPRESSION)) {
                paramProps = new ParamProperties(String.class, "");
            } else if (parameterName.endsWith(MosaicConstants.PARAM_SUFFIX_CONDITION)) {
                paramProps = new ParamProperties(Boolean.class, false);
            } else if (parameterName.endsWith(MosaicConstants.PARAM_SUFFIX_OUTPUT)) {
                paramProps = new ParamProperties(Boolean.class, false);
            }
        }
        if (paramProps == null) {
            throw new IllegalArgumentException("Invalid parameter name '" + parameterName + "'.");
        }
        return paramProps;
    }

    private ParamProperties createStringParamProperties() {
        return _defaultFactory.createStringParamProperties();
    }
}
