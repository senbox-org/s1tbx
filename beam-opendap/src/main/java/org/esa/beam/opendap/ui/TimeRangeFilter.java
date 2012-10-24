package org.esa.beam.opendap.ui;

import com.bc.ceres.binding.ValidationException;
import com.jidesoft.combobox.DateExComboBox;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.esa.beam.opendap.utils.PatternProvider;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.TimeStampExtractor;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.nc2.units.DateRange;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeRangeFilter implements FilterComponent {

    private JComboBox datePatternComboBox;
    private JComboBox fileNamePatternComboBox;
    private DateExComboBox startTimePicker;
    private DateExComboBox stopTimePicker;
    private JButton applyButton;
    private JCheckBox filterCheckBox;
    TimeStampExtractor timeStampExtractor;
    List<FilterChangeListener> listeners;
    List<JLabel> labels;

    Date startDate;
    Date endDate;

    public TimeRangeFilter(final JCheckBox filterCheckBox) {
        this.filterCheckBox = filterCheckBox;
        final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        startTimePicker = new DateExComboBox();
        startTimePicker.setLocale(Locale.ENGLISH);
        startTimePicker.setFormat(dateFormat);
        stopTimePicker = new DateExComboBox();
        stopTimePicker.setLocale(Locale.ENGLISH);
        stopTimePicker.setFormat(dateFormat);

        final int width = 150;
        final Dimension ps = startTimePicker.getPreferredSize();
        final Dimension comboBoxDimension = new Dimension(width, ps.height);
        setComboBoxSize(comboBoxDimension, startTimePicker);
        setComboBoxSize(comboBoxDimension, stopTimePicker);

        datePatternComboBox = new JComboBox();
        setComboBoxSize(comboBoxDimension, datePatternComboBox);
        datePatternComboBox.setEditable(true);

        fileNamePatternComboBox = new JComboBox();
        setComboBoxSize(comboBoxDimension, fileNamePatternComboBox);
        fileNamePatternComboBox.setEditable(true);

        initPatterns();

        final UIUpdater uiUpdater = new UIUpdater();
        final TimePickerValidator timePickerValidator = new TimePickerValidator();
        filterCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIState();
                if (timeStampExtractor != null) {
                    fireFilterChangedEvent();
                }
            }
        });
        startTimePicker.addActionListener(uiUpdater);
        startTimePicker.addActionListener(timePickerValidator);
        stopTimePicker.addActionListener(uiUpdater);
        stopTimePicker.addActionListener(timePickerValidator);
        datePatternComboBox.addActionListener(uiUpdater);
        fileNamePatternComboBox.addActionListener(uiUpdater);

        listeners = new ArrayList<FilterChangeListener>();
        labels = new ArrayList<JLabel>();

        applyButton = new JButton("Apply");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (StringUtils.isNotNullAndNotEmpty(datePatternComboBox.getEditor().getItem().toString())
                    && StringUtils.isNotNullAndNotEmpty(fileNamePatternComboBox.getEditor().getItem().toString())) {
                    timeStampExtractor = new TimeStampExtractor(datePatternComboBox.getSelectedItem().toString(),
                                                                fileNamePatternComboBox.getSelectedItem().toString());
                } else {
                    timeStampExtractor = null;
                }
                startDate = startTimePicker.getDate();
                endDate = stopTimePicker.getDate();
                updateUIState();
                applyButton.setEnabled(false);
                fireFilterChangedEvent();
            }
        });
    }

    private void updateUIState() {
        final boolean isSelected = filterCheckBox.isSelected();
        datePatternComboBox.setEnabled(isSelected);
        fileNamePatternComboBox.setEnabled(isSelected);
        startTimePicker.setEnabled(isSelected);
        stopTimePicker.setEnabled(isSelected);
        for (JLabel label : labels) {
            label.setEnabled(isSelected);
        }
        final String datePattern = datePatternComboBox.getSelectedItem().toString();
        final String fileNamePattern = fileNamePatternComboBox.getSelectedItem().toString();
        final boolean hasStartDate = startTimePicker.getDate() != null;
        final boolean hasEndDate = stopTimePicker.getDate() != null;
        final boolean patternProvided = !("".equals(datePattern) || "".equals(fileNamePattern));
        if (isSelected && (patternProvided || (!hasStartDate && !hasEndDate))) {
            applyButton.setEnabled(true);
        } else {
            applyButton.setEnabled(false);
        }
    }

    private void initPatterns() {
        datePatternComboBox.addItem("");
        fileNamePatternComboBox.addItem("");
        for (String datePattern : PatternProvider.DATE_PATTERNS) {
            datePatternComboBox.addItem(datePattern);
        }
        for (String fileNamePattern : PatternProvider.FILENAME_PATTERNS) {
            fileNamePatternComboBox.addItem(fileNamePattern);
        }
    }

    private void setComboBoxSize(Dimension comboBoxDimension, JComboBox comboBox) {
        comboBox.setPreferredSize(comboBoxDimension);
        comboBox.setMinimumSize(comboBoxDimension);
    }

    @Override
    public JComponent getUI() {

        final JPanel filterUI = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets.bottom = 4;
        gbc.insets.right = 4;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel datePatternLabel = new JLabel("Date pattern:");
        filterUI.add(datePatternLabel, gbc);
        gbc.gridx++;

        filterUI.add(datePatternComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel filenamePatternLabel = new JLabel("Filename pattern:");
        filterUI.add(filenamePatternLabel, gbc);
        gbc.gridx++;
        filterUI.add(fileNamePatternComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel startDateLabel = new JLabel("Start date:");
        filterUI.add(startDateLabel, gbc);
        gbc.gridx++;
        filterUI.add(startTimePicker, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel stopDateLabel = new JLabel("Stop date:");
        filterUI.add(stopDateLabel, gbc);

        gbc.gridx++;
        filterUI.add(stopTimePicker, gbc);
        gbc.gridy++;

        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        filterUI.add(applyButton, gbc);

        labels.add(datePatternLabel);
        labels.add(filenamePatternLabel);
        labels.add(startDateLabel);
        labels.add(stopDateLabel);

        updateUIState();

        return filterUI;
    }

    @Override
    public boolean accept(OpendapLeaf leaf) {
        DateRange timeCoverage = leaf.getDataset().getTimeCoverage();
        if (timeCoverage != null) {
            return fitsToServerSpecifiedTimeRange(timeCoverage);
        }
        if (timeStampExtractor != null) {
            return fitsToUserSpecifiedTimeRange(leaf);
        }
        return true;
    }

    private boolean fitsToServerSpecifiedTimeRange(DateRange dateRange) {
        if (startDate == null && endDate == null) {
            return true;
        } else if (startDate == null) {
            return endsAtOrBeforeEndDate(dateRange);
        } else if (endDate == null) {
            return startsAtOrAfterStartDate(dateRange);
        }
        return startsAtOrAfterStartDate(dateRange) && endsAtOrBeforeEndDate(dateRange);
    }

    private boolean endsAtOrBeforeEndDate(DateRange dateRange) {
        return dateRange.getEnd().getDate().equals(endDate) || dateRange.getEnd().before(endDate);
    }

    private boolean startsAtOrAfterStartDate(DateRange dateRange) {
        return dateRange.getStart().getDate().equals(startDate) || dateRange.getStart().after(startDate);
    }

    private boolean fitsToUserSpecifiedTimeRange(OpendapLeaf leaf) {
        try {
            final ProductData.UTC[] timeStamps = timeStampExtractor.extractTimeStamps(leaf.getName());

            final boolean startDateEqualsEndDate = timeStamps[0].getAsDate().getTime() == timeStamps[1].getAsDate().getTime();
            if (startDateEqualsEndDate) {
                timeStamps[1] = null;
            }

            boolean fileHasEndDate = timeStamps[1] != null;
            boolean userHasStartDate = startDate != null;
            boolean userHasEndDate = endDate != null;

            if (userHasStartDate) {
                if (startDate.after(timeStamps[0].getAsDate())) {
                    return false;
                }
            }

            if (userHasEndDate) {
                if (fileHasEndDate) {
                    if (endDate.before(timeStamps[1].getAsDate())) {
                        return false;
                    }
                } else {
                    if (endDate.before(timeStamps[0].getAsDate())) {
                        return false;
                    }
                }
            }
            return true;

        } catch (ValidationException e) {
            return true;
        }
    }

    @Override
    public void addFilterChangeListener(FilterChangeListener listener) {
        listeners.add(listener);
    }

    private void fireFilterChangedEvent() {
        for (FilterChangeListener listener : listeners) {
            listener.filterChanged();
        }
    }

    private class UIUpdater implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateUIState();
        }
    }

    private class TimePickerValidator implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Date startDate = startTimePicker.getDate();
            Date endDate = stopTimePicker.getDate();
            if (startDate == null || endDate == null) {
                return;
            }
            if (startDate.after(endDate)) {
                if (e.getSource().equals(startTimePicker)) {
                    startTimePicker.setDate(endDate);
                    BeamLogManager.getSystemLogger().info("Start date after end date: Set start date to end date.");
                } else if (e.getSource().equals(stopTimePicker)) {
                    stopTimePicker.setDate(startDate);
                    BeamLogManager.getSystemLogger().info("Start date after end date: Set end date to start date.");
                }
            }
        }
    }
}
