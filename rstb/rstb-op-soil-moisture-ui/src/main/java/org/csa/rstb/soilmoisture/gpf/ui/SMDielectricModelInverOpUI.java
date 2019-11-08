/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.gpf.ui;

import org.csa.rstb.soilmoisture.gpf.DielectricModelFactory;
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
 * User interface for IEM operators
 */
public class SMDielectricModelInverOpUI extends BaseOperatorUI {

    private final JComboBox modelToUseComboBox = new JComboBox<>(
            new String[]{DielectricModelFactory.HALLIKAINEN, DielectricModelFactory.MIRONOV});

    private final JTextField minSM = new JTextField();
    private final JTextField maxSM = new JTextField();

    private final JCheckBox outputRDCCheckBox = new JCheckBox("Output RDC");
    private final JCheckBox outputLandCoverCheckBox = new JCheckBox("Output Land Cover");

    private final JLabel effectiveSoilTemperatureLabel = new JLabel("Effective Soil Temp.(° C):");
    private final JTextField effectiveSoilTemperature = new JTextField();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        modelToUseComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                showMironovParams(modelToUseComboBox.getSelectedItem().equals(DielectricModelFactory.MIRONOV));
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        final String modelToUse = (String) paramMap.get("modelToUse");
        if (modelToUse != null) {
            modelToUseComboBox.setSelectedItem(modelToUse);
            effectiveSoilTemperature.setText(String.valueOf(paramMap.get("effectiveSoilTemperature")));
            showMironovParams(modelToUseComboBox.getSelectedItem().equals(DielectricModelFactory.MIRONOV));
        }

        Double minSMVal = (Double) paramMap.get("minSM");
        if (minSMVal == null)
            minSMVal = 0d;
        Double maxSMVal = (Double) paramMap.get("maxSM");
        minSM.setText(String.valueOf(minSMVal));
        maxSM.setText(String.valueOf(maxSMVal));

        final Boolean outputRDC = ((Boolean) paramMap.get("outputRDC"));
        if (outputRDC != null) {
            outputRDCCheckBox.setSelected(outputRDC);
        }
        final Boolean outputLandCover = ((Boolean) paramMap.get("outputLandCover"));
        if (outputLandCover != null) {
            outputLandCoverCheckBox.setSelected(outputLandCover);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("modelToUse", modelToUseComboBox.getSelectedItem());
        paramMap.put("minSM", Double.parseDouble(minSM.getText()));
        paramMap.put("maxSM", Double.parseDouble(maxSM.getText()));
        paramMap.put("outputRDC", outputRDCCheckBox.isSelected());
        paramMap.put("outputLandCover", outputLandCoverCheckBox.isSelected());

        paramMap.put("effectiveSoilTemperature", Double.parseDouble(effectiveSoilTemperature.getText()));
    }

    private void showMironovParams(final boolean flag) {
        effectiveSoilTemperature.setVisible(flag);
        DialogUtils.enableComponents(effectiveSoilTemperatureLabel, effectiveSoilTemperature, flag);
    }

    private JComponent createPanel() {

        final JPanel content = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        DialogUtils.addComponent(content, gbc, "Dielectric model:            ", modelToUseComboBox);

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Min SM (m³/m³):", minSM);
        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, "Max SM (m³/m³):", maxSM);

        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, effectiveSoilTemperatureLabel, effectiveSoilTemperature);
        DialogUtils.enableComponents(effectiveSoilTemperatureLabel, effectiveSoilTemperature, false);

        gbc.gridy++;
        gbc.gridx = 1;
        content.add(outputRDCCheckBox, gbc);
        gbc.gridy++;
        gbc.gridx = 1;
        content.add(outputLandCoverCheckBox, gbc);

        DialogUtils.fillPanel(content, gbc);

        return content;
    }
}