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

package org.esa.beam.framework.processor.ui;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.IOException;

/**
 * This class is intended to be used with {@link MultiPageProcessorUI}.
 * <p/>
 * It provides an UI for selecting an input and output product, the output fomat and for
 * enabling logging in to an extra loggoing file next to the output product.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Norman Fomferra
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
*/
@Deprecated
public class IOParameterPage extends ParameterPage {

    /**
     * Parameter name for the request parameter describing the input_product.
     * This field just redirects to {@link ProcessorConstants#INPUT_PRODUCT_PARAM_NAME}
     */
    public static final String INPUT_PRODUCT_PARAM_NAME = ProcessorConstants.INPUT_PRODUCT_PARAM_NAME;
    /**
     * Parameter name for the request parameter describing the output_product.
     * This field just redirects to {@link ProcessorConstants#OUTPUT_PRODUCT_PARAM_NAME}
     */
    public static final String OUTPUT_PRODUCT_PARAM_NAME = ProcessorConstants.OUTPUT_PRODUCT_PARAM_NAME;
    /**
     * Parameter name for the request parameter describing the output_format.
     * This field just redirects to {@link ProcessorConstants#OUTPUT_FORMAT_PARAM_NAME}
     */
    public static final String OUTPUT_FORMAT_PARAM_NAME = ProcessorConstants.OUTPUT_FORMAT_PARAM_NAME;
    /**
     * Parameter name for the request parameter describing the log_prefix.
     * This field just redirects to {@link ProcessorConstants#LOG_PREFIX_PARAM_NAME}
     */
    public static final String LOG_PREFIX_PARAM_NAME = ProcessorConstants.LOG_PREFIX_PARAM_NAME;
    /**
     * Parameter name for the request parameter describing the log_to_output.
     * This field just redirects to {@link ProcessorConstants#LOG_TO_OUTPUT_PARAM_NAME}
     */
    public static final String LOG_TO_OUTPUT_PARAM_NAME = ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME;

    /**
     * The default title of this page.
     */
    public static final String DEFAULT_PAGE_TITLE = "I/O Parameters";   /*I18N*/

    private static final String _defaultLogPrefix = "beam";
    private static final String _defaultLogToOutput = "false";
    private static final String _defaultOutputProductFileName = "output.dim";

    /**
     * Creates an I/O parameter page for the {@link MultiPageProcessorUI}.
     * This class is created with a {@link ParamGroup paramGroup} which contains the following parameters:
     * <ul>
     * <li>{@link IOParameterPage#INPUT_PRODUCT_PARAM_NAME} of type <code>java.io.File</code></li>
     * <li>{@link IOParameterPage#OUTPUT_PRODUCT_PARAM_NAME} of type <code>java.io.File</code></li>
     * <li>{@link IOParameterPage#OUTPUT_FORMAT_PARAM_NAME} of type <code>java.lang.String</code></li>
     * <li>{@link IOParameterPage#LOG_PREFIX_PARAM_NAME} of type <code>java.lang.String</code></li>
     * <li>{@link IOParameterPage#LOG_TO_OUTPUT_PARAM_NAME} of type <code>java.lang.Boolean</code></li>
     * </ul>
     *
     * @see #IOParameterPage(org.esa.beam.framework.param.ParamGroup)
     */
    public IOParameterPage() {
        this((InputProductValidator) null);
    }

    /**
     * Creates an default I/O page for the {@link MultiPageProcessorUI}.
     * This class is created with a {@link ParamGroup paramGroup} which contains the following parameters:
     * <ul>
     * <li>{@link IOParameterPage#INPUT_PRODUCT_PARAM_NAME} of type <code>java.io.File</code></li>
     * <li>{@link IOParameterPage#OUTPUT_PRODUCT_PARAM_NAME} of type <code>java.io.File</code></li>
     * <li>{@link IOParameterPage#OUTPUT_FORMAT_PARAM_NAME} of type <code>java.lang.String</code></li>
     * <li>{@link IOParameterPage#LOG_PREFIX_PARAM_NAME} of type <code>java.lang.String</code></li>
     * <li>{@link IOParameterPage#LOG_TO_OUTPUT_PARAM_NAME} of type <code>java.lang.Boolean</code></li>
     * </ul>
     *
     * @param validator the input product validator, can be null if not required
     *
     * @see #IOParameterPage(org.esa.beam.framework.param.ParamGroup)
     */
    public IOParameterPage(final InputProductValidator validator) {
        this(createDefaultParamGroup(validator));
    }

