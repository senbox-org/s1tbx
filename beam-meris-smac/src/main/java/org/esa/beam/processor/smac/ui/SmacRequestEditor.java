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
package org.esa.beam.processor.smac.ui;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.BorderLayoutUtils;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.processor.smac.SmacConstants;
import org.esa.beam.processor.smac.SmacUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

/**
 * The <code>SmacRequestEditor</code> class gives the user the room to modify or create the entries of a Request for
 * SMAC-Prozessor.
 * <p/>
 * You can create an instance of this class in two diferential ways: With od without main-frame-window. If you create an
 * instance without main-frame-window, the Dialog-Gui was centered to the screen. If you create an instance with
 * main-frame-window, the Dialog-Gui was centered to main-frame-window.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class SmacRequestEditor extends AbstractProcessorUI {

    private static final String IO_PARAM_TAB_NAME = "I/O Parameters";                       /*I18N*/
    private static final String PROC_PARAM_TAB_NAME = "Processing Parameters";              /*I18N*/

    private SmacRequestParameterPool _requestData;
    private String[] _formatNames;


    /**
     * Creates the basis tabbed pane which is exported to the <code>ProcessorUIFrame</code>.
     */
    private JTabbedPane _tabbedPane;

    private JRadioButton _rb1;
    private JRadioButton _rb2;
    private JPanel _parametersContainer;
    private JComboBox _fileCombo;

    private JLabel _aatsrBitmaskExpressionLabel1;
    private JComponent _aatsrBitmaskExpressionEditor1;
    private JLabel _aatsrBitmaskExpressionLabel2;
    private JComponent _aatsrBitmaskExpressionEditor2;
    private JLabel _merisBitmaskExpressionLabel;
    private JComponent _merisBitmaskExpressionEditor;

    /**
     * Constructs the Smac request editor.
     */
    public SmacRequestEditor() {
        _requestData = new SmacRequestParameterPool(this);
        scanProductFormatStrings();
    }

    /**
     * Initializes the editor with all the input- and output-products and all parameter in the given request. also the
     * internal state was set ok for methods <code>show()</code> and <code>getRequest()</code>
     */
    public void setRequests(Vector requests) {
        Guardian.assertNotNull("requests", requests);
        try {
            if (requests.size() > 0) {
                Request request = (Request) requests.elementAt(0);
                getRequestData().setRequest(request);
                setFileFormatSelector(request);
                _requestData.scanInputProductBands(request);
            } else {
                setDefaultRequests();
            }
        } catch (ProcessorException e) {
            Debug.trace(e);
        }
    }

    /**
     * Gets the request containing the user's modificiations.
     *
     * @return a <code>Request</code> object with all elements required by the SMAC processor
     */
    public Vector getRequests() throws ProcessorException {
        final Vector<Request> vRet = new Vector<Request>();
        final Parameter outputParam = getRequestData().getParameter(SmacConstants.OUTPUT_PRODUCT_PARAM_NAME);
        if (hasParameterEmptyString(outputParam)) {
            throw new ProcessorException("No output product specified.");
        }
        vRet.add(getRequestData().getRequest());
        return vRet;
    }

    /**
     * Create a new default request and set UI according
     */
    public void setDefaultRequests() {
        Vector<Request> newReq = new Vector<Request>();

        newReq.add(_requestData.getDefaultRequest());
        setRequests(newReq);
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype.
     */
    public JComponent getGuiComponent() {
        if (_tabbedPane == null) {
            createUI();
        }
        return _tabbedPane;
    }

    /**
     * Sets the processor app for the UI
     */
    @Override
    public void setApp(ProcessorApp app) {
        super.setApp(app);
        if (_requestData != null && _requestData.getParamGroup() != null) {
            getApp().markIODirChanges(_requestData.getParamGroup());
        }
    }

    /**
     * Sets the bitmask editor to the type passed in
     */
    public void setBitmaskEditor(String type) {
        if (SmacUtils.isSupportedMerisProductType(type)) {
            // meris
            _parametersContainer.remove(_aatsrBitmaskExpressionEditor2);
            _parametersContainer.remove(_aatsrBitmaskExpressionLabel2);
            _parametersContainer.remove(_aatsrBitmaskExpressionEditor1);
            _parametersContainer.remove(_aatsrBitmaskExpressionLabel1);
            final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
            GridBagUtils.setAttributes(gbc, "gridy=1, gridx=0, insets.top=15, fill=BOTH");
            _parametersContainer.add(_merisBitmaskExpressionLabel, gbc);
            GridBagUtils.setAttributes(gbc, "gridy=1, gridx=1, gridwidth=2");
            _parametersContainer.add(_merisBitmaskExpressionEditor, gbc);
        } else {
            // aatsr
            _parametersContainer.remove(_merisBitmaskExpressionEditor);
            _parametersContainer.remove(_merisBitmaskExpressionLabel);
            final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
            GridBagUtils.setAttributes(gbc, "gridy=1, gridx=0, fill=BOTH, insets.top=15");
            _parametersContainer.add(_aatsrBitmaskExpressionLabel1, gbc);
            GridBagUtils.setAttributes(gbc, "gridy=1, gridx=1, gridwidth=2");
            _parametersContainer.add(_aatsrBitmaskExpressionEditor1, gbc);
            GridBagUtils.setAttributes(gbc, "gridy=2, gridx=0, gridwidth=1, insets.top=5");
            _parametersContainer.add(_aatsrBitmaskExpressionLabel2, gbc);
            GridBagUtils.setAttributes(gbc, "gridy=2, gridx=1, gridwidth=2");
            _parametersContainer.add(_aatsrBitmaskExpressionEditor2, gbc);
        }
    }

    /**
     * Shows a warning dialog with the message passed in.
     */
    public void showWarningDialog(String message) {
        getApp().showWarningDialog("SMAC", message);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Scans the ProductIO for all product format strings of the registered writer plugins
     */
    private void scanProductFormatStrings() {
        ProductIOPlugInManager manager = ProductIOPlugInManager.getInstance();
        _formatNames = manager.getAllProductWriterFormatStrings();
    }

    /**
     * Creates all components of the user interface
     */
    private void createUI() {
        _tabbedPane = new JTabbedPane();
        createIOParamsTab();
        createProcParamTab();
        HelpSys.enableHelp(_tabbedPane, "smacScientificTool");
    }

    /**
     * Creates all components of the inpu/output product selection tab
     */
    private void createIOParamsTab() {
        int line = 0;
        JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        Parameter param;

        param = getParameter(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME);
        param.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                final Parameter parameter = event.getParameter();
                final String productPath = parameter.getValueAsText();
                final Parameter bitmaskForwardParam = getParameter(SmacConstants.BITMASK_FORWARD_PARAM_NAME);
                bitmaskForwardParam.getProperties().setPropertyValue(
                        BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT, productPath);
                final Parameter bitmaskNadirParam = getParameter(SmacConstants.BITMASK_NADIR_PARAM_NAME);
                bitmaskNadirParam.getProperties().setPropertyValue(
                        BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT, productPath);
                final Parameter bitmaskParam = getParameter(SmacConstants.BITMASK_PARAM_NAME);
                bitmaskParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                              productPath);
            }
        });
        GridBagUtils.setAttributes(gbc,
                                   "fill=HORIZONTAL, anchor=SOUTHWEST, weightx=1, weighty=1, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        param = getParameter(SmacConstants.OUTPUT_PRODUCT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        //Output format names
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, insets.top=-16, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, new JLabel("Output product format: "), gbc);
        _fileCombo = new JComboBox(_formatNames);
        _fileCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateOutFileType();
            }
        });
        GridBagUtils.setAttributes(gbc, "fill=NONE, anchor=NORTHWEST, insets.top=0, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, _fileCombo, gbc);

        // logging
        // -------
        param = getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=0.5, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        param = getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        _tabbedPane.add(IO_PARAM_TAB_NAME, panel);
    }

    /**
     * Retrieves a named parameter from the request data pool.
     *
     * @param name the parameter name
     */
    private Parameter getParameter(String name) {
        return getRequestData().getParameter(name);
    }

    /**
     * Fills the parameter tab of the smac UI with the appropriate controls
     */
    private void createProcParamTab() {
        JPanel bandsPanel = createVerticalPanel(SmacConstants.BANDS_PARAM_NAME);

        // to avoid resizing of the other parameters on long band names
        bandsPanel.setPreferredSize(new Dimension(140, 80));

        int line = 0;
        JPanel eastPanel = GridBagUtils.createPanel();
        addAerosolType(eastPanel, line++);
        ActionListener rbActionListener = createRadioButtonActionListener();
        addAerosolOpticalDepthLine(eastPanel, line++, rbActionListener);
        addHorizontalVisibilityLine(eastPanel, line++, rbActionListener);
        addUseMerisECMWF(eastPanel, line++);
        addSurfaceAirPressure(eastPanel, line++);
        addOzoneContent(eastPanel, line++);
        addRelativeHumidity(eastPanel, line++);
        addDefaultReflect(eastPanel, line++);

        createBitmaskEditorComponents();
        _parametersContainer = GridBagUtils.createDefaultEmptyBorderPanel();
        GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        GridBagUtils.setAttributes(gbc, "weightx=1, gridwidth=2, fill=BOTH");
        GridBagUtils.addToPanel(_parametersContainer, bandsPanel, gbc);

        GridBagUtils.setAttributes(gbc, "weightx=0, gridwidth=1, insets.left=10");
        GridBagUtils.addToPanel(_parametersContainer, eastPanel, gbc);

        GridBagUtils.setAttributes(gbc, "gridy=1, gridx=0, insets.top=15, weightx=0");
        GridBagUtils.addToPanel(_parametersContainer, _aatsrBitmaskExpressionLabel1, gbc);

        GridBagUtils.setAttributes(gbc, "gridy=1, gridx=1, gridwidth=2, weightx=1");
        GridBagUtils.addToPanel(_parametersContainer, _aatsrBitmaskExpressionEditor1, gbc);

        GridBagUtils.setAttributes(gbc, "gridy=2, gridx=0, gridwidth=1, insets.top=5, weightx=0");
        GridBagUtils.addToPanel(_parametersContainer, _aatsrBitmaskExpressionLabel2, gbc);

        GridBagUtils.setAttributes(gbc, "gridy=2, gridx=1, gridwidth=2, weightx=1");
        GridBagUtils.addToPanel(_parametersContainer, _aatsrBitmaskExpressionEditor2, gbc);

        _tabbedPane.add(PROC_PARAM_TAB_NAME, _parametersContainer);
    }

    private void createBitmaskEditorComponents() {
        Parameter parameter;

        parameter = getParameter(SmacConstants.BITMASK_FORWARD_PARAM_NAME);
        _aatsrBitmaskExpressionLabel1 = parameter.getEditor().getLabelComponent();
        _aatsrBitmaskExpressionEditor1 = parameter.getEditor().getEditorComponent();

        parameter = getParameter(SmacConstants.BITMASK_NADIR_PARAM_NAME);
        _aatsrBitmaskExpressionLabel2 = parameter.getEditor().getLabelComponent();
        _aatsrBitmaskExpressionEditor2 = parameter.getEditor().getEditorComponent();

        parameter = getParameter(SmacConstants.BITMASK_PARAM_NAME);
        _merisBitmaskExpressionLabel = parameter.getEditor().getLabelComponent();
        _merisBitmaskExpressionEditor = parameter.getEditor().getEditorComponent();
    }

    private ActionListener createRadioButtonActionListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateVisibilityType(e);
            }
        };
    }

    private JPanel createVerticalPanel(String paramName) {
        Parameter param = getParameter(paramName);
        JLabel label = param.getEditor().getLabelComponent();
        JComponent comp = getComponent(param);
        return BorderLayoutUtils.createPanel(comp, label, BorderLayout.NORTH);
    }

    private void addDefaultReflect(JPanel panel, int gridy) {
        Parameter param = getParameter(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, gridwidth=2, insets.top=20, weighty=2");
        GridBagUtils.addToPanel(panel, getLabelComponent(param), gbc);
        GridBagUtils.setAttributes(gbc, "gridwidth=1");
        GridBagUtils.addToPanel(panel, getComponent(param), gbc);
        GridBagUtils.addToPanel(panel, getPhysUnit(param), gbc);
    }

    private void addRelativeHumidity(JPanel panel, int gridy) {
        Parameter param = getParameter(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "anchor=WEST, gridwidth=2");
        GridBagUtils.addToPanel(panel, getLabelComponent(param), gbc);
        GridBagUtils.setAttributes(gbc, "gridwidth=1");
        JComponent comp = getComponent(param);
        comp.setEnabled(false);
        GridBagUtils.addToPanel(panel, comp, gbc);
        GridBagUtils.addToPanel(panel, getPhysUnit(param), gbc);
    }

    private void addOzoneContent(JPanel panel, int gridy) {
        Parameter param = getParameter(SmacConstants.OZONE_CONTENT_PARAM_NAME);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "anchor=WEST, gridwidth=2");
        GridBagUtils.addToPanel(panel, getLabelComponent(param), gbc);
        GridBagUtils.setAttributes(gbc, "gridwidth=1");
        JComponent comp = getComponent(param);
        comp.setEnabled(false);
        GridBagUtils.addToPanel(panel, comp, gbc);
        GridBagUtils.addToPanel(panel, getPhysUnit(param), gbc);
    }

    private void addSurfaceAirPressure(JPanel panel, int gridy) {
        Parameter param = getParameter(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "anchor=WEST, gridwidth=2");
        GridBagUtils.addToPanel(panel, getLabelComponent(param), gbc);
        GridBagUtils.setAttributes(gbc, "gridwidth=1");
        JComponent comp = getComponent(param);
        comp.setEnabled(false);
        GridBagUtils.addToPanel(panel, comp, gbc);
        GridBagUtils.addToPanel(panel, getPhysUnit(param), gbc);
    }

    private void addUseMerisECMWF(JPanel panel, int gridy) {
        Parameter param = getParameter(SmacConstants.USE_MERIS_ADS_PARAM_NAME);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "weighty=2, anchor=WEST, gridwidth=4");
        GridBagUtils.addToPanel(panel, getComponent(param), gbc);
    }

    private void addAerosolOpticalDepthLine(JPanel panel, int gridy, ActionListener actionListener) {
        Parameter param = getParameter(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        _rb1 = createRadioButton("aod", true, actionListener);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "anchor=WEST");
        GridBagUtils.addToPanel(panel, _rb1, gbc);
        GridBagUtils.addToPanel(panel, getLabelComponent(param), gbc);
        GridBagUtils.addToPanel(panel, getComponent(param), gbc);
        GridBagUtils.addToPanel(panel, getPhysUnit(param), gbc);
    }

    private static JLabel getPhysUnit(Parameter param) {
        return param.getEditor().getPhysUnitLabelComponent();
    }

    private static JLabel getLabelComponent(Parameter param) {
        return param.getEditor().getLabelComponent();
    }

    private void addHorizontalVisibilityLine(JPanel panel, int gridy, ActionListener actionListener) {
        Parameter param = getParameter(SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME);
        _rb2 = createRadioButton("hv", false, actionListener);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "anchor=WEST");
        GridBagUtils.addToPanel(panel, _rb2, gbc);
        GridBagUtils.addToPanel(panel, getLabelComponent(param), gbc);
        JComponent comp = getComponent(param);
        comp.setEnabled(false);
        GridBagUtils.addToPanel(panel, comp, gbc);
        GridBagUtils.addToPanel(panel, getPhysUnit(param), gbc);
    }

    private static GridBagConstraints createLineConstraint(int gridy) {
        return GridBagUtils.createConstraints("gridy=".concat(String.valueOf(gridy)));
    }

    private void addAerosolType(JPanel panel, int gridy) {
        Parameter param = getParameter(SmacConstants.AEROSOL_TYPE_PARAM_NAME);

        final GridBagConstraints gbc = createLineConstraint(gridy);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, gridwidth=2");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.addToPanel(panel, getComponent(param), gbc);
    }

    private static JComponent getComponent(Parameter param) {
        return param.getEditor().getComponent();
    }

    private static JRadioButton createRadioButton(String actionCommand,
                                                  boolean selected,
                                                  ActionListener actionListener) {
        JRadioButton jrb = new JRadioButton();
        jrb.setOpaque(false);
        jrb.setActionCommand(actionCommand);
        jrb.setSelected(selected);
        jrb.addActionListener(actionListener);
        return jrb;
    }

    private void updateVisibilityType(ActionEvent e) {
        String cmd = e.getActionCommand();
        Parameter aodParam = getRequestData().getParameter(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        Parameter hvParam = getRequestData().getParameter(SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME);
        JComponent aodComp = aodParam.getEditor().getComponent();
        JComponent hvComp = hvParam.getEditor().getComponent();

        boolean equal = "aod".equals(cmd);
        _rb1.setSelected(equal);
        _rb2.setSelected(!equal);
        aodComp.setEnabled(equal);
        hvComp.setEnabled(!equal);
        rbRepaint(aodComp, hvComp);
    }

    private static void rbRepaint(JComponent aodComp, JComponent hvComp) {
        aodComp.repaint();
        hvComp.repaint();
    }

    /**
     * Callback invoked on file type selection changed
     */
    private void updateOutFileType() {
        getRequestData().setOutputProductFormat((String) _fileCombo.getSelectedItem());
    }

    /**
     * Updates the file format combo with the request set
     */
    private void setFileFormatSelector(final Request request) {
        if (request.getNumOutputProducts() > 0) {
            final ProductRef ref = request.getOutputProductAt(0);
            final String format = ref.getFileFormat();
            if (format != null) {
                _fileCombo.setSelectedItem(format);
            }
        }
    }

    private SmacRequestParameterPool getRequestData() {
        return _requestData;
    }

    private static boolean hasParameterEmptyString(final Parameter parameter) {
        final String valueAsText = parameter.getValueAsText();

        if (valueAsText.trim().length() <= 0) {
            return true;
        } else {
            return false;
        }
    }
}
