/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;

/**
 * User interface for RangeDopplerGeocodingOp
 */
public class CalibrationOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox auxFile = new JComboBox(new String[] {CalibrationOp.LATEST_AUX,
                                                                  CalibrationOp.PRODUCT_AUX,
                                                                  CalibrationOp.EXTERNAL_AUX});

    private final JLabel auxFileLabel = new JLabel("ENVISAT Auxiliary File:");
    private final JLabel externalAuxFileLabel = new JLabel("External Auxiliary File:");
    private final JTextField externalAuxFile = new JTextField("");
    private final JButton externalAuxFileBrowseButton = new JButton("...");

    private final JCheckBox saveInComplexCheckBox = new JCheckBox("Save as complex output");
    private final JCheckBox saveInDbCheckBox = new JCheckBox("Save in dB");
    private final JCheckBox createGamma0VirtualBandCheckBox = new JCheckBox("Create gamma0 virtual band");
    private final JCheckBox createBeta0VirtualBandCheckBox = new JCheckBox("Create beta0 virtual band");

    private boolean saveInComplex = false;
    private boolean saveInDb = false;
    private boolean createGamma0VirtualBand = false;
    private boolean createBeta0VirtualBand = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        auxFile.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String item = (String)auxFile.getSelectedItem();
                if(item.equals(CalibrationOp.EXTERNAL_AUX)) {
                    enableExternalAuxFile(true);
                } else {
                    externalAuxFile.setText("");
                    enableExternalAuxFile(false);
                }
            }
        });
        externalAuxFile.setColumns(20);
        auxFile.setSelectedItem(parameterMap.get("auxFile"));
        enableExternalAuxFile(false);

        externalAuxFileBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File file = VisatApp.getApp().showFileOpenDialog("External Auxiliary File", false, null);
                externalAuxFile.setText(file.getAbsolutePath());
            }
        });

        saveInComplexCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {

                    saveInComplex = (e.getStateChange() == ItemEvent.SELECTED);

                    if (saveInComplex) {
                        saveInDbCheckBox.setEnabled(false);
                        createGamma0VirtualBandCheckBox.setEnabled(false);
                        createBeta0VirtualBandCheckBox.setEnabled(false);
                        saveInDbCheckBox.setSelected(false);
                        createGamma0VirtualBandCheckBox.setSelected(false);
                        createBeta0VirtualBandCheckBox.setSelected(false);
                    } else {
                        saveInDbCheckBox.setEnabled(true);
                        createGamma0VirtualBandCheckBox.setEnabled(true);
                        createBeta0VirtualBandCheckBox.setEnabled(true);
                    }
                }
        });

        saveInDbCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveInDb = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        createGamma0VirtualBandCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    createGamma0VirtualBand = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        createBeta0VirtualBandCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    createBeta0VirtualBand = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        return panel;
    }

    @Override
    public void initParameters() {
        OperatorUIUtils.initParamList(bandList, getBandNames(), (Object[])paramMap.get("sourceBands"));

        if(sourceProducts != null) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            if (absRoot != null) {

                final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
                if (sampleType.equals("COMPLEX")) {
                    auxFile.removeItem(CalibrationOp.PRODUCT_AUX);
                    auxFile.setSelectedItem(paramMap.get("auxFile"));

                } else {
                    if (auxFile.getItemCount() == 2) {
                        auxFile.addItem(CalibrationOp.PRODUCT_AUX);
                    }
                    auxFile.setSelectedItem(CalibrationOp.PRODUCT_AUX);
                }

                final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
                if(!mission.equals("ENVISAT")) {
                    auxFile.setEnabled(false);
                    auxFileLabel.setEnabled(false);
                } else {
                    auxFile.setEnabled(true);
                    auxFileLabel.setEnabled(true);
                }
//                if (mission.equals("RS2") || mission.contains("TSX") || mission.contains("ALOS")) {
                if (mission.equals("RS2") && sampleType.equals("COMPLEX")) {

                    saveInComplexCheckBox.setEnabled(true);
                    saveInComplexCheckBox.setSelected(false);

                    if (saveInComplex) {
                        saveInDbCheckBox.setEnabled(false);
                        createGamma0VirtualBandCheckBox.setEnabled(false);
                        createBeta0VirtualBandCheckBox.setEnabled(false);
                        saveInDbCheckBox.setSelected(false);
                        createGamma0VirtualBandCheckBox.setSelected(false);
                        createBeta0VirtualBandCheckBox.setSelected(false);
                    } else {
                        saveInDbCheckBox.setEnabled(true);
                        createGamma0VirtualBandCheckBox.setEnabled(true);
                        createBeta0VirtualBandCheckBox.setEnabled(true);
                    }

                } else {
                    saveInComplexCheckBox.setEnabled(false);
                    saveInComplexCheckBox.setSelected(false);
                }
            }
        } else {

            auxFile.setSelectedItem(paramMap.get("auxFile"));
        }

        final File extFile = (File)paramMap.get("externalAuxFile");
        if(extFile != null) {
            externalAuxFile.setText(extFile.getAbsolutePath());
        }

        saveInComplex = (Boolean)paramMap.get("outputImageInComplex");
        saveInComplexCheckBox.setSelected(saveInComplex);

        saveInDb = (Boolean)paramMap.get("outputImageScaleInDb");
        saveInDbCheckBox.setSelected(saveInDb);

        createGamma0VirtualBand = (Boolean)paramMap.get("createGammaBand");
        createGamma0VirtualBandCheckBox.setSelected(createGamma0VirtualBand);

        createBeta0VirtualBand = (Boolean)paramMap.get("createBetaBand");
        createBeta0VirtualBandCheckBox.setSelected(createBeta0VirtualBand);
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("auxFile", auxFile.getSelectedItem());

        final String extFileStr = externalAuxFile.getText();
        if(!extFileStr.isEmpty()) {
            paramMap.put("externalAuxFile", new File(extFileStr));
        }

        paramMap.put("outputImageInComplex", saveInComplex);
        paramMap.put("outputImageScaleInDb", saveInDb);
        paramMap.put("createGammaBand", createGamma0VirtualBand);
        paramMap.put("createBetaBand", createBeta0VirtualBand);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));

        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, auxFileLabel, auxFile);
        gbc.gridy++;
        DialogUtils.addInnerPanel(contentPane, gbc, externalAuxFileLabel, externalAuxFile,
                                  externalAuxFileBrowseButton);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(saveInComplexCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(saveInDbCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(createGamma0VirtualBandCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(createBeta0VirtualBandCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableExternalAuxFile(boolean flag) {
        DialogUtils.enableComponents(externalAuxFileLabel, externalAuxFile, flag);
        externalAuxFileBrowseButton.setVisible(flag);
    }
}