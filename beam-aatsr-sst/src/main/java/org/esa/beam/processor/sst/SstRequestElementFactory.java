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
package org.esa.beam.processor.sst;

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the core parameter creation and definition functionality. It defines: <ul> <li> all valid
 * parameter names <li> all parameter ranges defined (if any) <li> the UI editors for the parameters <li> and the
 * parameter default values </ul>
 * <p/>
 * This class is responsible for checking any request file loaded for valid content. It is passed to the request loader
 * to do this.
 * <p/>
 * It is implemented as a singleton
 *
 * @deprecated since BEAM 4.10 - no replacement.
 */
@Deprecated
public class SstRequestElementFactory implements RequestElementFactory {

    private final DefaultRequestElementFactory _defaultFactory;
    private final Map _paramInfoMap;

    /**
     * Singleton interface.
     */
    public static SstRequestElementFactory getInstance() {
        return Holder.instance;
    }

    /**
     * Creates a new reference to an input product for the current processing request.
     *
     * @param file       the input product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     *
     * @throws java.lang.IllegalArgumentException
     *          if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *          if the element could not be created
     */
    public ProductRef createInputProductRef(File file, String fileFormat, String typeId)
            throws RequestElementFactoryException {
        return _defaultFactory.createInputProductRef(file, fileFormat, typeId);
    }

    /**
     * Creates a new reference to an output product for the current processing request.
     *
     * @param file       the output product's file, must not be <code>null</code>
     * @param fileFormat the file format, can be <code>null</code> if not known
     * @param typeId     the product type identifier, can be <code>null</code> if not known
     *
     * @throws java.lang.IllegalArgumentException
     *          if <code>url</code> is <code>null</code>
     * @throws org.esa.beam.framework.processor.RequestElementFactoryException
     *          if the element could not be created
     */
    public ProductRef createOutputProductRef(File file, String fileFormat, String typeId)
            throws RequestElementFactoryException {
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
     * @return the created logging to output product parameter.
     */
    public Parameter createLogToOutputParameter(final String value) throws ParamValidateException {
        return _defaultFactory.createLogToOutputParameter(value);
    }

    /**
     * Creates a parameter with the name passed in by searching the parameter properties for the given name. Once found,
     * creates the parameter according to the properties and initializes it with the default value
     */
    public Parameter createParamWithDefaultValueSet(final String paramName) {
        final ParamProperties paramProps = getParamProperties(paramName);
        if (SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME.equalsIgnoreCase(paramName) ||
            SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME.equalsIgnoreCase(paramName)) {
            paramProps.setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT, null);
        }

        final Parameter param = new Parameter(paramName, paramProps);
        param.setDefaultValue();
        return param;
    }

    /**
     * Creates an input product parameter set to the default path.
     */
    public Parameter createDefaultInputProductParameter() {
        return _defaultFactory.createDefaultInputProductParameter();
    }

    /**
     * Creates an output product parameter set to the default path.
     */
    public Parameter createDefaultOutputProductParameter() {
        final File defaultOutFile = new File(SystemUtils.getUserHomeDir(), SstConstants.DEFAULT_FILE_NAME);
        final ParamProperties paramProps = _defaultFactory.createFileParamProperties(ParamProperties.FSM_FILES_ONLY,
                                                                                     defaultOutFile);
        paramProps.setLabel(SstConstants.OUTPUT_PRODUCT_LABELTEXT);
        paramProps.setDescription(SstConstants.OUTPUT_PRODUCT_DESCRIPTION);
        final Parameter param = new Parameter(SstConstants.OUTPUT_PRODUCT_PARAM_NAME, paramProps);
        param.setDefaultValue();
        return param;
    }

