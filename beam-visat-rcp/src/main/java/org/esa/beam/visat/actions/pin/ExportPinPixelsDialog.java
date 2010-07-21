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
package org.esa.beam.visat.actions.pin;

import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * This class defines a modal dialog which lets the user specify a quadratic region around a
 * selected pin and an arithmetic expression.
 *
 * @author Maximilian Aulinger
 * @version Revision 1.3.1
 */

class ExportPinPixelsDialog extends ModalDialog {

    private static final String _PARAM_NAME_USE_ARTIHMETIC_EXPRESSION = "useArithmeticExpr";
    private static final int _MAX_REGION_SIZE = 25;

    private final VisatApp visatApp;
    private Product product;

    private Parameter paramRegion;
    private Parameter paramExpression;
    private Parameter paramUseExpression;
    private JRadioButton buttonExportAllPins;
    private JRadioButton buttonExportSelectedPins;
    private JRadioButton buttonUseExpressionAsFilter;
    private JRadioButton buttonUseExpressionAsMarker;

    private JButton editExpressionButton;

    /**
     * Initialises a new instance of this class.
     *
     * @param visatApp a reference to the VISAT application instance
     * @param product  the product providing the pixel values
     */
    ExportPinPixelsDialog(final VisatApp visatApp) {
        super(visatApp.getMainFrame(), "Export Pin Pixels", ModalDialog.ID_OK_CANCEL, null);
        this.visatApp = visatApp;
        initParameter();
        createUI();
    }

    /**
     * <code>true</code> if the user has selected "Use arithmetic expression".
     */
    boolean isUseExpression() {
        return (Boolean) paramUseExpression.getValue();
    }

    /**
     * <code>true</code> if the user has selected "Export selected pin".
     */
    boolean isExportSelectedPinsOnly() {
        return buttonExportSelectedPins.isSelected();
    }

    /**
     * <code>true</code> if the user has selected "Use expression as filter".
     */
    boolean isUseExpressionAsFilter() {
        return buttonUseExpressionAsFilter.isSelected();
    }

    /**
     * Returns the region's size specified by the user.
     */
    String getRegion() {
        return paramRegion.getValueAsText();
    }

    /**
     * Returns the region's width/height
     */
    int getRegionSize() {
        return extractRegionSize(getRegion());
    }

    /**
     * Returns the arithmetic expression specified by the user.
     */
    String getExpression() {
        final String expression = paramExpression.getValueAsText();
        if (expression != null && expression.trim().length() > 0) {
            return expression;
        } else {
            return null;
        }
    }

    /**
     * Initialises the selectable parameters.
     */
    private void initParameter() {
        final ParamChangeListener paramChangeListener = createParamChangeListener();

        final String[] possibleRegions = defineRegionSelection(_MAX_REGION_SIZE);

        paramRegion = new Parameter("exportRegion", possibleRegions[0]);
        paramRegion.getProperties().setLabel("Region size:"); /* I18N */
        paramRegion.getProperties().setValueSet(possibleRegions);
        paramRegion.getProperties().setValueSetBound(true);

        paramExpression = new Parameter("arithmeticExpr", "");
        paramExpression.getProperties().setLabel("Expression"); /* I18N */
        paramExpression.getProperties().setDescription("Band maths expression"); /* I18N */
        paramExpression.getProperties().setNumRows(3);
        paramExpression.setUIEnabled(false);

        paramUseExpression = new Parameter(_PARAM_NAME_USE_ARTIHMETIC_EXPRESSION, false);
        paramUseExpression.getProperties().setLabel("Use band maths expression"); /* I18N */
        paramUseExpression.addParamChangeListener(paramChangeListener);
    }

    /**
     * Creates the UI elements.
     */
    private void createUI() {

        final JPanel panel = GridBagUtils.createPanel();
        int line = 0;
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, createPinPane(), gbc,
                                "weighty=0, weightx=1, fill=BOTH, gridwidth=3, anchor=NORTHWEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, createRegionPane(), gbc,
                                "weighty=0, weightx=1, fill=BOTH, gridwidth=3, anchor=NORTHWEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(panel, createExpressionPane(), gbc,
                                "weighty=1, weightx=1, fill=BOTH, gridwidth=3, anchor=NORTHWEST");

