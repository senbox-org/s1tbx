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

    private final JComboBox<String> windowSizeXStr =
            new JComboBox(new String[]{"3", "5", "7", "9", "11", "13", "15", "17", "19"});
    private final JComboBox<String> windowSizeYStr =
            new JComboBox(new String[]{"3", "5", "7", "9", "11", "13", "15", "17", "19"});

    private final CheckBox useMeanMatrixCheckBox = new CheckBox("Use Mean Matrix");
    private final CheckBox outputSpanCheckBox = new CheckBox("Span");
    private final CheckBox outputPedestalHeightCheckBox = new CheckBox("Pedestal Height");
    private final CheckBox outputRVICheckBox = new CheckBox("Radar Vegetation Index (RVI)");
    private final CheckBox outputRFDICheckBox = new CheckBox("Radar Forest Degradation Index (RFDI)");
    private final CheckBox outputCSICheckBox = new CheckBox("Canopy Structure Index (CSI)");
    private final CheckBox outputBMICheckBox = new CheckBox("Biomass Index (BMI)");
    private final CheckBox outputVSICheckBox = new CheckBox("Volume Scattering Index (VSI)");
    private final CheckBox outputITICheckBox = new CheckBox("Interaction Index (ITI) HH VV phase difference");
    private final CheckBox outputHHVVRatioCheckBox = new CheckBox("Co-Pol HH/VV Ratio");
    private final CheckBox outputHHHVRatioCheckBox = new CheckBox("Cross-Pol HH/HV Ratio");
    private final CheckBox outputVVVHRatioCheckBox = new CheckBox("Cross-Pol VV/VH Ratio");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = new JScrollPane(createPanel());
        initParameters();

        useMeanMatrixCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean useMeanMatrix = (e.getStateChange() == ItemEvent.SELECTED);
                windowSizeXStr.setEnabled(useMeanMatrix);
                windowSizeYStr.setEnabled(useMeanMatrix);
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        windowSizeXStr.setSelectedItem(paramMap.get("windowSizeXStr"));
        windowSizeYStr.setSelectedItem(paramMap.get("windowSizeYStr"));

        useMeanMatrixCheckBox.set(paramMap.get("useMeanMatrix"));

        outputSpanCheckBox.set(paramMap.get("outputSpan"));
        outputPedestalHeightCheckBox.set(paramMap.get("outputPedestalHeight"));
        outputRVICheckBox.set(paramMap.get("outputRVI"));
        outputRFDICheckBox.set(paramMap.get("outputRFDI"));
        outputCSICheckBox.set(paramMap.get("outputCSI"));
        outputVSICheckBox.set(paramMap.get("outputVSI"));
        outputBMICheckBox.set(paramMap.get("outputBMI"));
        outputITICheckBox.set(paramMap.get("outputITI"));
        outputHHVVRatioCheckBox.set(paramMap.get("outputHHVVRatio"));
        outputHHHVRatioCheckBox.set(paramMap.get("outputHHHVRatio"));
        outputVVVHRatioCheckBox.set(paramMap.get("outputVVVHRatio"));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        if (useMeanMatrixCheckBox.getValue()) {
            paramMap.put("windowSizeXStr", windowSizeXStr.getSelectedItem());
            paramMap.put("windowSizeYStr", windowSizeYStr.getSelectedItem());
        }
        paramMap.put("useMeanMatrix", useMeanMatrixCheckBox.getValue());
        paramMap.put("outputSpan", outputSpanCheckBox.getValue());
        paramMap.put("outputPedestalHeight", outputPedestalHeightCheckBox.getValue());
        paramMap.put("outputRVI", outputRVICheckBox.getValue());
        paramMap.put("outputRFDI", outputRFDICheckBox.getValue());
        paramMap.put("outputCSI", outputCSICheckBox.getValue());
        paramMap.put("outputVSI", outputVSICheckBox.getValue());
        paramMap.put("outputBMI", outputBMICheckBox.getValue());
        paramMap.put("outputITI", outputITICheckBox.getValue());
        paramMap.put("outputHHVVRatio", outputHHVVRatioCheckBox.getValue());
        paramMap.put("outputHHHVRatio", outputHHHVRatioCheckBox.getValue());
        paramMap.put("outputVVVHRatio", outputVVVHRatioCheckBox.getValue());
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
        contentPane.add(outputCSICheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputVSICheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputBMICheckBox, gbc);
        //gbc.gridy++;
        //contentPane.add(outputITICheckBox, gbc);

        gbc.gridy++;
        contentPane.add(outputHHVVRatioCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputHHHVRatioCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputVVVHRatioCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private static class CheckBox extends JCheckBox {
        private Boolean value;

        public CheckBox(String text) {
            super(text);

            this.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    value = (e.getStateChange() == ItemEvent.SELECTED);
                }
            });
        }

        public void set(Object o) {
            Boolean paramVal = (Boolean) o;
            if (paramVal != null) {
                value = paramVal;
                setSelected(value);
            }
        }

        public Boolean getValue() {
            return value;
        }
    }
}
