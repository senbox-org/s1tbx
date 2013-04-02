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
package org.esa.beam.processor.smac;

import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.param.validators.StringValidator;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for the <code>RequestLoader</code> to create validated SMAC parameters from a given request.
 * <p/>
 * This class implements the <code>RequestElementFactory</code> interface and can optionally be injected into the
 * <code>RequestLoader</code>. It enables the request loader to perform checks on parameter value ranges. Also, it
 * provides type information for the parameters.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class SmacRequestElementFactory implements RequestElementFactory {

    private final Map<String,ParamProperties> _paramInfoMap = new HashMap<String, ParamProperties>();

    private final DefaultRequestElementFactory _defaultFactory = DefaultRequestElementFactory.getInstance();

    /**
     * Singelton interface - retrieves the one and only instance of this class
     *
     * @return the singelton instance of this class
     */
    public static SmacRequestElementFactory getInstance() {
        return Holder.instance;
    }

    /**
     * Creates an input product reference given the input product URL, the product format and the product type.
     *
     * @param file   the file pointing to the input product
     * @param format the input product format
     * @param type   the input product type
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *          when the product reference cannot be created
     */
    public ProductRef createInputProductRef(File file, String format, String type) throws RequestElementFactoryException {
        return _defaultFactory.createInputProductRef(file, format, type);
    }

    /**
     * Creates an output product reference given the output product URL, the product format and the product type.
     *
     * @param file   the file pointing to the output product
     * @param format the output product format
     * @param type   the output product type
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *          when the product reference cannot be created
     */
    public ProductRef createOutputProductRef(File file, String format, String type) throws RequestElementFactoryException {
        return _defaultFactory.createOutputProductRef(file, format, type);
    }

    /**
     * Creates a <code>Parameter</code> given the parameter name and value. Checks the value for valid parameter range.
     *
     * @param name  the parameter name
     * @param value the parameter value as string
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *          when the parameter is not in the specified range or cannot be constructed due to other problems.
     * @throws IllegalArgumentException
     *          when the parameter name is not specified as valid name
     */
    public Parameter createParameter(String name, String value) throws RequestElementFactoryException {
        Guardian.assertNotNullOrEmpty("value", value);
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
     * Creates an output product parameter set to the default path.
     */
    public Parameter createDefaultOutputProductParameter() {
        return _defaultFactory.createDefaultOutputProductParameter();
    }

    /**
     * Creates a default logging pattern parameter set to the prefix passed in.
     *
     * @param prefix the default setting for the logging pattern
     * @return a logging pattern Parameter conforming the system settings
     */
    public Parameter createDefaultLogPatternParameter(final String prefix) {
        return _defaultFactory.createDefaultLogPatternParameter(prefix);
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
     * Creates a <code>Parameter</code> with the internally specified default value for the given name.
     *
     * @param name the parameter name
     * @throws IllegalArgumentException
     *          when the parameter name is not specified as valid names
     */
    public Parameter createParamWithDefaultValueSet(final String name) throws IllegalArgumentException {
        final ParamProperties paramProps = getParamProperties(name);
        if (SmacConstants.BITMASK_PARAM_NAME.equalsIgnoreCase(name) ||
                SmacConstants.BITMASK_NADIR_PARAM_NAME.equalsIgnoreCase(name) ||
                SmacConstants.BITMASK_FORWARD_PARAM_NAME.equalsIgnoreCase(name)) {

            paramProps.setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT, null);
        }
        final Parameter param = new Parameter(name, paramProps);
        param.setDefaultValue();
        return param;
    }

    /**
     * gets a default <code>ParamProperties</code> for the parameter with given name.
     *
     * @param parameterName the parameter name
     * @throws IllegalArgumentException
     *          when the parameter name is not specified as valid name
     */
    public ParamProperties getParamProperties(String parameterName) throws IllegalArgumentException {
        ParamProperties paramProps = _paramInfoMap.get(parameterName);
        if (paramProps == null) {
            throw new IllegalArgumentException("Invalid parameter name '" + parameterName + "'.");
        }
        return paramProps;
    }

    public ParamProperties createFileParamInfo(int fsm, Object value) {
        return _defaultFactory.createFileParamProperties(fsm, value);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs the object. Initializes the parameter map.
     */
    private SmacRequestElementFactory() {
        fillParamInfoMap();
    }

    private void fillParamInfoMap() {
        _paramInfoMap.put(SmacConstants.LOG_FILE_PARAM_NAME, createParamInfoLogFile());
        _paramInfoMap.put(SmacConstants.AEROSOL_TYPE_PARAM_NAME, createParamInfoAerosolType());
        _paramInfoMap.put(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME, createParamInfoAerosolOpticalDepth());
        _paramInfoMap.put(SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME, createParamInfoHorizontalVisibility());
        _paramInfoMap.put(SmacConstants.USE_MERIS_ADS_PARAM_NAME, createParamInfoUseMerisEcmwfData());
        _paramInfoMap.put(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME, createParamInfoSurfaceAirPressure());
        _paramInfoMap.put(SmacConstants.OZONE_CONTENT_PARAM_NAME, createParamInfoOzoneContent());
        _paramInfoMap.put(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME, createParamInfoRelativeHumidity());
        _paramInfoMap.put(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME,
                          createParamInfoDefaultReflectance());
        _paramInfoMap.put(SmacConstants.BANDS_PARAM_NAME, createParamInfoBands());
        _paramInfoMap.put(SmacConstants.PRODUCT_TYPE_PARAM_NAME, createParamInfoProductType());
        _paramInfoMap.put(SmacConstants.BITMASK_PARAM_NAME, createParamInfoMerisBitmaskType());
        _paramInfoMap.put(SmacConstants.BITMASK_FORWARD_PARAM_NAME, createParamInfoForwardBitmaskType());
        _paramInfoMap.put(SmacConstants.BITMASK_NADIR_PARAM_NAME, createParamInfoNadirBitmaskType());
    }

    private ParamProperties createParamInfoMerisBitmaskType() {
        ParamProperties paramProps = _defaultFactory.createBitmaskParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_MERIS_FLAGS_VALUE);
        paramProps.setValueSet(SmacConstants.DEFAULT_MERIS_FLAGS_VALUESET);
        paramProps.setLabel(SmacConstants.DEFAULT_MERIS_BITMASK_LABEL);
        paramProps.setDescription(SmacConstants.DEFAULT_MERIS_BITMASK_DESCRIPTION);
        return paramProps;
    }

    private ParamProperties createParamInfoForwardBitmaskType() {
        ParamProperties paramProps = _defaultFactory.createBitmaskParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE);
        paramProps.setValueSet(SmacConstants.DEFAULT_FORWARD_FLAGS_VALUESET);
        paramProps.setLabel(SmacConstants.DEFAULT_AATSR_FORWARD_LABEL);
        paramProps.setDescription(SmacConstants.DEFAULT_AATSR_FORWARD_DESCRIPTION);
        return paramProps;
    }

    private ParamProperties createParamInfoNadirBitmaskType() {
        ParamProperties paramProps = _defaultFactory.createBitmaskParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_NADIR_FLAGS_VALUE);
        paramProps.setValueSet(SmacConstants.DEFAULT_NADIR_FLAGS_VALUESET);
        paramProps.setLabel(SmacConstants.DEFAULT_AATSR_NADIR_LABEL);
        paramProps.setDescription(SmacConstants.DEFAULT_AATSR_NADIR_DESCRIPTION);
        return paramProps;
    }

    private ParamProperties createParamInfoAerosolType() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setValueSetBound(true);
        paramProps.setValueSet(SmacConstants.DEFAULT_AER_TYPE_VALUESET);
        paramProps.setLabel(SmacConstants.DEFAULT_AER_TYPE_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_AER_TYPE_DESCRIPTION);
        paramProps.setDefaultValue(SmacConstants.DEFAULT_AER_TYPE_DEFAULTVALUE);
        return paramProps;
    }

    private ParamProperties createParamInfoProductType() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