        setContent(panel);
    }

    private JPanel createPinPane() {
        final JPanel pinPane = GridBagUtils.createPanel();
        int line = 0;
        final GridBagConstraints gbc = new GridBagConstraints();

        buttonExportAllPins = new JRadioButton("Export all pin(s)"); /* I18N */
        buttonExportAllPins.setSelected(true);
        buttonExportSelectedPins = new JRadioButton("Export selected pin(s)"); /* I18N */

        final ButtonGroup pinsGroup = new ButtonGroup();
        pinsGroup.add(buttonExportAllPins);
        pinsGroup.add(buttonExportSelectedPins);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(pinPane, buttonExportAllPins, gbc,
                                "weighty=0, weightx=0, fill=NONE, gridwidth=1, anchor=WEST");
        GridBagUtils.addToPanel(pinPane, buttonExportSelectedPins, gbc,
                                "weighty=0, weightx=0, fill=NONE, gridwidth=1, anchor=WEST");

        return pinPane;
    }

    private JPanel createRegionPane() {
        final JPanel regionPane = GridBagUtils.createPanel();
        int line = 0;
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(regionPane, paramRegion.getEditor().getLabelComponent(), gbc,
                                "weightx=1, gridwidth=3, fill=BOTH, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(regionPane, paramRegion.getEditor().getComponent(), gbc,
                                "gridwidth=3, fill=BOTH, anchor=WEST");

        return regionPane;
    }

    private JPanel createExpressionPane() {

        final JPanel expressionPane = GridBagUtils.createPanel();
        int line = 0;
        final GridBagConstraints gbc = new GridBagConstraints();
        expressionPane.setBorder(BorderFactory.createTitledBorder("Band Maths Expression"));

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(expressionPane, paramUseExpression.getEditor().getComponent(), gbc,
                                "weighty=0, weightx=0, fill=NONE, gridwidth=0, anchor=NORTHWEST");

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(expressionPane, paramExpression.getEditor().getLabelComponent(), gbc,
                                "fill=HORIZONTAL, gridwidth=3, anchor=NORTHWEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(expressionPane, paramExpression.getEditor().getComponent(), gbc,
                                "weighty=1,weightx=1 , fill=BOTH, gridwidth=3, anchor=NORTHWEST");

        editExpressionButton = new JButton("Edit Expression...");
        editExpressionButton.addActionListener(createEditExpressionButtonListener());
        editExpressionButton.setEnabled(false);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(expressionPane, editExpressionButton, gbc,
                                "weighty=0,weightx=0, fill=NONE, gridwidth=3, anchor=NORTHEAST");

        buttonUseExpressionAsFilter = new JRadioButton("Use expression as filter"); /* I18N */
        buttonUseExpressionAsFilter.setEnabled(false);

        buttonUseExpressionAsMarker = new JRadioButton("Add column with pixel relevance information"); /* I18N */
        buttonUseExpressionAsMarker.setEnabled(false);
        buttonUseExpressionAsMarker.setSelected(true);

        final ButtonGroup expressionGroup = new ButtonGroup();
        expressionGroup.add(buttonUseExpressionAsFilter);
        expressionGroup.add(buttonUseExpressionAsMarker);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(expressionPane, buttonUseExpressionAsFilter, gbc,
                                "weighty=0, weightx=0, fill=NONE, gridwidth=1, anchor=WEST");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(expressionPane, buttonUseExpressionAsMarker, gbc,
                                "weighty=0, weightx=0, fill=NONE, gridwidth=1, anchor=WEST");

        return expressionPane;
    }

    /**
     * Updates the UI elements.
     */
    private void updateUIState() {
        final boolean useExpression = (Boolean) paramUseExpression.getValue();
        paramExpression.setUIEnabled(useExpression);
        editExpressionButton.setEnabled(useExpression);
        buttonUseExpressionAsFilter.setEnabled(useExpression);
        buttonUseExpressionAsMarker.setEnabled(useExpression);
    }

    @Override
    protected boolean verifyUserInput() {
        final String expression = paramExpression.getValueAsText();
        final boolean useExpression = (Boolean) paramUseExpression.getValue();
        if (useExpression && !isValidExpression(expression)) {
            VisatApp.getApp().showErrorDialog(
                    "Please check the expression you have entered.\nIt is not valid."); /* I18N */
            return false;
        }
        return true;
    }

    /**
     * Shows the ExportPinPixels dialog window.
     *
     * @param selectedProduct  the selected product
     * 
     * @return the int value associated with the button the user has pressed. See org.esa.beam.framework.ui.ModalDialog
     */
    public int show(final Product selectedProduct) {
        int numSelectedPins = 0;
        int numPinsTotal = selectedProduct.getPinGroup().getNodeCount();
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null && sceneView.getProduct() == selectedProduct) {
            Placemark[] selectedPins = sceneView.getSelectedPins();
            numSelectedPins = selectedPins.length;
        }
        buttonExportSelectedPins.setEnabled(numSelectedPins > 0);
        buttonExportAllPins.setEnabled(numSelectedPins != numPinsTotal);
        buttonExportSelectedPins.setSelected(numSelectedPins > 0);
        buttonExportAllPins.setSelected(numSelectedPins == 0);
        this.product = selectedProduct;
        return super.show();
    }

    /**
     * Extracts an integer value "size" out of a String size x size
     */
    private int extractRegionSize(final String userSelectedRegionSize) {
        final String[] extractedRegion = userSelectedRegionSize.split("x");
        return new Integer(extractedRegion[0].trim());
    }

    /**
     * Defines an array with all possible region sizes (used as pulldown menu).
     *
     * @param maxRegionSize the maximum height/width for the quadratic region around the pin
     * @return the array with the possible options
     */
    private String[] defineRegionSelection(final int maxRegionSize) {
        if (maxRegionSize == 0) {
            return null;
        }
        final String[] regionSelection = new String[(maxRegionSize - 1) / 2];
        for (int i = 0; (3 + i * 2 <= maxRegionSize); i++) {
            final int n = (3 + i * 2);
            regionSelection[i] = (n + " x " + n + " pixels");
        }
        return regionSelection;
    }

    /**
     * Checks the arithmetic expression.
     */
    private boolean isValidExpression(final String expression) {
        if (expression == null || expression.length() == 0) {
            return false;
        }
        return product.isCompatibleBandArithmeticExpression(expression);
    }

    /**
     * Defines the ActionListener for the "Edit Expression"-button.
     */
    private ActionListener createEditExpressionButtonListener() {
        return new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                final Product[] products = {product};
                ProductExpressionPane pep = ProductExpressionPane.createBooleanExpressionPane(products, product,
                                                                                              visatApp.getPreferences());
                pep.setCode(paramExpression.getValueAsText());
                final int status = pep.showModalDialog(getJDialog(), "Band Maths Expression Editor"); /* I18N */
                if (status == ModalDialog.ID_OK) {
                    paramExpression.setValue(pep.getCode(), null);
                    Debug.trace("BandMathsDialog: expression is: " + pep.getCode()); /* I18N */
                }
                pep.dispose();
            }
        };
    }

    /**
     * The ParameterChangeListener for the useExpression parameter.
     */
    private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {

            @Override
            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
            }
        };
    }
}
