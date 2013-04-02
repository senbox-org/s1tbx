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

package org.esa.beam.timeseries.ui.player;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponentDescriptor;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.timeseries.export.animations.AnimatedGifExport;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

/**
 * @author Thomas Storm
 */
class TimeSeriesPlayerForm extends JPanel {

    private final ImageIcon playIcon = new ImageIcon(getClass().getResource("icons/timeseries-play24.png"));
    private final ImageIcon stopIcon = new ImageIcon(getClass().getResource("icons/timeseries-stop24.png"));
    private final ImageIcon pauseIcon = UIUtils.loadImageIcon("icons/Pause24.png");
    private final ImageIcon blendIcon = new ImageIcon(getClass().getResource("icons/timeseries-blend24.png"));
    private final ImageIcon repeatIcon = new ImageIcon(getClass().getResource("icons/timeseries-repeat24.png"));
    private final ImageIcon minusIcon = UIUtils.loadImageIcon("icons/Remove16.png");
    private final ImageIcon plusIcon = UIUtils.loadImageIcon("icons/Add16.png");
    private final ImageIcon exportIcon = UIUtils.loadImageIcon("icons/Export24.gif");

    private final JSlider timeSlider;
    private final AbstractButton playButton;
    private final AbstractButton stopButton;
    private final JLabel dateLabel;
    private final JSlider speedSlider;
    private final JLabel speedLabel;
    private final JLabel speedUnit;
    private final AbstractButton blendButton;
    private Timer timer;
    private final AbstractButton repeatButton;
    private final AbstractButton minusButton;
    private final AbstractButton plusButton;
    private final AbstractButton exportButton;

    private int stepsPerTimespan = 1;
    private int timerDelay = 1250;
    private AbstractTimeSeries timeSeries;
    private ProductSceneView currentView;
    /**
     * must be different from any character occuring in the date format
     */
    private static final String DATE_SEPARATOR = " ";

    TimeSeriesPlayerForm(PageComponentDescriptor descriptor) {
        this.setLayout(new BorderLayout(4, 4));
        this.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        this.setPreferredSize(new Dimension(350, 200));
        JPanel firstPanel = new JPanel(createLayout());
        firstPanel.setPreferredSize(new Dimension(300, 150));
        final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final JPanel secondPanel = new JPanel(new BorderLayout());
//        BoxLayout boxLayout = new BoxLayout(secondPanel, BoxLayout.Y_AXIS);
//        secondPanel.setLayout(boxLayout);

        dateLabel = new JLabel("Date: ");
        timeSlider = createTimeSlider();
        playButton = createPlayButton();
        stopButton = createStopButton();
        repeatButton = createRepeatButton();
        blendButton = createBlendButton();
        speedLabel = new JLabel("Speed:");
        minusButton = createMinusButton();
        speedSlider = createSpeedSlider();
        speedUnit = new JLabel();
        plusButton = createPlusButton();
        exportButton = createExportButton();

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help22.png"), false);
        helpButton.setToolTipText("Help");

        updateSpeedUnit();
        setUIEnabled(false);

        buttonsPanel.add(playButton);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(repeatButton);
        buttonsPanel.add(new JSeparator(JSeparator.VERTICAL));
        buttonsPanel.add(blendButton);
        buttonsPanel.add(new JSeparator(JSeparator.VERTICAL));
        buttonsPanel.add(speedLabel);
        buttonsPanel.add(minusButton);
        buttonsPanel.add(speedSlider);
        buttonsPanel.add(plusButton);
        buttonsPanel.add(speedUnit);
        buttonsPanel.add(new JLabel("           "));
        secondPanel.add(exportButton, BorderLayout.NORTH);
        secondPanel.add(helpButton, BorderLayout.SOUTH);

        if (descriptor.getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, descriptor.getHelpId());
            HelpSys.enableHelpKey(buttonsPanel, descriptor.getHelpId());
        }

        firstPanel.add(dateLabel);
        firstPanel.add(timeSlider);
        firstPanel.add(buttonsPanel);

