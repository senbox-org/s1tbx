/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.calibration.gpf.ui;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;

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
    private final JScrollPane bandListPane = new JScrollPane(bandList);
    private final JLabel bandListLabel = new JLabel("Source Bands:");
    private final JComboBox auxFile = new JComboBox(new String[]{CalibrationOp.LATEST_AUX,
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

    private final JList<String> polList = new JList<>();
    private final JScrollPane polListPane = new JScrollPane(polList);
    private final JLabel polListLabel = new JLabel("Polarisations:");
    private final JCheckBox outputSigmaBandCheckBox = new JCheckBox("Output sigma0 band");
    private final JCheckBox outputGammaBandCheckBox = new JCheckBox("Output gamma0 band");
    private final JCheckBox outputBetaBandCheckBox = new JCheckBox("Output beta0 band");
    private final JCheckBox outputDNBandCheckBox = new JCheckBox("Output DN band");
    private boolean outputSigmaBand = false;
    private boolean outputGammaBand = false;
    private boolean outputBetaBand = false;
    private boolean outputDNBand = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        auxFile.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String item = (String) auxFile.getSelectedItem();
                if (item.equals(CalibrationOp.EXTERNAL_AUX)) {
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
                final File file = Dialogs.requestFileForOpen("External Auxiliary File", false, null, "s1tbx.calibration.aux.file");
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

                    outputSigmaBandCheckBox.setEnabled(false);
                    outputGammaBandCheckBox.setEnabled(false);
                    outputBetaBandCheckBox.setEnabled(false);
                    outputDNBandCheckBox.setEnabled(false);
                    outputSigmaBandCheckBox.setSelected(true);
                    outputGammaBandCheckBox.setSelected(false);
                    outputBetaBandCheckBox.setSelected(false);
                    outputDNBandCheckBox.setSelected(false);

                } else {
                    saveInDbCheckBox.setEnabled(true);
                    createGamma0VirtualBandCheckBox.setEnabled(true);
                    createBeta0VirtualBandCheckBox.setEnabled(true);

                    outputSigmaBandCheckBox.setEnabled(true);
                    outputGammaBandCheckBox.setEnabled(true);
                    outputBetaBandCheckBox.setEnabled(true);
                    outputDNBandCheckBox.setEnabled(true);
                    outputSigmaBandCheckBox.setSelected(true);
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

        outputSigmaBandCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputSigmaBand = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputGammaBandCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputGammaBand = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputBetaBandCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputBetaBand = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputDNBandCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputDNBand = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames(), (Object[])paramMap.get("sourceBands"));

        if (sourceProducts != null) {
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
                if (!mission.equals("ENVISAT")) {
                    auxFile.setEnabled(false);
                    auxFileLabel.setEnabled(false);
                } else {
                    auxFile.setEnabled(true);
                    auxFileLabel.setEnabled(true);
                }

                DialogUtils.enableComponents(auxFileLabel, auxFile, true);
                DialogUtils.enableComponents(bandListLabel, bandListPane, true);
                saveInComplexCheckBox.setVisible(true);
                saveInDbCheckBox.setVisible(true);
                createGamma0VirtualBandCheckBox.setVisible(true);
                createBeta0VirtualBandCheckBox.setVisible(true);

                DialogUtils.enableComponents(polListLabel, polListPane, false);
                outputSigmaBandCheckBox.setVisible(false);
                outputGammaBandCheckBox.setVisible(false);
                outputBetaBandCheckBox.setVisible(false);
                outputDNBandCheckBox.setVisible(false);

                saveInComplexCheckBox.setSelected(false);

                if (sampleType.equals("COMPLEX")) {
                    saveInComplexCheckBox.setEnabled(true);
                } else {
                    saveInComplexCheckBox.setEnabled(false);
                }

                if (mission.startsWith("SENTINEL-1")) {

                    OperatorUIUtils.initParamList(polList, Sentinel1Utils.getProductPolarizations(absRoot),
                            (String[]) paramMap.get("selectedPolarisations"));


                    DialogUtils.enableComponents(bandListLabel, bandListPane, false);
                    saveInDbCheckBox.setVisible(false);
                    createGamma0VirtualBandCheckBox.setVisible(false);
                    createBeta0VirtualBandCheckBox.setVisible(false);
                    DialogUtils.enableComponents(polListLabel, polListPane, true);
                    outputSigmaBandCheckBox.setVisible(true);
                    outputGammaBandCheckBox.setVisible(true);
                    outputBetaBandCheckBox.setVisible(true);
                    outputDNBandCheckBox.setVisible(true);
                }
            }

        } else {

            auxFile.setSelectedItem(paramMap.get("auxFile"));
        }

        final File extFile = (File) paramMap.get("externalAuxFile");
        if (extFile != null) {
            externalAuxFile.setText(extFile.getAbsolutePath());
        }

        Boolean paramVal;
        paramVal = (Boolean) paramMap.get("outputImageInComplex");
        if (paramVal != null) {
            saveInComplex = paramVal;
            saveInComplexCheckBox.setSelected(saveInComplex);
        }
        paramVal = (Boolean) paramMap.get("outputImageScaleInDb");
        if (paramVal != null) {
            saveInDb = paramVal;
            saveInDbCheckBox.setSelected(saveInDb);
        }
        paramVal = (Boolean) paramMap.get("createGammaBand");
        if (paramVal != null) {
            createGamma0VirtualBand = paramVal;
            createGamma0VirtualBandCheckBox.setSelected(createGamma0VirtualBand);
        }
        paramVal = (Boolean) paramMap.get("createBetaBand");
        if (paramVal != null) {
            createBeta0VirtualBand = paramVal;
            createBeta0VirtualBandCheckBox.setSelected(createBeta0VirtualBand);
        }
        paramVal = (Boolean) paramMap.get("outputSigmaBand");
        if (paramVal != null) {
            outputSigmaBand = paramVal;
            outputSigmaBandCheckBox.setSelected(outputSigmaBand);
        }
        paramVal = (Boolean) paramMap.get("outputGammaBand");
        if (paramVal != null) {
            outputGammaBand = paramVal;
            outputGammaBandCheckBox.setSelected(outputGammaBand);
        }
        paramVal = (Boolean) paramMap.get("outputBetaBand");
        if (paramVal != null) {
            outputBetaBand = paramVal;
            outputBetaBandCheckBox.setSelected(outputBetaBand);
        }
        paramVal = (Boolean) paramMap.get("outputDNBand");
        if (paramVal != null) {
            outputDNBand = paramVal;
            outputDNBandCheckBox.setSelected(outputDNBand);
        }
    }

    @Override
    public UIValidation validateParameters() {

        if (sourceProducts != null) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            if (absRoot != null) {
                final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
                if (mission.startsWith("SENTINEL-1")) {
                    final String procSysId = absRoot.getAttributeString(AbstractMetadata.ProcessingSystemIdentifier);
                    final float version = Float.valueOf(procSysId.substring(procSysId.lastIndexOf(" ")));
                    if (version < 2.34f) {
                        return new UIValidation(UIValidation.State.WARNING,
                                "The calibration LUT for this product could be incorrect and " +
                                        "therefore the calibration result may not be reliable.");
                    }
                }
            }
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("auxFile", auxFile.getSelectedItem());

        final String extFileStr = externalAuxFile.getText();
        if (!extFileStr.isEmpty()) {
            paramMap.put("externalAuxFile", new File(extFileStr));
        }

        paramMap.put("outputImageInComplex", saveInComplex);
        paramMap.put("outputImageScaleInDb", saveInDb);
        paramMap.put("createGammaBand", createGamma0VirtualBand);
        paramMap.put("createBetaBand", createBeta0VirtualBand);

        OperatorUIUtils.updateParamList(polList, paramMap, "selectedPolarisations");
        paramMap.put("outputSigmaBand", outputSigmaBand);
        paramMap.put("outputGammaBand", outputGammaBand);
        paramMap.put("outputBetaBand", outputBetaBand);
        paramMap.put("outputDNBand", outputDNBand);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, bandListLabel, bandListPane);
        DialogUtils.addComponent(contentPane, gbc, polListLabel, polListPane);

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
        contentPane.add(outputSigmaBandCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(createGamma0VirtualBandCheckBox, gbc);
        contentPane.add(outputGammaBandCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(createBeta0VirtualBandCheckBox, gbc);
        contentPane.add(outputBetaBandCheckBox, gbc);

//        gbc.gridy++;
//        contentPane.add(outputDNBandCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        DialogUtils.enableComponents(polListLabel, polListPane, false);
        outputSigmaBandCheckBox.setVisible(false);
        outputGammaBandCheckBox.setVisible(false);
        outputBetaBandCheckBox.setVisible(false);
        outputDNBandCheckBox.setVisible(false);

        return contentPane;
    }

    private void enableExternalAuxFile(boolean flag) {
        DialogUtils.enableComponents(externalAuxFileLabel, externalAuxFile, flag);
        externalAuxFileBrowseButton.setVisible(flag);
    }
}