//        paramProps.setValueSetBound(true);
        paramProps.setValidatorClass(SmacProductTypeValidator.class);
//        paramProps.setValueSet(SmacConstants.DEFAULT_PRODUCT_TYPE_VALUESET);  // can be removed
        paramProps.setLabel(SmacConstants.DEFAULT_PRODUCT_TYPE_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_PRODUCT_TYPE_DESCRIPTION);
        paramProps.setDefaultValue(SmacConstants.DEFAULT_PRODUCT_TYPE_DEFAULTVALUE);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter "BANDS_PARAM_NAME"
     */
    private ParamProperties createParamInfoBands() {
        ParamProperties paramProps = _defaultFactory.createStringArrayParamProperties();
        paramProps.setLabel(SmacConstants.DEFAULT_BANDS_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_BANDS_DESCRIPTION);
        paramProps.setNullValueAllowed(true);
        // leave initially empty - will be filled when product is scanned
        paramProps.setValueSet(SmacConstants.DEFAULT_BANDS_VALUESET);
        return paramProps;
    }

    private ParamProperties createParamInfoUseMerisEcmwfData() {
        ParamProperties paramProps = _defaultFactory.createBooleanParamProperties();
        paramProps.setDefaultValue(true);
        paramProps.setLabel(SmacConstants.DEFAULT_USEMERIS_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_USEMERIS_DESCRIPTION);
        return paramProps;
    }

    private ParamProperties createParamInfoDefaultReflectance() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_DEFREFLECT_DEFAULTVALUE);
        paramProps.setLabel(SmacConstants.DEFAULT_DEFREFLECT_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_DEFREFLECT_DESCRIPTION);
        paramProps.setMaxValue(SmacConstants.DEFAULT_DEFREFLECT_MAXVALUE);
        paramProps.setPhysicalUnit(SmacConstants.DEFAULT_DEFREFLECT_VALUEUNIT);
        return paramProps;
    }

    private ParamProperties createParamInfoRelativeHumidity() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_H2O_DEFAULTVALUE);
        paramProps.setMaxValue(SmacConstants.DEFAULT_H2O_MAXVALUE);
        paramProps.setLabel(SmacConstants.DEFAULT_H2O_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_H2O_DESCRIPTION);
        paramProps.setPhysicalUnit(SmacConstants.DEFAULT_H2O_VALUEUNIT);
        return paramProps;
    }

    private ParamProperties createParamInfoOzoneContent() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_OZONECONTENT_DEFAULTVALUE);
        paramProps.setMaxValue(SmacConstants.DEFAULT_OZONECONTENT_MAXVALUE);
        paramProps.setLabel(SmacConstants.DEFAULT_OZONECONTENT_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_OZONECONTENT_DESCRIPTION);
        paramProps.setPhysicalUnit(SmacConstants.DEFAULT_OZONECONTENT_VALUEUNIT);
        return paramProps;
    }

    private ParamProperties createParamInfoSurfaceAirPressure() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_SURF_AIR_PRESS_DEFAULTVALUE);
        paramProps.setMinValue(SmacConstants.DEFAULT_SURF_AIR_PRESS_MINVALUE);
        paramProps.setMaxValue(SmacConstants.DEFAULT_SURF_AIR_PRESS_MAXVALUE);
        paramProps.setLabel(SmacConstants.DEFAULT_SURF_AIR_PRESS_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_SURF_AIR_PRESS_DESCRIPTION);
        paramProps.setPhysicalUnit(SmacConstants.DEFAULT_SURF_AIR_PRESS_VALUEUNIT);
        return paramProps;
    }

    private ParamProperties createParamInfoHorizontalVisibility() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_HORIZ_VIS_DEFAULTVALUE);
        paramProps.setMinValue(SmacConstants.DEFAULT_MIN_HORIZ_VIS_MINVALUE);
        paramProps.setMaxValue(SmacConstants.DEFAULT_MAX_HORIZ_VIS_MAXVALUE);
        paramProps.setLabel(SmacConstants.DEFAULT_MAX_HORIZ_VIS_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_MAX_HORIZ_VIS_DESCRIPTION);
        paramProps.setPhysicalUnit(SmacConstants.DEFAULT_MAX_HORIZ_VIS_VALUEUNIT);
        return paramProps;
    }

    private ParamProperties createParamInfoAerosolOpticalDepth() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(SmacConstants.DEFAULT_AER_OPT_DEPTH_DEFAULTVALUE);
        paramProps.setMinValue(SmacConstants.DEFAULT_AER_OPT_DEPTH_MINVALUE);
        paramProps.setMaxValue(SmacConstants.DEFAULT_AER_OPT_DEPTH_MAXVALUE);
        paramProps.setLabel(SmacConstants.DEFAULT_AER_OPT_DEPTH_LABELTEXT);
        paramProps.setDescription(SmacConstants.DEFAULT_AER_OPT_DEPTH_DESCRIPTION);
        paramProps.setPhysicalUnit(SmacConstants.DEFAULT_AER_OPT_DEPTH_VALUEUNIT);
        return paramProps;
    }

    private ParamProperties createParamInfoLogFile() {
        int fsm = SmacConstants.DEFAULT_LOG_FILE_FILESELECTIONMODE;
        String curWorkDir = SystemUtils.getCurrentWorkingDir().toString();
        String defaultValue = SystemUtils.convertToLocalPath(
                curWorkDir + "/" + SmacConstants.DEFAULT_LOG_FILE_FILENAME);
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(fsm, defaultValue);
        paramProps.setLabel(SmacConstants.LOG_FILE_LABELTEXT);
        paramProps.setDescription(SmacConstants.LOG_FILE_DESCRIPTION);
        return paramProps;
    }

    public static class SmacProductTypeValidator extends StringValidator {

        @Override
        public void validate(Parameter parameter, Object value) throws ParamValidateException {
            if(String.class.isInstance(value)) {
                String productType = (String) value;
                if(!SmacUtils.isSupportedProductType(productType)) {
                    throw new ParamValidateException(parameter,
                                                     SmacConstants.LOG_MSG_UNSUPPORTED_INPUT_1 + productType + SmacConstants.LOG_MSG_UNSUPPORTED_INPUT_2);
                }
            }
        }
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final SmacRequestElementFactory instance = new SmacRequestElementFactory();
    }
}
