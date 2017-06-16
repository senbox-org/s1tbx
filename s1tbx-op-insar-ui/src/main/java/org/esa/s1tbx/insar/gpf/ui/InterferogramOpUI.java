/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.gpf.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.s1tbx.insar.gpf.CoherenceOp;
import org.esa.s1tbx.insar.rcp.toolviews.InSARStatisticsTopComponent;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModalDialog;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;

/**
 * User interface for CreateInterferogramOp
 */
public class InterferogramOpUI extends BaseOperatorUI {

    private final JCheckBox subtractFlatEarthPhaseCheckBox = new JCheckBox("Subtract flat-earth phase");
    private final JCheckBox subtractTopographicPhaseCheckBox = new JCheckBox("Subtract topographic phase");
    private final JCheckBox includeCoherenceCheckBox = new JCheckBox("Include coherence estimation");
    private final JCheckBox squarePixelCheckBox = new JCheckBox("Square Pixel");
    private final JCheckBox independentWindowSizeCheckBox = new JCheckBox("Independent Window Sizes");

    private final JTextField cohWinAz = new JTextField("");
    private final JTextField cohWinRg = new JTextField("");

    private final JComboBox<Integer> srpPolynomialDegreeStr = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8});
    private final JComboBox<Integer> srpNumberPointsStr = new JComboBox(new Integer[]{301, 401, 501, 601, 701, 801, 901, 1001});
    private final JComboBox<Integer> orbitDegreeStr = new JComboBox(new Integer[]{1, 2, 3, 4, 5});

    private static final JLabel cohWinAzLabel = new JLabel("Coherence Azimuth Window Size");
    private static final JLabel cohWinRgLabel = new JLabel("Coherence Range Window Size");
    private static final JLabel srpPolynomialDegreeStrLabel = new JLabel("Degree of \"Flat Earth\" polynomial");
    private static final JLabel srpNumberPointsStrLabel = new JLabel("Number of \"Flat Earth\" estimation points");
    private static final JLabel orbitDegreeStrLabel = new JLabel("Orbit interpolation degree");

    private Boolean subtractFlatEarthPhase = false;
    private Boolean includeCoherence = true;
    private Boolean squarePixel = true;
    private final CoherenceOp.DerivedParams param = new CoherenceOp.DerivedParams();

    private Boolean subtractTopographicPhase = false;
    private static final String[] demValueSet = DEMFactory.getDEMNameList();
    //    private final JTextField orbitDegree = new JTextField("");
    private final JComboBox<String> demName = new JComboBox<>(demValueSet);
    private static final String externalDEMStr = "External DEM";
    private final JTextField externalDEMFile = new JTextField("");
    private final JTextField externalDEMNoDataValue = new JTextField("");
    private final JButton externalDEMBrowseButton = new JButton("...");
    private final JLabel externalDEMFileLabel = new JLabel("External DEM");
    private final JLabel externalDEMNoDataValueLabel = new JLabel("DEM No Data Value:");
    private final JCheckBox externalDEMApplyEGMCheckBox = new JCheckBox("Apply Earth Gravitational Model");
    private final DialogUtils.TextAreaKeyListener textAreaKeyListener = new DialogUtils.TextAreaKeyListener();
    private final JComboBox<String> tileExtensionPercent = new JComboBox<>(new String[]{"20", "40", "60", "80", "100", "150", "200"});
    private Double extNoDataValue = 0.0;
    private Boolean externalDEMApplyEGM = true;

    private static final JLabel pairSelButtonLabel = new JLabel("Master-Slave Pair Selection");
    private final JButton pairSelButton = new JButton("Pair Selection");
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private JTextPane textArea;

    final JCheckBox mstToAllSlvCheckBox = new JCheckBox("Master to all slaves");
    final JList bandList = new JList();
    final JScrollPane bandListPane = new JScrollPane(bandList);
    final JLabel bandListLabel = new JLabel("Source Bands:");

    final JCheckBox baselineCheckBox = new JCheckBox("Baseline based selection");
    final JTextField maxGeoBaseline = new JTextField("");
    final JTextField maxTempBaseline = new JTextField("");
    final JLabel maxGeoBaselineLabel = new JLabel("Max Geometrical Baseline (m)");
    final JLabel maxTempBaselineLabel = new JLabel("Max Temporal Baseline (day)");

    private JPanel panel;
    private ChartPanel chartPanel;
    private JTextArea textarea;
    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private static final String TITLE = "Baselines";
    private static final String XAXIS_LABEL = "Temporal Baseline";
    private static final String YAXIS_LABEL = "Perpendicular Baseline";
    public static final String EmptyMsg = "This tool window requires a coregistered stack product.";

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = new JScrollPane(createPanel());
        initParameters();

        subtractFlatEarthPhaseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {

                subtractFlatEarthPhase = (e.getStateChange() == ItemEvent.SELECTED);
                if (subtractFlatEarthPhase) {
                    srpPolynomialDegreeStr.setEnabled(true);
                    srpNumberPointsStr.setEnabled(true);
                    orbitDegreeStr.setEnabled(true);
                } else {
                    srpPolynomialDegreeStr.setEnabled(false);
                    srpNumberPointsStr.setEnabled(false);
                    orbitDegreeStr.setEnabled(false);
                }
            }
        });

        includeCoherenceCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {

                includeCoherence = (e.getStateChange() == ItemEvent.SELECTED);
                if (includeCoherence) {
                    squarePixelCheckBox.setEnabled(true);
                    independentWindowSizeCheckBox.setEnabled(true);
                    cohWinAz.setEnabled(true);
                    cohWinRg.setEnabled(true);
                } else {
                    squarePixelCheckBox.setEnabled(false);
                    independentWindowSizeCheckBox.setEnabled(false);
                    cohWinAz.setEnabled(false);
                    cohWinRg.setEnabled(false);
                }
            }
        });

        squarePixelCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                squarePixel = (e.getStateChange() == ItemEvent.SELECTED);
                independentWindowSizeCheckBox.setSelected(!squarePixel);
                if (squarePixel) {
                    cohWinAz.setText("2");
                    cohWinAz.setEditable(false);
                }
                setCohWinAz();
                setCohWinRg();
            }
        });

        independentWindowSizeCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                squarePixel = (e.getStateChange() != ItemEvent.SELECTED);
                squarePixelCheckBox.setSelected(squarePixel);
                if (!squarePixel) {
                    cohWinAz.setEditable(true);
                }
                setCohWinAz();
                setCohWinRg();
            }
        });

        subtractTopographicPhaseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {

                subtractTopographicPhase = (e.getStateChange() == ItemEvent.SELECTED);
                if (subtractTopographicPhase) {
                    demName.setEnabled(true);
                    tileExtensionPercent.setEnabled(true);
                } else {
                    demName.setEnabled(false);
                    tileExtensionPercent.setEnabled(false);
                }
            }
        });

        demName.addItem(externalDEMStr);

        demName.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String item = ((String) demName.getSelectedItem()).replace(DEMFactory.AUTODEM, "");
                if (item.equals(externalDEMStr)) {
                    enableExternalDEM(true);
                } else {
                    externalDEMFile.setText("");
                    enableExternalDEM(false);
                }
            }
        });
        externalDEMFile.setColumns(30);
        enableExternalDEM(((String) demName.getSelectedItem()).startsWith(externalDEMStr));

        externalDEMBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File file = Dialogs.requestFileForOpen("External DEM File", false, null, DEMFactory.LAST_EXTERNAL_DEM_DIR_KEY);
                if (file != null) {
                    externalDEMFile.setText(file.getAbsolutePath());
                    extNoDataValue = OperatorUIUtils.getNoDataValue(file);
                }
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        });

        externalDEMApplyEGMCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                externalDEMApplyEGM = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        externalDEMNoDataValue.addKeyListener(textAreaKeyListener);

        pairSelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                promptPairSelection();
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        Boolean paramVal;
        paramVal = (Boolean) paramMap.get("subtractFlatEarthPhase");
        if (paramVal != null) {
            subtractFlatEarthPhase = paramVal;
            subtractFlatEarthPhaseCheckBox.setSelected(subtractFlatEarthPhase);
        }

        srpPolynomialDegreeStr.setSelectedItem(paramMap.get("srpPolynomialDegree"));
        srpNumberPointsStr.setSelectedItem(paramMap.get("srpNumberPoints"));
        orbitDegreeStr.setSelectedItem(paramMap.get("orbitDegree"));

        if (subtractFlatEarthPhase) {
            srpPolynomialDegreeStr.setEnabled(true);
            srpNumberPointsStr.setEnabled(true);
            orbitDegreeStr.setEnabled(true);
        }
        paramVal = (Boolean) paramMap.get("subtractTopographicPhase");
        if (paramVal != null) {
            subtractTopographicPhase = paramVal;
            subtractTopographicPhaseCheckBox.setSelected(subtractTopographicPhase);
        }