    /**
     * Creates a logging file parameter set to default value
     */
    public Parameter createDefaultLogfileParameter() {
        String curWorkDir = SystemUtils.getCurrentWorkingDir().toString();
        String defaultValue = SystemUtils.convertToLocalPath(curWorkDir + "/" + SstConstants.DEFAULT_LOG_FILE_FILENAME);
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(ParamProperties.FSM_FILES_ONLY,
                                                                               defaultValue);
        paramProps.setLabel(SstConstants.LOG_FILE_LABELTEXT);
        paramProps.setDescription(SstConstants.LOG_FILE_DESCRIPTION);
        Parameter _param = new Parameter(SstConstants.LOG_FILE_PARAM_NAME, paramProps);
        _param.setDefaultValue();
        return _param;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Construction - private - singleton
     */
    private SstRequestElementFactory() {
        _defaultFactory = DefaultRequestElementFactory.getInstance();
        _paramInfoMap = new HashMap();
        fillParamInfoMap();
    }

    /**
     * Initializes the parameter information map with all parameter information we have available.
     */
    private void fillParamInfoMap() {
        _paramInfoMap.put(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME, createProcessDualViewInfo());
        _paramInfoMap.put(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME, createDualViewCoeffFileInfo());
        _paramInfoMap.put(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME, createDualViewCoeffFileDescriptionInfo());
        _paramInfoMap.put(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME, createDualViewBitmaskInfo());
        _paramInfoMap.put(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME, createProcessNadirViewInfo());
        _paramInfoMap.put(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME, createNadirViewCoeffFileInfo());
        _paramInfoMap.put(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME, createNadirViewCoeffFileDescriptionInfo());
        _paramInfoMap.put(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME, createNadirViewBitmaskInfo());
        _paramInfoMap.put(SstConstants.INVALID_PIXEL_PARAM_NAME, createInvalidPixelInfo());
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
            throw new IllegalArgumentException("Invalid parameter name: '" + name + "'");
        }
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter "process dual view sst".
     */
    private ParamProperties createProcessDualViewInfo() {
        ParamProperties paramProps = _defaultFactory.createBooleanParamProperties();
        paramProps.setLabel(SstConstants.PROCESS_DUAL_VIEW_SST_LABELTEXT);
        paramProps.setDescription(SstConstants.PROCESS_DUAL_VIEW_SST_DESCRIPTION);
        paramProps.setDefaultValue(SstConstants.DEFAULT_PROCESS_DUAL_VIEW_SST);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter dual view coefficient file
     */
    private ParamProperties createDualViewCoeffFileInfo() {
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(ParamProperties.FSM_FILES_ONLY,
                                                                               SstConstants.DEFAULT_DUAL_VIEW_COEFF_FILE);
        paramProps.setLabel(SstConstants.DUAL_VIEW_COEFF_FILE_LABELTEXT);
        paramProps.setDescription(SstConstants.DUAL_VIEW_COEFF_FILE_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter dual view coefficient file description
     */
    private ParamProperties createDualViewCoeffFileDescriptionInfo() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setLabel(SstConstants.DUAL_VIEW_COEFF_FILE_LABELTEXT);
        paramProps.setDefaultValue(SstConstants.DEFAULT_DUAL_VIEW_COEFF_FILE);
        paramProps.setDescription(SstConstants.DUAL_VIEW_COEFF_FILE_DESCRIPTION);
        paramProps.setEditorClass(ComboBoxEditor.class);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter dual view bitmask
     */
    private ParamProperties createDualViewBitmaskInfo() {
        ParamProperties paramProps = _defaultFactory.createBitmaskParamProperties();
        paramProps.setLabel(SstConstants.DUAL_VIEW_BITMASK_LABELTEXT);
        paramProps.setDescription(SstConstants.DUAL_VIEW_BITMASK_DESCRIPTION);
        paramProps.setDefaultValue(SstConstants.DEFAULT_DUAL_VIEW_BITMASK);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter "process nadir view sst"
     */
    private static ParamProperties createProcessNadirViewInfo() {
        ParamProperties paramProps = new ParamProperties();
        paramProps.setValueType(Boolean.class);
        paramProps.setLabel(SstConstants.PROCESS_NADIR_VIEW_SST_LABELTEXT);
        paramProps.setDescription(SstConstants.PROCESS_NADIR_VIEW_SST_DESCRIPTION);
        paramProps.setDefaultValue(SstConstants.DEFAULT_PROCESS_NADIR_VIEW_SST);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter nadir view coefficient file
     */
    private ParamProperties createNadirViewCoeffFileInfo() {
        ParamProperties paramProps = _defaultFactory.createFileParamProperties(ParamProperties.FSM_FILES_ONLY,
                                                                               SstConstants.DEFAULT_NADIR_VIEW_COEFF_FILE);
        paramProps.setLabel(SstConstants.NADIR_VIEW_COEFF_FILE_LABELTEXT);
        paramProps.setDescription(SstConstants.NADIR_VIEW_COEFF_FILE_DESCRIPTION);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter nadir view coefficient file description
     */
    private ParamProperties createNadirViewCoeffFileDescriptionInfo() {
        ParamProperties paramProps = _defaultFactory.createStringParamProperties();
        paramProps.setLabel(SstConstants.NADIR_VIEW_COEFF_FILE_LABELTEXT);
        paramProps.setDefaultValue(SstConstants.DEFAULT_NADIR_VIEW_COEFF_FILE);
        paramProps.setDescription(SstConstants.NADIR_VIEW_COEFF_FILE_DESCRIPTION);
        paramProps.setEditorClass(ComboBoxEditor.class);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter nadir view bitmask
     */
    private ParamProperties createNadirViewBitmaskInfo() {
        ParamProperties paramProps = _defaultFactory.createBitmaskParamProperties();
        paramProps.setLabel(SstConstants.NADIR_VIEW_BITMASK_LABELTEXT);
        paramProps.setDescription(SstConstants.NADIR_VIEW_BITMASK_DESCRIPTION);
        paramProps.setDefaultValue(SstConstants.DEFAULT_NADIR_VIEW_BITMASK);
        return paramProps;
    }

    /**
     * Creates the parameter information for the parameter invalid pixel
     */
    private ParamProperties createInvalidPixelInfo() {
        ParamProperties paramProps = new ParamProperties(Float.class);
        paramProps.setLabel(SstConstants.INVALID_PIXEL_LABELTEXT);
        paramProps.setDescription(SstConstants.INVALID_PIXEL_DESCRIPTION);
        paramProps.setDefaultValue(SstConstants.DEFAULT_INVALID_PIXEL);
        return paramProps;
    }

    // Initialization on demand holder idiom
    private static class Holder {

        private static final SstRequestElementFactory instance = new SstRequestElementFactory();
    }
}
