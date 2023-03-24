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
package org.esa.s1tbx.sar.gpf.ui.geometric;

import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.dem.dataio.DEMFactory;
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
 * User interface for TerrainFlatteningOp
 */
public class TerrainFlatteningOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox<String> demName = new JComboBox<>(DEMFactory.getDEMNameList());
    private static final String externalDEMStr = "External DEM";
    private final JCheckBox externalDEMApplyEGMCheckBox = new JCheckBox("External DEM Apply EGM");
    private final JCheckBox outputSimulatedImageCheckBox = new JCheckBox("Output Simulated Image");
    private final JCheckBox outputGamma0CheckBox = new JCheckBox("Output Terrain Flattened Gamma0");
    private final JCheckBox outputSigma0CheckBox = new JCheckBox("Output Terrain Flattened Sigma0");
    private final JCheckBox nodataValueAtSeaCheckBox = new JCheckBox("Mask out areas without elevation");

    private final JComboBox<String> demResamplingMethod = new JComboBox<>(ResamplingFactory.resamplingNames);
    private final JTextField externalDEMFile = new JTextField("");
    private final JTextField externalDEMNoDataValue = new JTextField("");
    private final JTextField additionalOverlap = new JTextField("");
    private final JTextField oversamplingMultiple = new JTextField("");
    private final JButton externalDEMBrowseButton = new JButton("...");
    private final JLabel externalDEMFileLabel = new JLabel("External DEM:");
    private final JLabel externalDEMNoDataValueLabel = new JLabel("DEM No Data Value:");
    private Double extNoDataValue = 0.0;
    private Boolean externalDEMApplyEGM = false;
    private Boolean outputSimulatedImage = false;
    private Boolean outputSigma0 = false;
    private Boolean nodataValueAtSea = true;

    private final DialogUtils.TextAreaKeyListener textAreaKeyListener = new DialogUtils.TextAreaKeyListener();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        demName.addItem(externalDEMStr);

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = createPanel();
        initParameters();

        demName.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (((String) demName.getSelectedItem()).startsWith(externalDEMStr)) {
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
                externalDEMFile.setText(file.getAbsolutePath());
                extNoDataValue = OperatorUIUtils.getNoDataValue(file);
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        });

        externalDEMNoDataValue.addKeyListener(textAreaKeyListener);

        externalDEMApplyEGMCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                externalDEMApplyEGM = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputSimulatedImageCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputSimulatedImage = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputSigma0CheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputSigma0 = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        nodataValueAtSeaCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                nodataValueAtSea = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        final String demNameParam = (String) paramMap.get("demName");
        if (demNameParam != null) {
            ElevationModelDescriptor descriptor = ElevationModelRegistry.getInstance().getDescriptor(demNameParam);
            if(descriptor != null) {
                demName.setSelectedItem(DEMFactory.getDEMDisplayName(descriptor));
            } else {
                demName.setSelectedItem(demNameParam);
            }
        }
        demResamplingMethod.setSelectedItem(paramMap.get("demResamplingMethod"));

        final File extFile = (File) paramMap.get("externalDEMFile");
        if (extFile != null) {
            externalDEMFile.setText(extFile.getAbsolutePath());
            extNoDataValue = (Double) paramMap.get("externalDEMNoDataValue");
            if (extNoDataValue != null && !textAreaKeyListener.isChangedByUser()) {
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        }

        externalDEMApplyEGMCheckBox.setSelected(externalDEMApplyEGM);
        outputSimulatedImageCheckBox.setSelected(outputSimulatedImage);
        outputSigma0CheckBox.setSelected(outputSigma0);
        nodataValueAtSeaCheckBox.setSelected(nodataValueAtSea);

        additionalOverlap.setText(String.valueOf(paramMap.get("additionalOverlap")));
        oversamplingMultiple.setText(String.valueOf(paramMap.get("oversamplingMultiple")));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("demName", (DEMFactory.getProperDEMName((String) demName.getSelectedItem())));
        paramMap.put("demResamplingMethod", demResamplingMethod.getSelectedItem());

        final String extFileStr = externalDEMFile.getText();
        if (!extFileStr.isEmpty()) {
            paramMap.put("externalDEMFile", new File(extFileStr));
            paramMap.put("externalDEMNoDataValue", Double.parseDouble(externalDEMNoDataValue.getText()));
        }

        paramMap.put("externalDEMApplyEGM", externalDEMApplyEGM);
        paramMap.put("outputSimulatedImage", outputSimulatedImage);
        paramMap.put("outputSigma0", outputSigma0);
        paramMap.put("nodataValueAtSea", nodataValueAtSea);

        final String additionalOverlapStr = additionalOverlap.getText();
        if (additionalOverlapStr != null && !additionalOverlapStr.isEmpty()) {
            paramMap.put("additionalOverlap", Double.parseDouble(additionalOverlapStr));
        }
        final String oversamplingMultipleStr = oversamplingMultiple.getText();
        if (oversamplingMultipleStr != null && !oversamplingMultipleStr.isEmpty()) {
            paramMap.put("oversamplingMultiple", Double.parseDouble(oversamplingMultipleStr));
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Digital Elevation Model:", demName);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, externalDEMFileLabel, externalDEMFile);
        gbc.gridx = 2;
        contentPane.add(externalDEMBrowseButton, gbc);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, externalDEMNoDataValueLabel, externalDEMNoDataValue);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "DEM Resampling Method:", demResamplingMethod);
        gbc.gridy++;
        contentPane.add(externalDEMApplyEGMCheckBox, gbc);
        gbc.gridx = 1;
        contentPane.add(outputGamma0CheckBox, gbc);
        outputGamma0CheckBox.setSelected(true);
        outputGamma0CheckBox.setEnabled(false);
        gbc.gridy++;
        gbc.gridx = 0;
        contentPane.add(outputSimulatedImageCheckBox, gbc);
        gbc.gridx = 1;
        contentPane.add(outputSigma0CheckBox, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        contentPane.add(nodataValueAtSeaCheckBox, gbc);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Additional Overlap Percentage[0,1]:", additionalOverlap);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Oversampling Multiple:", oversamplingMultiple);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableExternalDEM(boolean flag) {
        DialogUtils.enableComponents(externalDEMFileLabel, externalDEMFile, flag);
        DialogUtils.enableComponents(externalDEMNoDataValueLabel, externalDEMNoDataValue, flag);
        externalDEMBrowseButton.setVisible(flag);
    }
}
