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

import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.internal.AbstractButtonAdapter;
import com.jidesoft.combobox.DateExComboBox;
import com.jidesoft.swing.AutoResizingTextArea;
import com.jidesoft.swing.TitledSeparator;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.ui.GridBagUtils;

import javax.measure.unit.NonSI;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * The panel in the binning operator UI which allows for setting the regional and temporal filters.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
class BinningFilterPanel extends JPanel {

    public static final String PROPERTY_WEST_BOUND = "westBound";
    public static final String PROPERTY_NORTH_BOUND = "northBound";
    public static final String PROPERTY_EAST_BOUND = "eastBound";
    public static final String PROPERTY_SOUTH_BOUND = "southBound";
    public static final String PROPERTY_WKT = "manualWkt";

    private final BindingContext bindingContext;
    private BinningFormModel binningFormModel;

    BinningFilterPanel(final BinningFormModel binningFormModel) {
        this.binningFormModel = binningFormModel;
        bindingContext = binningFormModel.getBindingContext();
        init();
    }

    private void init() {
        ButtonGroup buttonGroup = new ButtonGroup();

        final JRadioButton computeOption = new JRadioButton("Compute the geographical region according to extents of input products");
        final JRadioButton globalOption = new JRadioButton("Use the whole globe as region");
        final JRadioButton regionOption = new JRadioButton("Specify region:");
        final JRadioButton wktOption = new JRadioButton("Enter WKT:");

        bindingContext.bind(BinningFormModel.PROPERTY_KEY_COMPUTE_REGION, new RadioButtonAdapter(computeOption));
        bindingContext.bind(BinningFormModel.PROPERTY_KEY_GLOBAL, new RadioButtonAdapter(globalOption));
        bindingContext.bind(BinningFormModel.PROPERTY_KEY_MANUAL_WKT, new RadioButtonAdapter(wktOption));
        bindingContext.bind(BinningFormModel.PROPERTY_KEY_REGION, new RadioButtonAdapter(regionOption));

        buttonGroup.add(computeOption);
        buttonGroup.add(globalOption);
        buttonGroup.add(wktOption);
        buttonGroup.add(regionOption);

        computeOption.setSelected(true);

        final GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();

        GridBagUtils.addToPanel(this, new TitledSeparator("Specify target region", SwingConstants.CENTER), gbc, "insets=5,weighty=0,anchor=NORTHWEST,fill=HORIZONTAL");
        GridBagUtils.addToPanel(this, computeOption, gbc, "insets=3,gridy=1");
        GridBagUtils.addToPanel(this, globalOption, gbc, "gridy=2");
        GridBagUtils.addToPanel(this, wktOption, gbc, "gridy=3");
        GridBagUtils.addToPanel(this, createWktInputPanel(), gbc, "gridy=4");
        GridBagUtils.addToPanel(this, regionOption, gbc, "gridy=5");
        GridBagUtils.addToPanel(this, createBoundsInputPanel(), gbc, "gridy=6,insets.bottom=5");

        GridBagUtils.addToPanel(this, new TitledSeparator("Specify temporal filtering", SwingConstants.CENTER), gbc, "gridy=7,insets.bottom=3");
        GridBagUtils.addToPanel(this, createTemporalFilterPanel(), gbc, "gridy=8");
        GridBagUtils.addVerticalFiller(this, gbc);
    }

