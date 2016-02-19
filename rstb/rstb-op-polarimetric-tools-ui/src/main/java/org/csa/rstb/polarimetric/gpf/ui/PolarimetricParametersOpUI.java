/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.polarimetric.gpf.ui;

import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for PolarimetricParametersOp
 */
public class PolarimetricParametersOpUI extends BaseOperatorUI {

    private final JComboBox windowSizeXStr = new JComboBox(new String[]{"3", "5", "7", "9", "11", "13", "15", "17", "19"});
    private final JComboBox windowSizeYStr = new JComboBox(new String[]{"3", "5", "7", "9", "11", "13", "15", "17", "19"});

    final JCheckBox useMeanMatrixCheckBox = new JCheckBox("Use Mean Matrix");
    final JCheckBox outputSpanCheckBox = new JCheckBox("Span");
    final JCheckBox outputPedestalHeightCheckBox = new JCheckBox("Pedestal Height");
    final JCheckBox outputRVICheckBox = new JCheckBox("Radar Vegetation Index");
    final JCheckBox outputRFDICheckBox = new JCheckBox("Radar Forest Degradation Index");
    final JCheckBox outputHHHVRatioCheckBox = new JCheckBox("HH/HV Ratio");

    private Boolean useMeanMatrix = false;
    private Boolean outputSpan = false;
    private Boolean outputPedestalHeight = false;
    private Boolean outputRVI = false;
    private Boolean outputRFDI = false;
    private Boolean outputHHHVRatio = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = new JScrollPane(createPanel());
        initParameters();

        useMeanMatrixCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {

                useMeanMatrix = (e.getStateChange() == ItemEvent.SELECTED);
                if (useMeanMatrix) {
                    windowSizeXStr.setEnabled(true);
                    windowSizeYStr.setEnabled(true);
                } else {
                    windowSizeXStr.setEnabled(false);
                    windowSizeYStr.setEnabled(false);
                }
            }
        });

        outputSpanCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputSpan = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputPedestalHeightCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputPedestalHeight = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputRVICheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputRVI = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputRFDICheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputRFDI = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        outputHHHVRatioCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputHHHVRatio = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        windowSizeXStr.setSelectedItem(paramMap.get("windowSizeXStr"));
        windowSizeYStr.setSelectedItem(paramMap.get("windowSizeYStr"));

        Boolean paramVal;
        paramVal = (Boolean) paramMap.get("useMeanMatrix");
        if (paramVal != null) {
            useMeanMatrix = paramVal;
            useMeanMatrixCheckBox.setSelected(useMeanMatrix);
        }

        paramVal = (Boolean) paramMap.get("outputSpan");
        if (paramVal != null) {
            outputSpan = paramVal;
            outputSpanCheckBox.setSelected(outputSpan);
        }

        paramVal = (Boolean) paramMap.get("outputPedestalHeight");
        if (paramVal != null) {
            outputPedestalHeight = paramVal;
            outputPedestalHeightCheckBox.setSelected(outputPedestalHeight);
        }

        paramVal = (Boolean) paramMap.get("outputRVI");
        if (paramVal != null) {
            outputRVI = paramVal;
            outputRVICheckBox.setSelected(outputRVI);
        }

        paramVal = (Boolean) paramMap.get("outputRFDI");
        if (paramVal != null) {
            outputRFDI = paramVal;
            outputRFDICheckBox.setSelected(outputRFDI);
        }

        paramVal = (Boolean) paramMap.get("outputHHHVRatio");
        if (paramVal != null) {
            outputHHHVRatio = paramVal;
            outputHHHVRatioCheckBox.setSelected(outputHHHVRatio);
        }
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        if (useMeanMatrix) {
            paramMap.put("windowSizeXStr", windowSizeXStr.getSelectedItem());
            paramMap.put("windowSizeYStr", windowSizeYStr.getSelectedItem());
        }
        paramMap.put("useMeanMatrix", useMeanMatrix);
        paramMap.put("outputSpan", outputSpan);
        paramMap.put("outputPedestalHeight", outputPedestalHeight);
        paramMap.put("outputRVI", outputRVI);
        paramMap.put("outputRFDI", outputRFDI);
        paramMap.put("outputHHHVRatio", outputHHHVRatio);
    }

    JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(useMeanMatrixCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Window Size X:", windowSizeXStr);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Window Size Y:", windowSizeYStr);

        gbc.gridy++;
        contentPane.add(outputSpanCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(outputPedestalHeightCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(outputRVICheckBox, gbc);

        gbc.gridy++;
        contentPane.add(outputRFDICheckBox, gbc);

        gbc.gridy++;
        contentPane.add(outputHHHVRatioCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}
