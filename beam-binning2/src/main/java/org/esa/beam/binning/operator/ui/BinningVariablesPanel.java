/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * The panel in the binning operator UI which allows for specifying the configuration of binning variables.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningVariablesPanel extends JPanel {

    private final AppContext appContext;
    private final BinningFormModel binningFormModel;
    private VariableConfigTable bandsTable;

    BinningVariablesPanel(AppContext appContext, BinningFormModel binningFormModel) {
        this.appContext = appContext;
        this.binningFormModel = binningFormModel;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(this, createBandsPanel(), gbc, "insets=5,fill=BOTH,weightx=1.0,weighty=1.0");
        GridBagUtils.addToPanel(this, createValidExpressionPanel(), gbc, "gridy=1,fill=HORIZONTAL,weightx=1.0,weighty=0.0");
        GridBagUtils.addToPanel(this, createSuperSamplingAndTargetHeightPanel(), gbc, "gridy=4");
        GridBagUtils.addToPanel(this, createOutputBinnedDataComponent(), gbc, "gridy=5");
    }

    private JPanel createBandsPanel() {
        bandsTable = new VariableConfigTable(binningFormModel, appContext);
        final JPanel bandsPanel = new JPanel(new GridBagLayout());

        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"), false);
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"), false);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String name;
                final int expressionCount = bandsTable.getExpressionCount();
                name = "<expression_" + expressionCount + ">";
                bandsTable.addRow(name, null, AggregatorAverage.Descriptor.NAME, 1.0, 0);
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bandsTable.removeSelectedRows();
            }
        });

        final GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(bandsPanel, addButton, gbc, "gridx=0");
        GridBagUtils.addToPanel(bandsPanel, removeButton, gbc, "gridx=1");
        GridBagUtils.addHorizontalFiller(bandsPanel, gbc);
        GridBagUtils.addToPanel(bandsPanel, bandsTable.getComponent(), gbc, "gridx=0,gridy=1,gridwidth=3,fill=BOTH,weightx=1,weighty=1");

        return bandsPanel;
    }

    private JPanel createValidExpressionPanel() {
        final JButton button = new JButton("...");
        final Dimension preferredSize = button.getPreferredSize();
        preferredSize.setSize(25, preferredSize.getHeight());
        button.setPreferredSize(preferredSize);
        button.setEnabled(hasSourceProducts());
        binningFormModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!evt.getPropertyName().equals(BinningFormModel.PROPERTY_KEY_SOURCE_PRODUCTS)) {
                    return;
                }
                button.setEnabled(hasSourceProducts());
            }
        });
        final JTextField textField = new JTextField();
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                final String expression = editExpression(textField.getText());
                if (expression != null) {
                    textField.setText(expression);
                    try {
                        binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_EXPRESSION, expression);
                    } catch (ValidationException e) {
                        appContext.handleError("Invalid expression", e);
                    }
                }
            }
        });

        final JPanel validExpressionPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(validExpressionPanel, new JLabel("Valid expression:"), gbc, "anchor=NORTHWEST,insets=3,insets.top=6");
        GridBagUtils.addToPanel(validExpressionPanel, textField, gbc, "gridx=1,weightx=1,fill=HORIZONTAL,insets.top=3,insets.left=24");
        GridBagUtils.addToPanel(validExpressionPanel, button, gbc, "gridx=2,weightx=0,fill=NONE,insets.top=2,insets.left=3");

        return validExpressionPanel;
    }

    private JComponent createOutputBinnedDataComponent() {
        final JCheckBox checkBox = new JCheckBox("Output binned data");
        final Property property = BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_OUTPUT_BINNED_DATA, Boolean.class);
        binningFormModel.getBindingContext().getPropertySet().addProperty(property);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_OUTPUT_BINNED_DATA, checkBox);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_OUTPUT_BINNED_DATA).setPropertyValue(Boolean.TRUE);
        return checkBox;
    }

    private Component createSuperSamplingAndTargetHeightPanel() {
        final JLabel targetHeightLabel = new JLabel("Target height (px):");
        final JLabel superSamplingLabel = new JLabel("Supersampling:");
        final JTextField targetHeightTextField = new IntegerTextField(BinningFormModel.DEFAULT_NUM_ROWS);
        final JLabel resolutionLabel = new JLabel();
        updateResolutionLabel(targetHeightTextField, resolutionLabel);
        final JTextField superSamplingTextField = new IntegerTextField(1);
        targetHeightTextField.setPreferredSize(new Dimension(120, 20));
        targetHeightTextField.setMinimumSize(new Dimension(120, 20));
        superSamplingTextField.setPreferredSize(new Dimension(120, 20));
        superSamplingTextField.setMinimumSize(new Dimension(120, 20));

        binningFormModel.getBindingContext().getPropertySet().addProperty(
                BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, Integer.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(
                BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, Integer.class));
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT, targetHeightTextField);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING, superSamplingTextField);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT).setPropertyValue(
                BinningFormModel.DEFAULT_NUM_ROWS);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_SUPERSAMPLING).setPropertyValue(1);

        binningFormModel.getBindingContext().getPropertySet().getProperty(BinningFormModel.PROPERTY_KEY_TARGET_HEIGHT).addPropertyChangeListener(
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateResolutionLabel(targetHeightTextField, resolutionLabel);
                    }
                });

        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(panel, targetHeightLabel, gbc, "anchor=NORTHWEST,weightx=0,insets=3,insets.top=8");
        GridBagUtils.addToPanel(panel, targetHeightTextField, gbc, "gridx=2,weightx=0,insets.top=3,insets.left=13");
        GridBagUtils.addToPanel(panel, resolutionLabel, gbc, "gridx=3,weightx=1,insets.left=3,insets.top=5,fill=HORIZONTAL");
        GridBagUtils.addToPanel(panel, superSamplingLabel, gbc, "gridy=1,gridx=0,weightx=0,insets.top=8,insets.left=3,fill=NONE");
        GridBagUtils.addToPanel(panel, superSamplingTextField, gbc, "gridx=2,weightx=1,gridwidth=2,insets.top=3,insets.left=13");

        return panel;
    }

    private static void updateResolutionLabel(JTextField targetHeightTextField, JLabel resolutionLabel) {
        resolutionLabel.setText(
                "Spatial resolution: ~ " + getResolutionString(Integer.parseInt(targetHeightTextField.getText())));
    }

    static String getResolutionString(int numRows) {
        final double RE = 6378.145;
        final double resolution = (RE * Math.PI) / (numRows - 1);
        final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        final DecimalFormat decimalFormat = new DecimalFormat("#.##", formatSymbols);
        return decimalFormat.format(resolution) + " km/pixel";
    }

    private boolean hasSourceProducts() {
        return binningFormModel.getSourceProducts().length > 0;
    }

    private String editExpression(String expression) {
        final Product product = binningFormModel.getSourceProducts()[0];
        if (product == null) {
            return null;
        }
        final ProductExpressionPane expressionPane;
        expressionPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product,
                                                                           appContext.getPreferences());
        expressionPane.setCode(expression);
        final int i = expressionPane.showModalDialog(appContext.getApplicationWindow(), "Expression Editor");
        if (i == ModalDialog.ID_OK) {
            return expressionPane.getCode();
        }
        return null;
    }

    private static class IntegerTextField extends JTextField {

        private final static String disallowedChars = "`§~!@#$%^&*()_+=\\|\"':;?/>.<,- ";

        public IntegerTextField(int defaultValue) {
            super(defaultValue + "");
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            if (!Character.isLetter(e.getKeyChar()) && disallowedChars.indexOf(e.getKeyChar()) == -1) {
                super.processKeyEvent(e);
            }
        }
    }

}
