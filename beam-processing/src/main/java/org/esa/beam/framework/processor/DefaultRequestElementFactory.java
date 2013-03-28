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
package org.esa.beam.framework.processor;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.param.ParamParseException;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.param.editors.FileEditor;
import org.esa.beam.framework.param.validators.BooleanExpressionValidator;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * The request element factory is responsible for creating <code>Parameters</code> with predefined datatypes, validity
 * ranges and appropriate editors (i.e. UI elements). The request element factory is invoked by
 * <code>RequestLoader</code> to assemble <code>Request</code>s and validate the datat coming from a request file.
 * <p/>
 * This class is the default implementation. It does no validation on parameters and only supports default editors.
 * Overwrite this class to perform specific implementation.
 * <p/>
 * Also, this class implements some helper functions for creating standard parameters of type <ul> <li>file parameter
 * <li>bitmask parameter <li>float value parameter and <li>string array parameter </ul>
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class DefaultRequestElementFactory implements RequestElementFactory, ProcessorConstants {

    /**
     * Retrieve an instance of the factory. This is a singleton object.
     *
     * @return a reference to the one and only instance of the factory
     */
    public static DefaultRequestElementFactory getInstance() {
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
        return createProductRef(file, fileFormat, typeId);
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
        return createProductRef(file, fileFormat, typeId);
    }

    /**
     * Creates a <code>Parameter</code> of type <code>String</code> with the given name and the given
     * <code>String</code> value.
     *
     * @param name  the parameter name
     * @param value the value of the parameter
     */
    public Parameter createParameter(String name, String value) throws RequestElementFactoryException {
        Guardian.assertNotNullOrEmpty("name", name);
        return new Parameter(name, value, new ParamProperties(String.class));
    }

    /**
     * Creates a default logging pattern parameter set to the prefix passed in.
     *
     * @param prefix the default setting for the logging pattern
     *
     * @return a logging pattern Parameter conforming the system settings
     */
    public Parameter createDefaultLogPatternParameter(String prefix) {
        Guardian.assertNotNull("prefix", prefix);
        ParamProperties props = createStringParamProperties();
        Parameter param;

        props.setLabel(LOG_PREFIX_LABELTEXT);
        props.setDescription(LOG_PREFIX_DESCRIPTION);
        props.setDefaultValue(prefix);

        param = new Parameter(LOG_PREFIX_PARAM_NAME, props);
        param.setDefaultValue();

        return param;
    }

    /**
     * Creates a logging to output product parameter with the default value <code>true</code>.
     * The current value is set to the given parameter <code>value</code>.
     *
     * @param value the value as <code>String</code> this parameter is set to.
     *
     * @return a logging to output product parameter
     */
    public Parameter createLogToOutputParameter(final String value) throws ParamValidateException {
        final ParamProperties props = createBooleanParamProperties();
        final Parameter param;

        props.setLabel(LOG_TO_OUTPUT_LABELTEXT);
        props.setDescription(LOG_TO_OUTPUT_DESCRIPTION);
        props.setDefaultValue(true);

        param = new Parameter(LOG_TO_OUTPUT_PARAM_NAME, props);
        try {
            param.setValueAsText(value);
        } catch (ParamParseException e) {
            throw new ParamValidateException(param, e.getMessage());
        }
        return param;
    }

    /**
     * Creates a default <code>ParamProperties</code> for file parameters with given file selection mode and given
     * default value.
     *
     * @param fileSelectionMode the file selection mode, can be FSM_FILES_ONLY, FSM_DIRECTORIES_ONLY and
     *                          FSM_FILES_AND_DIRECTORIES defined in {@link ParamProperties}.
     * @param defaultValue      the default value wich was set at the returned <code>ParamProperties</code>
     *
     * @throws IllegalArgumentException if the parameter <code>fileSelectionMode</code> is not a valid selection mode:
     *                                  Valid file selection modes are FSM_FILES_ONLY, FSM_DIRECTORIES_ONLY and
     *                                  FSM_FILES_AND_DIRECTORIES
     */
    public ParamProperties createFileParamProperties(int fileSelectionMode, Object defaultValue) {
        ParamProperties paramProps = new ParamProperties(File.class);
        paramProps.setEditorClass(FileEditor.class);
        paramProps.setFileSelectionMode(fileSelectionMode);
        paramProps.setDefaultValue(defaultValue);
        return paramProps;
    }

    /**
     * Creates a default parameter info type for parameters of type bitmask expression.
     */
    public ParamProperties createBitmaskParamProperties() {
        ParamProperties paramProps = new ParamProperties();
        paramProps.setValidatorClass(BooleanExpressionValidator.class);
        paramProps.setEditorClass(BooleanExpressionEditor.class);
        return paramProps;
    }

    /**
     * Creates a default parameter info type for parameters of type bound float. These parameters check for the value to
     * value >= 0.f
     */
    public ParamProperties createBoundFloatParamProperties() {
        return new ParamProperties(Float.class, null, 0.f, null);
    }

    /**
     * Creates a default parameter info type for parameters of type float
     */
    public ParamProperties createIntegerParamProperties() {
        return new ParamProperties(Integer.class);
    }

    /**
     * Creates a default parameter info type for parameters of type float
     */
    public ParamProperties createFloatParamProperties() {
        return new ParamProperties(Float.class);
    }

    /**
     * Creates a string array value parameter information.
     */
    public ParamProperties createStringArrayParamProperties() {
        return new ParamProperties(String[].class);
    }

    /**
     * Creates default string value parameter properties.
     */
    public ParamProperties createStringParamProperties() {
        return new ParamProperties(String.class);
    }

    /**
     * Creates default boolean parameter properties.
     *
     * @return default boolean parameter properties.
     */
    public ParamProperties createBooleanParamProperties() {
        return new ParamProperties(Boolean.class);
    }

    /**
     * Creates an input product parameter set to the default path.
     */
    public Parameter createDefaultInputProductParameter() {
        File defaultInPath = new File("");
        int fileSelectionMode = ParamProperties.FSM_FILES_ONLY;
        ParamProperties paramProps = createFileParamProperties(fileSelectionMode, defaultInPath);
        paramProps.setLabel(INPUT_PRODUCT_LABELTEXT);
        paramProps.setDescription(INPUT_PRODUCT_DESCRIPTION);
        paramProps.setChoosableFileFilters(getDefaultFileFilter());
        Parameter param = new Parameter(INPUT_PRODUCT_PARAM_NAME, paramProps);
        param.setDefaultValue();
        return param;
    }

    /**
     * Creates an output product parameter set to the current user's home directory.
     */
    public Parameter createDefaultOutputProductParameter() {
        File defaultOutPath = new File(SystemUtils.getUserHomeDir().getPath());
        int fsm = ParamProperties.FSM_FILES_ONLY;
        ParamProperties paramProps = createFileParamProperties(fsm, defaultOutPath);
        paramProps.setLabel(OUTPUT_PRODUCT_LABELTEXT);
        paramProps.setDescription(OUTPUT_PRODUCT_DESCRIPTION);
        Parameter param = new Parameter(OUTPUT_PRODUCT_PARAM_NAME, paramProps);
        param.setDefaultValue();
        return param;
    }

    /**
     * Creates an output product format parameter with all the registered product format names
     */
    public Parameter createOutputFormatParameter() {
        ProductIOPlugInManager instance = ProductIOPlugInManager.getInstance();
        String[] formats = instance.getAllProductWriterFormatStrings();
        ParamProperties paramProperties = createStringParamProperties();
        if (formats.length > 0) {
            paramProperties.setDefaultValue(formats[0]);
        }
        paramProperties.setValueSet(formats);
        paramProperties.setValueSetBound(true);
        paramProperties.setReadOnly(true);
        paramProperties.setLabel(ProcessorConstants.OUTPUT_FORMAT_LABELTEXT);
        paramProperties.setDescription(ProcessorConstants.OUTPUT_FORMAT_DESCRIPTION);
        paramProperties.setDefaultValue(DimapProductConstants.DIMAP_FORMAT_NAME);
        return new Parameter(ProcessorConstants.OUTPUT_FORMAT_PARAM_NAME, paramProperties);
    }

    private ProductRef createProductRef(File file, String format, String type) {
        Guardian.assertNotNull("file", file);
        return new ProductRef(file, format, type);
    }

    private FileFilter[] getDefaultFileFilter() {
        FileFilter[] filters = new FileFilter[3];

        filters[0] = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName().toLowerCase();
                return pathname.isDirectory() || name.endsWith(".n1") || name.endsWith(".prd");
            }

            @Override
            public String getDescription() {
                return "Envisat product files (*.N1, *.prd)"; /*I18N*/
            }
        };

        filters[1] = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName().toLowerCase();
                return pathname.isDirectory() || name.endsWith(".dim");
            }

            @Override
            public String getDescription() {
                return "BEAM DIMAP Files (*.dim)"; /*I18N*/
            }
        };

        filters[2] = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return true;
            }

            @Override
            public String getDescription() {
                return "All Files"; /*I18N*/
            }
        };
        return filters;
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final DefaultRequestElementFactory instance = new DefaultRequestElementFactory();
    }
}