//        orbitDegree.setText(String.valueOf(paramMap.get("orbitDegree")));
        final String demNameParam = (String) paramMap.get("demName");
        if (demNameParam != null) {
            ElevationModelDescriptor descriptor = ElevationModelRegistry.getInstance().getDescriptor(demNameParam);
            if(descriptor != null) {
                demName.setSelectedItem(DEMFactory.getDEMDisplayName(descriptor));
            } else {
                demName.setSelectedItem(demNameParam);
            }
        }

        final File extFile = (File)paramMap.get("externalDEMFile");
        if(extFile != null) {
            externalDEMFile.setText(extFile.getAbsolutePath());
            extNoDataValue =  (Double)paramMap.get("externalDEMNoDataValue");
            if(extNoDataValue != null && !textAreaKeyListener.isChangedByUser()) {
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }

            paramVal = (Boolean) paramMap.get("externalDEMApplyEGM");
            if (paramVal != null) {
                externalDEMApplyEGM = paramVal;
                externalDEMApplyEGMCheckBox.setSelected(externalDEMApplyEGM);
            }
        }

        tileExtensionPercent.setSelectedItem(paramMap.get("tileExtensionPercent"));

        paramVal = (Boolean) paramMap.get("includeCoherence");
        if (paramVal != null) {
            includeCoherence = paramVal;
            includeCoherenceCheckBox.setSelected(includeCoherence);
        }

        cohWinAz.setText(String.valueOf(paramMap.get("cohWinAz")));
        cohWinRg.setText(String.valueOf(paramMap.get("cohWinRg")));

        squarePixel = (Boolean) paramMap.get("squarePixel");
        if (squarePixel != null) {
            squarePixelCheckBox.setSelected(squarePixel);
            independentWindowSizeCheckBox.setSelected(!squarePixel);
            if (squarePixel) {
                cohWinAz.setText("2");
                cohWinAz.setEditable(false);
            } else {
                cohWinAz.setEditable(true);
            }
        }

        setCohWinAz();
        setCohWinRg();

        if (includeCoherence) {
            squarePixelCheckBox.setEnabled(true);
            independentWindowSizeCheckBox.setEnabled(true);
            cohWinAz.setEnabled(true);
            cohWinRg.setEnabled(true);
        }
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("subtractFlatEarthPhase", subtractFlatEarthPhase);

        if (subtractFlatEarthPhase) {
            paramMap.put("srpPolynomialDegree", srpPolynomialDegreeStr.getSelectedItem());
            paramMap.put("srpNumberPoints", srpNumberPointsStr.getSelectedItem());
            paramMap.put("orbitDegree", orbitDegreeStr.getSelectedItem());
        }

        paramMap.put("subtractTopographicPhase", subtractTopographicPhase);
        if (subtractTopographicPhase) {
//          paramMap.put("orbitDegree", Integer.parseInt(orbitDegree.getText()));
            final String properDEMName = (DEMFactory.getProperDEMName((String) demName.getSelectedItem()));
            paramMap.put("demName", (DEMFactory.getProperDEMName((String) demName.getSelectedItem())));
            if(properDEMName.equals(externalDEMStr)) {
                final String extFileStr = externalDEMFile.getText();
                paramMap.put("externalDEMFile", new File(extFileStr));
                paramMap.put("externalDEMNoDataValue", Double.parseDouble(externalDEMNoDataValue.getText()));
                paramMap.put("externalDEMApplyEGM", externalDEMApplyEGM);
            }
            paramMap.put("tileExtensionPercent", tileExtensionPercent.getSelectedItem());
        }

        paramMap.put("includeCoherence", includeCoherence);
        if (includeCoherence) {
            final String cohWinRgStr = cohWinRg.getText();
            final String cohWinAzStr = cohWinAz.getText();
            if (cohWinRgStr != null && !cohWinRgStr.isEmpty())
                paramMap.put("cohWinRg", Integer.parseInt(cohWinRg.getText()));

            if (cohWinAzStr != null && !cohWinAzStr.isEmpty())
                paramMap.put("cohWinAz", Integer.parseInt(cohWinAz.getText()));

            paramMap.put("squarePixel", squarePixel);
        }
    }

    JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(subtractFlatEarthPhaseCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, srpPolynomialDegreeStrLabel, srpPolynomialDegreeStr);
        srpPolynomialDegreeStr.setEnabled(false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, srpNumberPointsStrLabel, srpNumberPointsStr);
        srpNumberPointsStr.setEnabled(false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, orbitDegreeStrLabel, orbitDegreeStr);
        orbitDegreeStr.setEnabled(false);

        gbc.gridy++;
        contentPane.add(subtractTopographicPhaseCheckBox, gbc);

