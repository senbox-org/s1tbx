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
package org.esa.s1tbx.sentinel1.gpf.ui;

import org.esa.snap.dataio.dem.DEMFactory;
import org.esa.snap.framework.dataop.resamp.ResamplingFactory;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;

/**
 * User interface for BackGeocodingOp
 */
public class BackGeocodingOpUI extends BaseOperatorUI {

    private final JComboBox<String> demName = new JComboBox<>(DEMFactory.getDEMNameList());
    private final JComboBox demResamplingMethod = new JComboBox<>(ResamplingFactory.resamplingNames);
    private final JComboBox resamplingType = new JComboBox(ResamplingFactory.resamplingNames);
    final JCheckBox maskOutAreaWithoutElevationCheckBox = new JCheckBox("Mask out areas with no elevation");
    final JCheckBox outputRangeAzimuthOffsetCheckBox = new JCheckBox("Output Range and Azimuth Offset");
    final JCheckBox outputDerampDemodPhaseCheckBox = new JCheckBox("Output Deramp and Demod Phase");

    private final JTextField externalDEMFile = new JTextField("");
    private final JTextField externalDEMNoDataValue = new JTextField("");
    private final JButton externalDEMBrowseButton = new JButton("...");
    private final JLabel externalDEMFileLabel = new JLabel("External DEM:");
    private final JLabel externalDEMNoDataValueLabel = new JLabel("DEM No Data Value:");
    private static final String externalDEMStr = "External DEM";
    private Double extNoDataValue = 0.0;
    private Boolean maskOutAreaWithoutElevation = false;
    private Boolean outputRangeAzimuthOffset = false;
    private Boolean outputDerampDemodPhase = false;

    private final boolean includeOutputRangeAzimuthOffset = false;

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
                final File file = SnapDialogs.requestFileForOpen("External DEM File", false, null, null);
                externalDEMFile.setText(file.getAbsolutePath());
                extNoDataValue = OperatorUIUtils.getNoDataValue(file);
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        });

        externalDEMNoDataValue.addKeyListener(textAreaKeyListener);

        maskOutAreaWithoutElevationCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                maskOutAreaWithoutElevation = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputRangeAzimuthOffsetCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputRangeAzimuthOffset = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputDerampDemodPhaseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputDerampDemodPhase = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        final String demNameParam = (String) paramMap.get("demName");
        if (demNameParam != null)
            demName.setSelectedItem(DEMFactory.appendAutoDEM(demNameParam));
        demResamplingMethod.setSelectedItem(paramMap.get("demResamplingMethod"));

        final File extFile = (File) paramMap.get("externalDEMFile");
        if (extFile != null) {
            externalDEMFile.setText(extFile.getAbsolutePath());
            extNoDataValue = (Double) paramMap.get("externalDEMNoDataValue");
            if (extNoDataValue != null && !textAreaKeyListener.isChangedByUser()) {
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        }

        resamplingType.setSelectedItem(paramMap.get("resamplingType"));

        maskOutAreaWithoutElevation = (Boolean)paramMap.get("maskOutAreaWithoutElevation");
        outputRangeAzimuthOffset = (Boolean)paramMap.get("outputRangeAzimuthOffset");
        outputDerampDemodPhase = (Boolean)paramMap.get("outputDerampDemodPhase");

        if(maskOutAreaWithoutElevation != null) {
            maskOutAreaWithoutElevationCheckBox.setSelected(maskOutAreaWithoutElevation);
        }
        if(outputRangeAzimuthOffset != null) {
            outputRangeAzimuthOffsetCheckBox.setSelected(outputRangeAzimuthOffset);
        }
        if(outputDerampDemodPhase != null) {
            outputDerampDemodPhaseCheckBox.setSelected(outputDerampDemodPhase);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("demName", DEMFactory.getProperDEMName((String) demName.getSelectedItem()));
        paramMap.put("demResamplingMethod", demResamplingMethod.getSelectedItem());

        final String extFileStr = externalDEMFile.getText();
        if (!extFileStr.isEmpty()) {
            paramMap.put("externalDEMFile", new File(extFileStr));
            paramMap.put("externalDEMNoDataValue", Double.parseDouble(externalDEMNoDataValue.getText()));
        }

        paramMap.put("resamplingType", resamplingType.getSelectedItem());

        paramMap.put("maskOutAreaWithoutElevation", maskOutAreaWithoutElevation);
        paramMap.put("outputRangeAzimuthOffset", outputRangeAzimuthOffset);
        paramMap.put("outputDerampDemodPhase", outputDerampDemodPhase);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

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
        DialogUtils.addComponent(contentPane, gbc, "Resampling Type:", resamplingType);
        gbc.gridy++;
        contentPane.add(maskOutAreaWithoutElevationCheckBox, gbc);
        if(includeOutputRangeAzimuthOffset) {
            gbc.gridy++;
            contentPane.add(outputRangeAzimuthOffsetCheckBox, gbc);
        }
        gbc.gridy++;
        contentPane.add(outputDerampDemodPhaseCheckBox, gbc);
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