    /**
     * Creates an default I/O page for the {@link MultiPageProcessorUI}.
     * The given {@link ParamGroup paramGroup} must contain the following parameters:
     * <ul>
     * <li>{@link IOParameterPage#INPUT_PRODUCT_PARAM_NAME} of type <code>java.io.File</code></li>
     * <li>{@link IOParameterPage#OUTPUT_PRODUCT_PARAM_NAME} of type <code>java.io.File</code></li>
     * <li>{@link IOParameterPage#OUTPUT_FORMAT_PARAM_NAME} of type <code>java.lang.String</code></li>
     * <li>{@link IOParameterPage#LOG_PREFIX_PARAM_NAME} of type <code>java.lang.String</code></li>
     * <li>{@link IOParameterPage#LOG_TO_OUTPUT_PARAM_NAME} of type <code>java.lang.Boolean</code></li>
     * </ul>
     *
     * @param paramGroup the paramGroup to create the UI from
     *
     * @see ProcessorConstants
     * @see #IOParameterPage()
     */
    public IOParameterPage(final ParamGroup paramGroup) {
        super(paramGroup);
        setTitle(DEFAULT_PAGE_TITLE);
    }

    /**
     * Gets the default output product file name.
     *
     * @return the default output product file name
     */
    public String getDefaultOutputProductFileName() {
        final Object defaultValue = getParamGroup().getParameter(
                OUTPUT_PRODUCT_PARAM_NAME).getProperties().getDefaultValue();

        if (defaultValue instanceof File) {
            final File file = (File) defaultValue;
            return file.getName();
        } else {
            return "";
        }
    }

    /**
     * Sets the default output product file name.
     *
     * @param name the output product file name
     */
    public void setDefaultOutputProductFileName(final String name) {
        final Parameter param = getParamGroup().getParameter(OUTPUT_PRODUCT_PARAM_NAME);
        final Object defaultValue = param.getProperties().getDefaultValue();
        if (defaultValue instanceof File) {
            final File file = (File) defaultValue;
            param.getProperties().setDefaultValue(new File(file.getParentFile(), name));
        }
    }

    /**
     * Gets the default log prefix.
     *
     * @return the default log prefix
     */
    public String getDefaultLogPrefix() {
        return String.valueOf(getParamGroup().getParameter(LOG_PREFIX_PARAM_NAME).getProperties().getDefaultValue());
    }

    /**
     * Sets the default log prefix.
     *
     * @param logPrefix the log prefix
     */
    public void setDefaultLogPrefix(final String logPrefix) {
        getParamGroup().getParameter(LOG_PREFIX_PARAM_NAME).getProperties().setDefaultValue(logPrefix);
    }

    public void setDefaultLogToOutputParameter(final Boolean logToOutput) {
        getParamGroup().getParameter(LOG_TO_OUTPUT_PARAM_NAME).getProperties().setDefaultValue(logToOutput);
    }


    /**
     * Sets the parameter values by these given with the {@link Request request}.
     *
     * @param request the request to obtain the parameters
     *
     * @throws ProcessorException if an error occurred
     */
    @Override
    public void setUIFromRequest(final Request request) throws ProcessorException {
        updateParamInputFile(request);
        updateParamOutputFile(request);
        updateParamOutputFormat(request);
        updateLogParameter(request);
    }

    /**
     * Fills the given {@link Request request} with parameters.
     *
     * @param request the request to fill
     *
     * @throws ProcessorException if an error occurred
     */
    @Override
    public void initRequestFromUI(final Request request) throws ProcessorException {
        request.addInputProduct(createInputProductRef());
        final Parameter outputProductParam = getParamGroup().getParameter(OUTPUT_PRODUCT_PARAM_NAME);
        if (StringUtils.isNullOrEmpty(outputProductParam.getValueAsText())) {
            throw new ProcessorException("No output product specified.");       /*I18N*/
        }

        request.addOutputProduct(createOutputProductRef());
        request.addParameter(createOutputFormatParamForRequest());
        request.addParameter(getParamGroup().getParameter(LOG_PREFIX_PARAM_NAME));
        request.addParameter(getParamGroup().getParameter(LOG_TO_OUTPUT_PARAM_NAME));
    }