    private JComponent createWktInputPanel() {
        final AutoResizingTextArea textArea = new AutoResizingTextArea(5, 5);
        //Overrides behavior when set enabled
        textArea.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (textArea.isEnabled()) {
                    textArea.setBackground(Color.WHITE);
                } else {
                    textArea.setBackground(new Color(240, 240, 240));
                }
            }
        });
        bindingContext.bind(PROPERTY_WKT, textArea);
        bindingContext.bindEnabledState(PROPERTY_WKT, false, BinningFormModel.PROPERTY_KEY_MANUAL_WKT, false);
        textArea.setEnabled(false);

        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setMinimumSize(new Dimension(120, 120));
        scrollPane.setPreferredSize(new Dimension(120, 100));

        return scrollPane;
    }

    private JPanel createBoundsInputPanel() {
        final DoubleFormatter doubleFormatter = new DoubleFormatter("###0.0##");

        final JLabel westDegreeLabel = new JLabel(NonSI.DEGREE_ANGLE.toString());
        final JLabel westLabel = new JLabel("West:");
        final JFormattedTextField westLonField = new JFormattedTextField(doubleFormatter);
        westLonField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_WEST_BOUND, westLonField);
        bindingContext.bindEnabledState(PROPERTY_WEST_BOUND, false, BinningFormModel.PROPERTY_KEY_REGION, false);
        bindingContext.getBinding(PROPERTY_WEST_BOUND).addComponent(westLabel);
        bindingContext.getBinding(PROPERTY_WEST_BOUND).addComponent(westDegreeLabel);

        final JLabel eastDegreeLabel = new JLabel(NonSI.DEGREE_ANGLE.toString());
        final JLabel eastLabel = new JLabel("East:");
        final JFormattedTextField eastLonField = new JFormattedTextField(doubleFormatter);
        eastLonField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_EAST_BOUND, eastLonField);
        bindingContext.bindEnabledState(PROPERTY_EAST_BOUND, false, BinningFormModel.PROPERTY_KEY_REGION, false);
        bindingContext.getBinding(PROPERTY_EAST_BOUND).addComponent(eastLabel);
        bindingContext.getBinding(PROPERTY_EAST_BOUND).addComponent(eastDegreeLabel);

        final JLabel northLabel = new JLabel("North:");
        final JLabel northDegreeLabel = new JLabel(NonSI.DEGREE_ANGLE.toString());
        final JFormattedTextField northLatField = new JFormattedTextField(doubleFormatter);
        northLatField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_NORTH_BOUND, northLatField);
        bindingContext.bindEnabledState(PROPERTY_NORTH_BOUND, false, BinningFormModel.PROPERTY_KEY_REGION, false);
        bindingContext.getBinding(PROPERTY_NORTH_BOUND).addComponent(northLabel);
        bindingContext.getBinding(PROPERTY_NORTH_BOUND).addComponent(northDegreeLabel);

        final JLabel southLabel = new JLabel("South:");
        final JLabel southDegreeLabel = new JLabel(NonSI.DEGREE_ANGLE.toString());
        final JFormattedTextField southLatField = new JFormattedTextField(doubleFormatter);
        southLatField.setHorizontalAlignment(JTextField.RIGHT);
        bindingContext.bind(PROPERTY_SOUTH_BOUND, southLatField);
        bindingContext.bindEnabledState(PROPERTY_SOUTH_BOUND, false, BinningFormModel.PROPERTY_KEY_REGION, false);
        bindingContext.getBinding(PROPERTY_SOUTH_BOUND).addComponent(southLabel);
        bindingContext.getBinding(PROPERTY_SOUTH_BOUND).addComponent(southDegreeLabel);

        bindingContext.getBinding(BinningFormModel.PROPERTY_KEY_REGION).setPropertyValue(false);

        westLonField.setMinimumSize(new Dimension(120, 20));
        westLonField.setPreferredSize(new Dimension(120, 20));
        northLatField.setMinimumSize(new Dimension(120, 20));
        northLatField.setPreferredSize(new Dimension(120, 20));
        eastLonField.setMinimumSize(new Dimension(120, 20));
        eastLonField.setPreferredSize(new Dimension(120, 20));
        southLatField.setMinimumSize(new Dimension(120, 20));
        southLatField.setPreferredSize(new Dimension(120, 20));

        final JPanel panel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.addToPanel(panel, westLabel, gbc, "anchor=WEST,gridx=0,gridy=1");
        GridBagUtils.addToPanel(panel, westLonField, gbc, "gridx=1");
        GridBagUtils.addToPanel(panel, westDegreeLabel, gbc, "gridx=2");
        GridBagUtils.addToPanel(panel, northLabel, gbc, "gridx=3,gridy=0");
        GridBagUtils.addToPanel(panel, northLatField, gbc, "gridx=4");
        GridBagUtils.addToPanel(panel, northDegreeLabel, gbc, "gridx=5");
        GridBagUtils.addToPanel(panel, eastLabel, gbc, "gridx=6,gridy=1");
        GridBagUtils.addToPanel(panel, eastLonField, gbc, "gridx=7");
        GridBagUtils.addToPanel(panel, eastDegreeLabel, gbc, "gridx=8");
        GridBagUtils.addToPanel(panel, southLabel, gbc, "gridx=3,gridy=2");
        GridBagUtils.addToPanel(panel, southLatField, gbc, "gridx=4");
        GridBagUtils.addToPanel(panel, southDegreeLabel, gbc, "gridx=5");

        return panel;
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
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_TEMPORAL_FILTER, Boolean.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_START_DATE, Calendar.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_END_DATE, Calendar.class));
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_TEMPORAL_FILTER, temporalFilterCheckBox);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_START_DATE, startDatePicker);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_END_DATE, endDatePicker);
        binningFormModel.getBindingContext().bindEnabledState(BinningFormModel.PROPERTY_KEY_START_DATE, true, BinningFormModel.PROPERTY_KEY_TEMPORAL_FILTER, true);
        binningFormModel.getBindingContext().bindEnabledState(BinningFormModel.PROPERTY_KEY_END_DATE, true, BinningFormModel.PROPERTY_KEY_TEMPORAL_FILTER, true);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_START_DATE).addComponent(startDateLabel);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_END_DATE).addComponent(endDateLabel);

        GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        GridBagUtils.addToPanel(panel, temporalFilterCheckBox, gbc, "anchor=NORTHWEST, insets=5");
        GridBagUtils.addToPanel(panel, startDateLabel, gbc, "gridx=1,insets.top=9");
        GridBagUtils.addToPanel(panel, startDatePicker, gbc, "gridx=2,insets.top=6,weightx=1");
        GridBagUtils.addToPanel(panel, endDateLabel, gbc, "gridy=1,gridx=1,insets.top=9,weightx=0");
        GridBagUtils.addToPanel(panel, endDatePicker, gbc, "gridx=2,insets.top=6,weightx=1");
        return panel;
    }

    private DateExComboBox createDatePicker() {
        DateExComboBox datePicker = new DateExComboBox();
        datePicker.setLocale(Locale.ENGLISH);
        datePicker.getDateModel().setDateFormat(new SimpleDateFormat(BinningOp.DATE_PATTERN));
        datePicker.setPreferredSize(new Dimension(120, 20));
        datePicker.setMinimumSize(new Dimension(120, 20));
        return datePicker;
    }

    private static class RadioButtonAdapter extends AbstractButtonAdapter implements ItemListener {

        RadioButtonAdapter(AbstractButton button) {
            super(button);
        }

        @Override
        public void bindComponents() {
            getButton().addItemListener(this);
        }

        @Override
        public void unbindComponents() {
            getButton().removeItemListener(this);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            getBinding().setPropertyValue(getButton().isSelected());
        }
    }

    private static class DoubleFormatter extends JFormattedTextField.AbstractFormatter {

        private final DecimalFormat format;

        DoubleFormatter(String pattern) {
            final DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
            format = new DecimalFormat(pattern, decimalFormatSymbols);

            format.setParseIntegerOnly(false);
            format.setParseBigDecimal(false);
            format.setDecimalSeparatorAlwaysShown(true);
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            return format.parse(text).doubleValue();
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "";
            }
            return format.format(value);
        }
    }
}
