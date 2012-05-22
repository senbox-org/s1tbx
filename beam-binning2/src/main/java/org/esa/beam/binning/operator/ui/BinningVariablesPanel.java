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
import com.jidesoft.combobox.DateExComboBox;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;

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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * The panel in the binning operator UI which allows for specifying the configuration of binning variables.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningVariablesPanel extends JPanel {

    private final AppContext appContext;
    private final BinningModel binningModel;
    private VariableConfigTable bandsTable;

    BinningVariablesPanel(AppContext appContext, BinningModel binningModel) {
        this.appContext = appContext;
        this.binningModel = binningModel;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(this, createBandsPanel(), gbc, "insets=5,fill=BOTH,weightx=1.0,weighty=1.0");
        GridBagUtils.addToPanel(this, createValidExpressionPanel(), gbc, "gridy=1,fill=HORIZONTAL,weightx=1.0,weighty=0.0");
        GridBagUtils.addToPanel(this, createTemporalFilterPanel(), gbc, "gridy=2");
        GridBagUtils.addToPanel(this, createSuperSamplingAndTargetHeightPanel(), gbc, "gridy=3");
        GridBagUtils.addToPanel(this, createOutputBinnedDataComponent(), gbc, "gridy=4");
    }

    private JPanel createBandsPanel() {
        bandsTable = new VariableConfigTable(binningModel, appContext);
        final JPanel bandsPanel = new JPanel(new GridBagLayout());

        final JButton addButton = new JButton("Add");
        final JButton removeButton = new JButton("Remove");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bandsTable.addRow("<expression>", null, AggregatorAverage.Descriptor.NAME, Double.NaN, Float.NaN);
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bandsTable.removeSelectedRows();
            }
        });

        final GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(bandsPanel, new JLabel(), gbc, "insets=5,gridwidth=1,fill=HORIZONTAL,weightx=1.0");
        GridBagUtils.addToPanel(bandsPanel, addButton, gbc, "gridx=1,fill=NONE,weightx=0.0");
        GridBagUtils.addToPanel(bandsPanel, removeButton, gbc, "gridx=2");
        GridBagUtils.addToPanel(bandsPanel, bandsTable.getComponent(), gbc, "gridx=0,gridy=1,gridwidth=3,fill=BOTH,weightx=1,weighty=1");

        return bandsPanel;
    }

    private JPanel createValidExpressionPanel() {
        final JButton button = new JButton("...");
        final Dimension preferredSize = button.getPreferredSize();
        preferredSize.setSize(25, preferredSize.getHeight());
        button.setPreferredSize(preferredSize);
        button.setEnabled(hasSourceProducts());
        binningModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!evt.getPropertyName().equals(BinningModel.PROPERTY_KEY_SOURCE_PRODUCTS)) {
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
                        binningModel.setProperty(BinningModel.PROPERTY_KEY_EXPRESSION, expression);
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

    private Component createTemporalFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        JCheckBox temporalFilterCheckBox = new JCheckBox("Temporal Filter");
        JLabel startDateLabel = new JLabel("Start date:");
        JLabel endDateLabel = new JLabel("End date:");
        DateExComboBox startDatePicker = createDatePicker();
        DateExComboBox endDatePicker = createDatePicker();
        startDateLabel.setEnabled(false);
        endDateLabel.setEnabled(false);
        binningModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, Boolean.class));
        binningModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_START_DATE, Calendar.class));
        binningModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_END_DATE, Calendar.class));
        binningModel.getBindingContext().bind(BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, temporalFilterCheckBox);
        binningModel.getBindingContext().bind(BinningModel.PROPERTY_KEY_START_DATE, startDatePicker);
        binningModel.getBindingContext().bind(BinningModel.PROPERTY_KEY_END_DATE, endDatePicker);
        binningModel.getBindingContext().bindEnabledState(BinningModel.PROPERTY_KEY_START_DATE, true, BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, true);
        binningModel.getBindingContext().bindEnabledState(BinningModel.PROPERTY_KEY_END_DATE, true, BinningModel.PROPERTY_KEY_TEMPORAL_FILTER, true);
        binningModel.getBindingContext().getBinding(BinningModel.PROPERTY_KEY_START_DATE).addComponent(startDateLabel);
        binningModel.getBindingContext().getBinding(BinningModel.PROPERTY_KEY_END_DATE).addComponent(endDateLabel);

        GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        GridBagUtils.addToPanel(panel, temporalFilterCheckBox, gbc, "anchor=NORTHWEST, insets=5");
        GridBagUtils.addToPanel(panel, startDateLabel, gbc, "gridx=1,insets.top=9");
        GridBagUtils.addToPanel(panel, startDatePicker, gbc, "gridx=2,insets.top=6,weightx=1");
        GridBagUtils.addToPanel(panel, endDateLabel, gbc, "gridy=1,gridx=1,insets.top=9,weightx=0");
        GridBagUtils.addToPanel(panel, endDatePicker, gbc, "gridx=2,insets.top=6,weightx=1");
        return panel;
    }

    private JComponent createOutputBinnedDataComponent() {
        final JCheckBox checkBox = new JCheckBox("Output binned data");
        final Property property = BinningDialog.createProperty(BinningModel.PROPERTY_KEY_OUTPUT_BINNED_DATA, Boolean.class);
        binningModel.getBindingContext().getPropertySet().addProperty(property);
        binningModel.getBindingContext().bind(BinningModel.PROPERTY_KEY_OUTPUT_BINNED_DATA, checkBox);
        binningModel.getBindingContext().getBinding(BinningModel.PROPERTY_KEY_OUTPUT_BINNED_DATA).setPropertyValue(Boolean.TRUE);
        return checkBox;
    }

    private DateExComboBox createDatePicker() {
        DateExComboBox datePicker = new DateExComboBox();
        datePicker.setLocale(Locale.ENGLISH);
        datePicker.getDateModel().setDateFormat(new SimpleDateFormat(BinningOp.DATE_PATTERN));
        datePicker.setPreferredSize(new Dimension(120, 20));
        datePicker.setMinimumSize(new Dimension(120, 20));
        return datePicker;
    }

    private Component createSuperSamplingAndTargetHeightPanel() {
        final JLabel targetHeightLabel = new JLabel("Target height (px):");
        final JLabel superSamplingLabel = new JLabel("Supersampling:");
        final JTextField targetHeightTextField = new IntegerTextField("2160");
        final JTextField superSamplingTextField = new IntegerTextField("1");
        targetHeightTextField.setPreferredSize(new Dimension(120, 20));
        targetHeightTextField.setMinimumSize(new Dimension(120, 20));
        superSamplingTextField.setPreferredSize(new Dimension(120, 20));
        superSamplingTextField.setMinimumSize(new Dimension(120, 20));

        binningModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_TARGET_HEIGHT, Integer.class));
        binningModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningModel.PROPERTY_KEY_SUPERSAMPLING, Integer.class));
        binningModel.getBindingContext().bind(BinningModel.PROPERTY_KEY_TARGET_HEIGHT, targetHeightTextField);
        binningModel.getBindingContext().bind(BinningModel.PROPERTY_KEY_SUPERSAMPLING, superSamplingTextField);
        binningModel.getBindingContext().getBinding(BinningModel.PROPERTY_KEY_TARGET_HEIGHT).setPropertyValue(2160);
        binningModel.getBindingContext().getBinding(BinningModel.PROPERTY_KEY_SUPERSAMPLING).setPropertyValue(1);

        final JPanel panel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(panel, targetHeightLabel, gbc, "anchor=NORTHWEST,weightx=0,insets=3,insets.top=8");
        GridBagUtils.addToPanel(panel, targetHeightTextField, gbc, "gridx=2,weightx=1,insets.top=3,insets.left=13");
        GridBagUtils.addToPanel(panel, superSamplingLabel, gbc, "gridy=1,gridx=0,weightx=0,insets.top=8,insets.left=3");
        GridBagUtils.addToPanel(panel, superSamplingTextField, gbc, "gridx=2,weightx=1,insets.top=3,insets.left=13");

        return panel;
    }

    private boolean hasSourceProducts() {
        return binningModel.getSourceProducts().length > 0;
    }

    private String editExpression(String expression) {
        final Product product = binningModel.getSourceProducts()[0];
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

        private final static String disallowedChars = "`~!@#$%^&*()_+=\\|\"':;?/>.<,- ";

        public IntegerTextField(String defaultValue) {
            super(defaultValue);
        }

        @Override
        protected void processKeyEvent(KeyEvent e) {
            if (!Character.isLetter(e.getKeyChar()) && disallowedChars.indexOf(e.getKeyChar()) == -1) {
                super.processKeyEvent(e);
            }
        }
    }

}
