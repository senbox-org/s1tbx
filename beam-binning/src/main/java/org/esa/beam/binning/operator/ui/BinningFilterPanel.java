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

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.binding.internal.AbstractButtonAdapter;
import com.jidesoft.combobox.DateExComboBox;
import com.jidesoft.swing.AutoResizingTextArea;
import com.jidesoft.swing.TitledSeparator;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.RegionBoundsInputUI;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static org.esa.beam.binning.operator.BinningOp.TimeFilterMethod.*;

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
        GridBagUtils.addToPanel(this, createAndInitBoundsUI(), gbc, "gridy=6,insets.bottom=5");

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

    private JPanel createAndInitBoundsUI() {
        final RegionBoundsInputUI regionBoundsInputUI;
        regionBoundsInputUI = new RegionBoundsInputUI(bindingContext);

        bindingContext.addPropertyChangeListener(BinningFormModel.PROPERTY_KEY_REGION, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean enabled = (Boolean) evt.getNewValue();
                regionBoundsInputUI.setEnabled(enabled);
            }
        });

        regionBoundsInputUI.setEnabled(false);

        return regionBoundsInputUI.getUI();
    }

    private Component createTemporalFilterPanel() {
        TableLayout layout = new TableLayout(3);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableWeightX(0.0);
        layout.setTableWeightY(0.0);
        layout.setTablePadding(10, 5);
        layout.setCellColspan(0, 1, 2);
        layout.setCellColspan(1, 1, 2);
        layout.setCellColspan(3, 1, 2);
        layout.setCellWeightX(2, 1, 1.0);
        layout.setCellWeightX(2, 2, 0.0);
        layout.setColumnWeightX(1, 1.0);

        JPanel panel = new JPanel(layout);
        JLabel temporalFilterLabel = new JLabel("Time filter method:");
        JLabel startDateLabel = new JLabel("Start date:");
        JLabel periodDurationLabel = new JLabel("Period duration:");
        JLabel minDataHourLabel = new JLabel("Min data hour:");
        JLabel periodDurationUnitLabel = new JLabel("days");

        JComboBox<String> temporalFilterComboBox = new JComboBox<>(new String[]{
                NONE.name(),
                TIME_RANGE.name(),
                SPATIOTEMPORAL_DATADAY.name()
        });
        DateExComboBox startDatePicker = createDatePicker();
        JTextField periodDurationTextField = new JTextField();
        JTextField minDataHourTextField = new JTextField();
        startDateLabel.setEnabled(false);
        periodDurationLabel.setEnabled(false);
        temporalFilterLabel.setToolTipText("The method that is used to decide which source pixels are used with respect to their observation time.");
        startDateLabel.setToolTipText("The UTC start date of the binning period. If only the date part is given, the time 00:00:00 is assumed.");
        periodDurationLabel.setToolTipText("Duration of the binning period in days.");
        minDataHourLabel.setToolTipText("A sensor-dependent constant given in hours of a day (0 to 24) at which a sensor has a minimum number of observations at the date line (the 180 degree meridian).");
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, String.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_START_DATE_TIME, Calendar.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_PERIOD_DURATION, Double.class));
        binningFormModel.getBindingContext().getPropertySet().addProperty(BinningDialog.createProperty(BinningFormModel.PROPERTY_KEY_MIN_DATA_HOUR, Double.class));

        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, temporalFilterComboBox);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_START_DATE_TIME, startDatePicker);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_PERIOD_DURATION, periodDurationTextField);
        binningFormModel.getBindingContext().bind(BinningFormModel.PROPERTY_KEY_MIN_DATA_HOUR, minDataHourTextField);

        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_START_DATE_TIME).addComponent(startDateLabel);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_PERIOD_DURATION).addComponent(periodDurationLabel);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_PERIOD_DURATION).addComponent(periodDurationUnitLabel);
        binningFormModel.getBindingContext().getBinding(BinningFormModel.PROPERTY_KEY_MIN_DATA_HOUR).addComponent(minDataHourLabel);

        temporalFilterComboBox.setSelectedIndex(0); // selected value must not be empty when setting enablement

        binningFormModel.getBindingContext().bindEnabledState(BinningFormModel.PROPERTY_KEY_START_DATE_TIME, true, hasTimeInformation(TIME_RANGE, SPATIOTEMPORAL_DATADAY));
        binningFormModel.getBindingContext().bindEnabledState(BinningFormModel.PROPERTY_KEY_PERIOD_DURATION, true, hasTimeInformation(TIME_RANGE, SPATIOTEMPORAL_DATADAY));
        binningFormModel.getBindingContext().bindEnabledState(BinningFormModel.PROPERTY_KEY_MIN_DATA_HOUR, true, hasTimeInformation(SPATIOTEMPORAL_DATADAY));

        temporalFilterComboBox.setSelectedIndex(0); // ensure that enablement is applied

        panel.add(temporalFilterLabel);
        panel.add(temporalFilterComboBox);
        panel.add(startDateLabel);
        panel.add(startDatePicker);
        panel.add(periodDurationLabel);
        panel.add(periodDurationTextField);
        panel.add(periodDurationUnitLabel);
        panel.add(minDataHourLabel);
        panel.add(minDataHourTextField);
        return panel;
    }

    private static Enablement.Condition hasTimeInformation(final BinningOp.TimeFilterMethod... conditions) {
        return new Enablement.Condition() {
            @Override
            public boolean evaluate(BindingContext bindingContext) {
                String chosenMethod = bindingContext.getPropertySet().getProperty(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD).getValueAsText();
                BinningOp.TimeFilterMethod temporalFilter = BinningOp.TimeFilterMethod.valueOf(chosenMethod);
                for (BinningOp.TimeFilterMethod condition : conditions) {
                    if (condition == temporalFilter) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void install(BindingContext bindingContext, Enablement enablement) {
                bindingContext.addPropertyChangeListener(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, enablement);
            }

            @Override
            public void uninstall(BindingContext bindingContext, Enablement enablement) {
                bindingContext.removePropertyChangeListener(BinningFormModel.PROPERTY_KEY_TIME_FILTER_METHOD, enablement);
            }
        };
    }

    private static DateExComboBox createDatePicker() {
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

}