        this.add(BorderLayout.CENTER, firstPanel);
        this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        this.add(BorderLayout.EAST, secondPanel);
    }

    List<Band> getBandList(final String rasterName) {
        final String variableName = AbstractTimeSeries.rasterToVariableName(rasterName);
        return timeSeries.getBandsForVariable(variableName);
    }

    void setTimeSeries(AbstractTimeSeries timeSeries) {
        this.timeSeries = timeSeries;
    }

    Timer getTimer() {
        return timer;
    }

    void setView(ProductSceneView view) {
        this.currentView = view;
    }

    int getStepsPerTimespan() {
        return stepsPerTimespan;
    }


    JSlider getTimeSlider() {
        return timeSlider;
    }

    void configureTimeSlider(RasterDataNode raster) {
        if (timeSeries != null) {
            List<Band> bandList = getBandList(raster.getName());

            timeSlider.setMinimum(0);
            final int nodeCount = bandList.size();
            final int maximum = (nodeCount - 1) * stepsPerTimespan;
            timeSlider.setMaximum(maximum);

            final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            if (nodeCount > 1) {
                setUIEnabled(true);
                labelTable.put(0, new JLabel(createSliderLabelFormattedText(bandList, 0)));
                labelTable.put(maximum,
                               new JLabel(createSliderLabelFormattedText(bandList, maximum / stepsPerTimespan)));
                timeSlider.setLabelTable(labelTable);
            } else {
                timeSlider.setLabelTable(null);
                setUIEnabled(false);
            }
            final int index = bandList.indexOf(raster);
            if (index != -1) {
                timeSlider.setValue(index * stepsPerTimespan);
            }
        } else {
            timeSlider.setLabelTable(null);
            setUIEnabled(false);
        }
    }

    private String createSliderLabelText(List<Band> bandList, int index) {
        Band band = bandList.get(index);
        TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(band);
        if (timeCoding != null) {
            final ProductData.UTC utcStartTime = timeCoding.getStartTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            final String dateText = dateFormat.format(utcStartTime.getAsCalendar().getTime());
            final String timeText = timeFormat.format(utcStartTime.getAsCalendar().getTime());
            return dateText + DATE_SEPARATOR + timeText;
        } else {
            return "";
        }
    }

    private String createSliderLabelFormattedText(List<Band> bandList, int index) {
        final String labelText = createSliderLabelText(bandList, index);
        final String[] strings = labelText.split(DATE_SEPARATOR);
        return String.format("<html><p align=\"center\"> <font size=\"2\">%s<br>%s</font></p>", strings[0], strings[1]);
    }

    private JSlider createTimeSlider() {
        final JSlider timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 0, 0);
        timeSlider.setMajorTickSpacing(stepsPerTimespan);
        timeSlider.setMinorTickSpacing(1);
        timeSlider.setPaintTrack(true);
        timeSlider.setSnapToTicks(true);
        timeSlider.setPaintTicks(true);
        timeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final int index = timeSlider.getValue() / stepsPerTimespan;
                final List<Band> bandList = getBandList(currentView.getRaster().getName());
                final String labelText = createSliderLabelText(bandList, index);
                dateLabel.setText("Date: " + labelText);
            }
        });
        timeSlider.setPreferredSize(new Dimension(320, 60));
        return timeSlider;
    }

    private AbstractButton createPlayButton() {
        final ActionListener playAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int currentValue = timeSlider.getValue();
                // if slider is on maximum value and repeat button is selected, start from beginning
                if (currentValue == timeSlider.getMaximum() && repeatButton.isSelected()) {
                    currentValue = 0;
                } else if (currentValue == timeSlider.getMaximum() && !repeatButton.isSelected()) {
                    // if slider is on maximum value and repeat button is not selected, stop
                    playButton.setSelected(false);
                    timer.stop();
                    playButton.setIcon(playIcon);
                    playButton.setRolloverIcon(playIcon);
                    currentValue = 0;
                } else {
                    // if slider is not on maximum value, go on
                    currentValue++;
                }
                timeSlider.setValue(currentValue);
            }
        };

        timer = new Timer(timerDelay, playAction);

        final AbstractButton playButton = ToolButtonFactory.createButton(playIcon, false);
        playButton.setToolTipText("Play the time series");
        playButton.setRolloverIcon(playIcon);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playButton.getIcon() == playIcon) {
                    timer.start();
                    playButton.setIcon(pauseIcon);
                    playButton.setRolloverIcon(pauseIcon);
                } else { // pause
                    timer.stop();
                    int newValue = timeSlider.getValue() / stepsPerTimespan * stepsPerTimespan;
                    timeSlider.setValue(newValue);
                    playButton.setIcon(playIcon);
                    playButton.setRolloverIcon(playIcon);
                }
            }
        });
        return playButton;
    }

    private AbstractButton createStopButton() {
        final AbstractButton stopButton = ToolButtonFactory.createButton(stopIcon, false);
        stopButton.setToolTipText("Stop playing the time series");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                timeSlider.setValue(0);
                playButton.setIcon(playIcon);
                playButton.setRolloverIcon(playIcon);
                playButton.setSelected(false);
            }
        });
        return stopButton;
    }

    private AbstractButton createRepeatButton() {
        final AbstractButton repeatButton = ToolButtonFactory.createButton(repeatIcon, true);
        repeatButton.setToolTipText("Toggle repeat");
        return repeatButton;
    }

    private AbstractButton createBlendButton() {
        final AbstractButton blendButton = ToolButtonFactory.createButton(blendIcon, true);
        blendButton.setToolTipText("Toggle blending mode");
        blendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (blendButton.isSelected()) {
                    stepsPerTimespan = 8;
                    timeSlider.setValue(0);
                    timer.setDelay(calculateTimerDelay());
                    configureTimeSlider(currentView.getRaster());
                } else {
                    stepsPerTimespan = 1;
                    timeSlider.setValue(0);
                    timer.setDelay(calculateTimerDelay());
                    configureTimeSlider(currentView.getRaster());
                }
            }
        });
        return blendButton;
    }

    private AbstractButton createMinusButton() {
        final AbstractButton minusButton = ToolButtonFactory.createButton(minusIcon, false);
        minusButton.setToolTipText("Decrease playing speed");
        minusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (speedSlider.getValue() > speedSlider.getMinimum()) {
                    speedSlider.setValue(speedSlider.getValue() - 1);
                }
            }
        });
        return minusButton;
    }

    private JSlider createSpeedSlider() {
        final JSlider speedSlider = new JSlider(1, 10);
        speedSlider.setToolTipText("Choose the playing speed");
        speedSlider.setSnapToTicks(true);
        speedSlider.setPaintTrack(true);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setValue(6);
        speedSlider.setPreferredSize(new Dimension(80, speedSlider.getPreferredSize().height));
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                timerDelay = calculateTimerDelay();
                timer.setDelay(timerDelay);
                updateSpeedUnit();
            }
        });
        return speedSlider;
    }

    private AbstractButton createPlusButton() {
        final AbstractButton plusButton = ToolButtonFactory.createButton(plusIcon, false);
        plusButton.setToolTipText("Increase playing speed");
        plusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (speedSlider.getValue() < speedSlider.getMaximum()) {
                    speedSlider.setValue(speedSlider.getValue() + 1);
                }
            }
        });
        return plusButton;
    }

    private AbstractButton createExportButton() {
        final AbstractButton exportButton = ToolButtonFactory.createButton(exportIcon, false);
        exportButton.setToolTipText("Export as animated gif");
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final AnimatedGifExport export = new AnimatedGifExport(TimeSeriesPlayerForm.this,
                                                                       "Export time series as animated gif");
                final String varName = AbstractTimeSeries.rasterToVariableName(currentView.getRaster().getName());
                export.createFrames(timeSeries.getBandsForVariable(varName));
                export.executeWithBlocking();
            }
        });
        return exportButton;
    }

    private void setUIEnabled(boolean enable) {
        dateLabel.setEnabled(enable);
        timeSlider.setPaintLabels(enable);
        timeSlider.setEnabled(enable);
        playButton.setEnabled(enable);
        stopButton.setEnabled(enable);
        repeatButton.setEnabled(enable);
        blendButton.setEnabled(enable);
        speedLabel.setEnabled(enable);
        minusButton.setEnabled(enable);
        speedSlider.setEnabled(enable);
        speedUnit.setEnabled(enable);
        plusButton.setEnabled(enable);
        exportButton.setEnabled(enable);
    }

    private int calculateTimerDelay() {
        return 250 / stepsPerTimespan * (11 - speedSlider.getValue());
    }

    private void updateSpeedUnit() {
        double fps = 1 / (timerDelay * stepsPerTimespan / 1000.0);
        DecimalFormat formatter = new DecimalFormat("0.00");
        speedUnit.setText(formatter.format(fps) + " FPS");
        speedUnit.setToolTipText(formatter.format(fps) + " Frames per second");
    }

    private static TableLayout createLayout() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setRowPadding(0, new Insets(4, 4, 4, 0));
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setRowFill(1, TableLayout.Fill.BOTH);
        return tableLayout;
    }
}
