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
package org.esa.beam.processor.flh_mci;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.param.editors.ComboBoxEditor;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;

/**
 * This class contains the core parameter creation and definfition functionality. It defines: <ul> <li> all valid
 * parameter names <li> all parameter ranges defined (if any) <li> the UI editors for the parameters <li> and the
 * parameter default values </ul>
 * <p/>
 * This class is responsible for checking any request file loaded for valid content. It is passed to the request loader
 * to do this.
 */
@Deprecated
public class FlhMciRequestElementFactory implements RequestElementFactory {

    private final Map<String,ParamProperties> _paramInfoMap = new HashMap<String, ParamProperties>();
    private final DefaultRequestElementFactory _defaultFactory = DefaultRequestElementFactory.getInstance();


    /**
     * Singleton interface.
     */
    public static FlhMciRequestElementFactory getInstance() {
        return Holder.instance;
    }

    /**
     * {@inheritDoc}
     */
    public ProductRef createInputProductRef(File file, String fileFormat, String typeId) throws RequestElementFactoryException {
        return _defaultFactory.createInputProductRef(file, fileFormat, typeId);
    }

    /**
     * {@inheritDoc}
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
     * Creates a default logging pattern parameter set to the prefix passed in.
     *
     * @param prefix the default setting for the logging pattern
     *
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
     * @param paramName the parameter name
     *
     * @throws java.lang.IllegalArgumentException
     *          when the parameter name is not specified as valid names
     */
    public Parameter createParamWithDefaultValueSet(String paramName) {
        ParamProperties paramProps = getParamInfo(paramName);
        if (FlhMciConstants.BITMASK_PARAM_NAME.equalsIgnoreCase(paramName)) {
            paramProps.setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT, null);
        }
        Parameter param = new Parameter(paramName, paramProps);
        param.setDefaultValue();
        return param;
    }

    /**
     * Creates an output product parameter set to the default path.
     */
    public Parameter createDefaultOutputProductParameter() {
        File defaultOutProduct = new File(SystemUtils.getUserHomeDir(), FlhMciConstants.DEFAULT_FILE_NAME);
        int fsm = ParamProperties.FSM_FILES_ONLY;
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(fsm, defaultOutProduct);
        paramProps.setLabel(FlhMciConstants.OUTPUT_PRODUCT_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.OUTPUT_PRODUCT_DESCRIPTION);
        Parameter _param = new Parameter(FlhMciConstants.OUTPUT_PRODUCT_PARAM_NAME, paramProps);
        _param.setDefaultValue();
        return _param;
    }

    /**
     * Creates a logging file parameter set to default value
     */
    public Parameter createDefaultLogFileParameter() {
        // @todo 1 nf/tb - convert to log-file extension
        int fsm = FlhMciConstants.DEFAULT_LOG_FILE_FILESELECTIONMODE;
        String curWorkDir = SystemUtils.getCurrentWorkingDir().toString();
        String defaultValue = SystemUtils.convertToLocalPath(
                curWorkDir + "/" + FlhMciConstants.DEFAULT_LOG_FILE_FILENAME);
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(fsm, defaultValue);
        paramProps.setLabel(FlhMciConstants.LOG_FILE_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.LOG_FILE_DESCRIPTION);
        Parameter _param = new Parameter(FlhMciConstants.LOG_FILE_PARAM_NAME, paramProps);
        _param.setDefaultValue();
        return _param;
    }

    ///////////////////////////////////////////////////////////////////////////
    ///////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Constructs the object. Initializes the parameter map.
     */
    private FlhMciRequestElementFactory() {
        fillParamInfoMap();
    }

    /**
     * gets a default <code>ParamProperties</code> for the parameter with given name.
     *
     * @param parameterName the parameter name
     *
     * @throws java.lang.IllegalArgumentException
     *          when the parameter name is not specified as valid name
     */
    private ParamProperties getParamInfo(String parameterName) throws IllegalArgumentException {
        ParamProperties paramProps = _paramInfoMap.get(parameterName);
        if (paramProps == null) {
            throw new IllegalArgumentException("Invalid parameter name '" + parameterName + "'.");
        }
        return paramProps;
    }

    /**
     * Fills the internal <code>HashMap</code> with the valid parameter infos for all parameter supported by this class
     */
    private void fillParamInfoMap() {
        _paramInfoMap.put(FlhMciConstants.REQUEST_TYPE_PARAM_NAME, createParamInfoRequestType());
        _paramInfoMap.put(FlhMciConstants.PRESET_PARAM_NAME, createParamInfoPreset());
        _paramInfoMap.put(FlhMciConstants.BAND_LOW_PARAM_NAME, createParamInfoBandLow());
        _paramInfoMap.put(FlhMciConstants.BAND_SIGNAL_PARAM_NAME, createParamInfoBandSignal());
        _paramInfoMap.put(FlhMciConstants.BAND_HIGH_PARAM_NAME, createParamInfoBandHigh());
        _paramInfoMap.put(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME, createParamInfoLineheightBandName());
        _paramInfoMap.put(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME, createParamInfoProcessSlope());
        _paramInfoMap.put(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME, createParamInfoSlopeBandName());
        _paramInfoMap.put(FlhMciConstants.BITMASK_PARAM_NAME, createParamInfoBitmask());
        _paramInfoMap.put(FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME, createParamInfoInvalidPixel());
        _paramInfoMap.put(FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME, createParamInfoCloudCorrection());
    }


    /**
     * Creates the parameter information for the request parameter <code>BAND_LOW_PARAM_NAME</code>
     */
    private ParamProperties createParamInfoBandLow() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_BAND_LOW);
        paramProps.setValueSet(new String[]{FlhMciConstants.DEFAULT_BAND_LOW});
        paramProps.setLabel(FlhMciConstants.DEFAULT_BAND_LOW_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.DEFAULT_BAND_LOW_DESCRIPTION);
        paramProps.setPhysicalUnit(FlhMciConstants.DEFAULT_BAND_VALUEUNIT);
        paramProps.setEditorClass(ComboBoxEditor.class);
        return paramProps;
    }

    /**
     * Creates the parameter information for the request parameter <code>BAND_SIGNAL_PARAM_NAME</code>
     */
    private ParamProperties createParamInfoBandSignal() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_BAND_SIGNAL);
        paramProps.setValueSet(new String[]{FlhMciConstants.DEFAULT_BAND_SIGNAL});
        paramProps.setLabel(FlhMciConstants.DEFAULT_BAND_SIGNAL_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.DEFAULT_BAND_SIGNAL_DESCRIPTION);
        paramProps.setPhysicalUnit(FlhMciConstants.DEFAULT_BAND_VALUEUNIT);
        paramProps.setEditorClass(ComboBoxEditor.class);
        return paramProps;
    }

    /**
     * Creates the parameter information for the request parameter <code>BAND_HIGH_PARAM_NAME</code>
     */
    private ParamProperties createParamInfoBandHigh() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_BAND_HIGH);
        paramProps.setValueSet(new String[]{FlhMciConstants.DEFAULT_BAND_HIGH});
        paramProps.setLabel(FlhMciConstants.DEFAULT_BAND_HIGH_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.DEFAULT_BAND_HIGH_DESCRIPTION);
        paramProps.setPhysicalUnit(FlhMciConstants.DEFAULT_BAND_VALUEUNIT);
        paramProps.setEditorClass(ComboBoxEditor.class);
        return paramProps;
    }

    /**
     * Creates a parameter information for the request parameter for the lineheight band name
     */
    private ParamProperties createParamInfoLineheightBandName() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_LINE_HEIGHT_BAND_NAME);
        paramProps.setLabel(FlhMciConstants.LINEHEIGHT_BAND_NAME_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.LINEHEIGHT_BAND_NAME_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates a parameter information for the request parameter process slope
     */
    private ParamProperties createParamInfoProcessSlope() {
        ParamProperties paramProps = _defaultFactory.createBooleanParamProperties();
        paramProps.setDefaultValue(new Boolean(FlhMciConstants.DEFAULT_PROCESS_SLOPE));
        paramProps.setLabel(FlhMciConstants.PROCESS_SLOPE_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.PROCESS_SLOPE_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates a parameter information for the request parameter slope band name
     */
    private ParamProperties createParamInfoSlopeBandName() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_SLOPE_BAND_NAME);
        paramProps.setLabel(FlhMciConstants.SLOPE_BAND_NAME_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.SLOPE_BAND_NAME_LABELTEXT);
        return paramProps;
    }

    /**
     * Creates a parameter information for the request parameter "bitmask".
     */
    private ParamProperties createParamInfoBitmask() {
        final ParamProperties paramProps = _defaultFactory.createBitmaskParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_BITMASK);
        paramProps.setLabel(FlhMciConstants.BITMASK_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.BITMASK_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates a parameter information for the request parameter for invalid pixel value
     */
    private ParamProperties createParamInfoInvalidPixel() {
        ParamProperties paramProps = _defaultFactory.createBoundFloatParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE);
        paramProps.setLabel(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE_DESCRIPTION);
        paramProps.setPhysicalUnit(FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE_VALUEUNIT);
        return paramProps;
    }

    /**
     * Creates a parameter information for the request parameter for cloud correction
     */
    private ParamProperties createParamInfoCloudCorrection() {
        ParamProperties paramProps = _defaultFactory.createFloatParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.DEFAULT_CLOUD_CORRECTION_FACTOR);
        paramProps.setLabel(FlhMciConstants.CLOUD_CORRECTION_FACTOR_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.CLOUD_CORRECTION_FACTOR_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter request type
     */
    private ParamProperties createParamInfoRequestType() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.REQUEST_TYPE);
        paramProps.setValueSet(new String[]{FlhMciConstants.REQUEST_TYPE});
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter preset
     */
    private ParamProperties createParamInfoPreset() {
        ParamProperties paramProps = _defaultFactory.createStringArrayParamProperties();
        paramProps.setDefaultValue(FlhMciConstants.PRESET_PARAM_DEFAULT_VALUE);
        paramProps.setValueSet(FlhMciConstants.PRESET_PARAM_VALUE_SET);
        paramProps.setLabel(FlhMciConstants.PRESET_PARAM_LABELTEXT);
        paramProps.setDescription(FlhMciConstants.PRESET_PARAM_DESCRIPTION);
        paramProps.setPhysicalUnit(FlhMciConstants.PRESET_PARAM_VALUE_UNIT);
        paramProps.setEditorClass(ComboBoxEditor.class);
        paramProps.setValueSetBound(true);
        return paramProps;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final FlhMciRequestElementFactory instance = new FlhMciRequestElementFactory();
    }
}