//        gbc.gridy++;
//        DialogUtils.addComponent(contentPane, gbc, "Orbit Interpolation Degree:", orbitDegree);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Digital Elevation Model:", demName);
        gbc.gridy++;
        DialogUtils.addInnerPanel(contentPane, gbc, externalDEMFileLabel, externalDEMFile, externalDEMBrowseButton);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, externalDEMNoDataValueLabel, externalDEMNoDataValue);
        gbc.gridy++;
        gbc.gridx = 1;
        contentPane.add(externalDEMApplyEGMCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = gbc.gridy + 10;
        DialogUtils.addComponent(contentPane, gbc, "Tile Extension [%]", tileExtensionPercent);

        demName.setEnabled(false);
        tileExtensionPercent.setEnabled(false);

        gbc.gridy++;
        contentPane.add(includeCoherenceCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(squarePixelCheckBox, gbc);
        squarePixelCheckBox.setEnabled(false);

        gbc.gridx = 1;
        contentPane.add(independentWindowSizeCheckBox, gbc);
        independentWindowSizeCheckBox.setEnabled(false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWinRgLabel, cohWinRg);
        cohWinRg.setEnabled(false);
        cohWinRg.setDocument(new CohWinRgDocument());

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWinAzLabel, cohWinAz);
        cohWinAz.setEnabled(false);
        cohWinAz.setEditable(false);

        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, pairSelButtonLabel, pairSelButton);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }


    private synchronized void setCohWinAz() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            try {
                if (squarePixelCheckBox.isSelected()) {
                    param.cohWinRg = Integer.parseInt(cohWinRg.getText());
                    CoherenceOp.getDerivedParameters(sourceProducts[0], param);
                    cohWinAz.setText(String.valueOf(param.cohWinAz));
                }
            } catch (Exception e) {
            }
        }
    }

    private void setCohWinRg() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            if (squarePixelCheckBox.isSelected()) {
                cohWinRg.setText(String.valueOf(param.cohWinRg));
            }
        }
    }

    @SuppressWarnings("serial")
    private class CohWinRgDocument extends PlainDocument {

        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(offset, length, text, attrs);

            setCohWinAz();
        }
    }

    private void enableExternalDEM(boolean flag) {
        DialogUtils.enableComponents(externalDEMFileLabel, externalDEMFile, flag);
        DialogUtils.enableComponents(externalDEMNoDataValueLabel, externalDEMNoDataValue, flag);
        if(!flag) {
            externalDEMFile.setText("");
        }
        externalDEMBrowseButton.setVisible(flag);
        externalDEMApplyEGMCheckBox.setVisible(flag);
    }


    private void promptPairSelection() {

        final TopComponent pairSelTopComponent = new TopComponent();
        pairSelTopComponent.setLayout(new BorderLayout());
        pairSelTopComponent.setDisplayName("Master-Slave Pair Selection");
        pairSelTopComponent.add(createTabbedPanel(), BorderLayout.CENTER);
        pairSelTopComponent.open();
        pairSelTopComponent.requestActive();
    }

    private JTabbedPane createTabbedPanel() {

        tabbedPane.removeAll();
        tabbedPane.add("Pair Selection", createTab1Panel());
        tabbedPane.add("Baseline Plot", createTab2Panel());
        tabbedPane.setSelectedIndex(0);

        return tabbedPane;
    }

    private Component createTab1Panel() {

        //=======================================
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(mstToAllSlvCheckBox, gbc);
        mstToAllSlvCheckBox.setSelected(true);
        gbc.gridy++;

        DialogUtils.addComponent(contentPane, gbc, bandListLabel, bandListPane);
        gbc.gridy++;
        gbc.gridy++;

        contentPane.add(baselineCheckBox, gbc);
        baselineCheckBox.setSelected(false);
        gbc.gridy++;

        DialogUtils.addComponent(contentPane, gbc, maxGeoBaselineLabel, maxGeoBaseline);
        gbc.gridy++;

        DialogUtils.addComponent(contentPane, gbc, maxTempBaselineLabel, maxTempBaseline);
        gbc.gridy++;
        enableBaselineFields(false);

        //=======================================
        OperatorUIUtils.initParamList(bandList, getBandNames(), (Object[])paramMap.get("sourceBands"));

        mstToAllSlvCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
                baselineCheckBox.setSelected(!isSelected);
                enableSourceBandsField(isSelected);
                enableBaselineFields(!isSelected);
            }
        });

        baselineCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                final boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
                mstToAllSlvCheckBox.setSelected(!isSelected);
                enableSourceBandsField(!isSelected);
                enableBaselineFields(isSelected);
            }
        });

        return contentPane;
    }

    private Component createTab2Panel() {

        // Add the series to your data set
        dataset = new XYSeriesCollection();

        // Generate the graph
        chart = ChartFactory.createXYLineChart(
                TITLE, // Title
                XAXIS_LABEL, // x-axis Label
                YAXIS_LABEL, // y-axis Label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // Plot Orientation
                true, // Show Legend
                true, // Use tooltips
                false // Configure chart to generate URLs?
        );

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 470));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel = new JPanel(new BorderLayout());
        textarea = new JTextArea(EmptyMsg);
        panel.add(textarea, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        setVisible(false);

        return panel;

        /*textArea = new JTextPane();
        textArea.setContentType("text/html");
        return new JScrollPane(textArea);*/
    }

    private void setVisible(final boolean flag) {
        textarea.setVisible(!flag);
        chartPanel.setVisible(flag);
    }

    private void enableSourceBandsField(final boolean isSelected) {
        bandListPane.setEnabled(isSelected);
        bandListPane.setRequestFocusEnabled(isSelected);
    }

    private void enableBaselineFields(final boolean isSelected) {
        maxGeoBaseline.setEnabled(isSelected);
        maxTempBaseline.setEnabled(isSelected);
    }
}