    /**
     * It creates the UI by using the {@link ParamGroup parameter group} of this page.
     * <p/>
     * <p>It's only called once by the {@link MultiPageProcessorUI} during lifetime of an
     * instance of this class.</p>
     *
     * @return the UI component displayed as page of the {@link MultiPageProcessorUI}.
     */
    @Override
    public JComponent createUI() {
        final ParamGroup paramGroup = getParamGroup();
        final JPanel productsPanel = createProductsPanel(paramGroup);
        final JPanel loggingPanel = createLogginPanel(paramGroup);


        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(
                "fill=HORIZONTAL, weightx = 1, weighty=1, gridx=0, gridy=0");

        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(productsPanel, gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets.top = 30;
        panel.add(loggingPanel, gbc);

        return panel;
    }

    private static ParamGroup createDefaultParamGroup(final InputProductValidator validator) {
        final ParamGroup paramGroup = new ParamGroup();
        final DefaultRequestElementFactory factory = DefaultRequestElementFactory.getInstance();
        final Parameter inputProductParameter = factory.createDefaultInputProductParameter();
        inputProductParameter.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                validateInputProduct(event.getParameter(), validator);
            }
        });
        paramGroup.addParameter(inputProductParameter);
        final Parameter outputProductParameter = factory.createDefaultOutputProductParameter();
        final Object defaultValue = outputProductParameter.getProperties().getDefaultValue();
        if (defaultValue instanceof File) {
            outputProductParameter.getProperties().setDefaultValue(new File((File) defaultValue,
                                                                            _defaultOutputProductFileName));
        }
        paramGroup.addParameter(outputProductParameter);
        paramGroup.addParameter(factory.createOutputFormatParameter());
        paramGroup.addParameter(factory.createDefaultLogPatternParameter(_defaultLogPrefix));
        try {
            paramGroup.addParameter(factory.createLogToOutputParameter(_defaultLogToOutput));
        } catch (ParamValidateException e) {
            Debug.trace("Unable to validate parameter '" + LOG_TO_OUTPUT_PARAM_NAME + "'"); /*I18N*/
            Debug.trace(e);
        }
        return paramGroup;
    }

    private static JPanel createLogginPanel(final ParamGroup paramGroup) {
        final JPanel loggingPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(
                "anchor=NORTHWEST, weightx = 1, fill=HORIZONTAL, gridx=0");

        // logging
        // -------
        Parameter param;
        param = paramGroup.getParameter(LOG_PREFIX_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.addToPanel(loggingPanel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.addToPanel(loggingPanel, param.getEditor().getComponent(), gbc);

        param = paramGroup.getParameter(LOG_TO_OUTPUT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.addToPanel(loggingPanel, param.getEditor().getComponent(), gbc);
        return loggingPanel;
    }

    private static JPanel createProductsPanel(final ParamGroup paramGroup) {
        final JPanel productsPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST, fill=HORIZONTAL, weightx = 1");
        gbc.gridy = 0;

        // input product
        // -------------
        Parameter param;
        param = paramGroup.getParameter(INPUT_PRODUCT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.addToPanel(productsPanel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.addToPanel(productsPanel, param.getEditor().getComponent(), gbc);

        // output product
        // --------------
        param = paramGroup.getParameter(OUTPUT_PRODUCT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=7");
        GridBagUtils.addToPanel(productsPanel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=0");
        GridBagUtils.addToPanel(productsPanel, param.getEditor().getComponent(), gbc);

        // output format
        // -------------
        param = paramGroup.getParameter(OUTPUT_FORMAT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=7, anchor=SOUTHWEST, fill=NONE");
        GridBagUtils.addToPanel(productsPanel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=0, anchor=NORTHWEST");
        GridBagUtils.addToPanel(productsPanel, param.getEditor().getComponent(), gbc);

        return productsPanel;
    }


    private void updateParamOutputFormat(final Request request) {
        final Parameter parameter = request.getParameter(OUTPUT_FORMAT_PARAM_NAME);
        if (parameter != null) {
            final String format = parameter.getValueAsText();
            final ProductIOPlugInManager instance = ProductIOPlugInManager.getInstance();
            final String[] allowedformats = instance.getAllProductWriterFormatStrings();
            if (ArrayUtils.isMemberOf(format, allowedformats)) {
                getParamGroup().getParameter(OUTPUT_FORMAT_PARAM_NAME).setValue(format, null);
            }
        }
    }

    private void updateParamOutputFile(final Request request) {
        final File file = new File(request.getOutputProductAt(0).getFilePath());
        getParamGroup().getParameter(OUTPUT_PRODUCT_PARAM_NAME).setValue(file, null);
    }

    private void updateParamInputFile(final Request request) {
        final File file = new File(request.getInputProductAt(0).getFilePath());
        getParamGroup().getParameter(INPUT_PRODUCT_PARAM_NAME).setValue(file, null);
    }

    private void updateLogParameter(final Request request) {
        Parameter param;
        Parameter toUpdate;

        param = request.getParameter(LOG_PREFIX_PARAM_NAME);
        final ParamGroup paramGroup = getParamGroup();
        if (param != null) {
            toUpdate = paramGroup.getParameter(LOG_PREFIX_PARAM_NAME);
            toUpdate.setValue(param.getValue(), null);
        }

        param = request.getParameter(LOG_TO_OUTPUT_PARAM_NAME);
        if (param != null) {
            toUpdate = paramGroup.getParameter(LOG_TO_OUTPUT_PARAM_NAME);
            toUpdate.setValue(param.getValue(), null);
        }
    }

    /*
     * Creates an input product reference for a request.
     */
    private ProductRef createInputProductRef() {
        final String filePath = getParamGroup().getParameter(INPUT_PRODUCT_PARAM_NAME).getValueAsText();
        return new ProductRef(new File(filePath), null, null);
    }

    /*
     * Creates an output product reference for a request.
     */
    private ProductRef createOutputProductRef() {
        final ParamGroup paramGroup = getParamGroup();
        final String fileName = paramGroup.getParameter(OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();
        final String fileFormat = paramGroup.getParameter(OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
        return ProcessorUtils.createProductRef(fileName, fileFormat);
    }

    /*
     * Creates an output product format parameter for a request.
     */
    private Parameter createOutputFormatParamForRequest() {
        final String outputFormat = getParamGroup().getParameter(OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
        return new Parameter(OUTPUT_FORMAT_PARAM_NAME, outputFormat);
    }

    private static void validateInputProduct(final Parameter parameter, final InputProductValidator validator) {
        final File file = (File) parameter.getValue();
        if (file == null || "".equals(file.getPath().trim())) {
            return;
        }
        String msg = null;
        if (file.exists()) {
            Product product = null;
            try {
                product = ProductIO.readProduct(file);
                if (product != null) {
                    if (validator != null) {
                        final boolean valid = validator.validate(product);
                        if (!valid) {
                            msg = validator.getErrorMessage();
                            if (msg == null) {
                                msg = "Unknown error.";
                            }
                        }
                    }
                } else {
                    msg = "Unknown product file format.";
                }
            } catch (IOException e) {
                msg = e.getMessage();
            } finally {
                if (product != null) {
                    product.dispose();
                }
            }
        } else {
            msg = "File '" + file.getPath() + "' does not exists.";
        }
        if (msg != null) {
            JOptionPane.showMessageDialog(parameter.getEditor().getEditorComponent(),
                                          "Invalid input product file:\n" + msg,
                                          "Invalid Input Product", JOptionPane.ERROR_MESSAGE);
            Debug.trace(msg);
        }
    }

    public abstract static class InputProductValidator {

        private String _errorMessage;

        public String getErrorMessage() {
            return _errorMessage;
        }

        public void setErrorMessage(final String errorMessage) {
            _errorMessage = errorMessage;
        }

        public abstract boolean validate(Product product);
    }
}
