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
package org.esa.nest.gpf;

import org.esa.snap.gpf.ui.BaseOperatorUI;
import org.esa.snap.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.snap.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * User interface for ConstantOffsetEstimationOp
 */
public class ConstantOffsetEstimationOpUI extends BaseOperatorUI {

    private final JComboBox registrationWindowSize = new JComboBox(
            new String[]{"512", "1024", "2048"});
    private final JComboBox interpFactor = new JComboBox(
            new String[]{"2", "4", "8", "16"});

    private final JTextField maxIteration = new JTextField("");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        registrationWindowSize.setSelectedItem(paramMap.get("registrationWindowSize"));
        interpFactor.setSelectedItem(paramMap.get("interpFactor"));
        maxIteration.setText(String.valueOf(paramMap.get("maxIteration")));
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("registrationWindowSize", registrationWindowSize.getSelectedItem());
        paramMap.put("interpFactor", interpFactor.getSelectedItem());
        paramMap.put("maxIteration", Integer.parseInt(maxIteration.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Registration Window Size:", registrationWindowSize);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Interpolation Factor:", interpFactor);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Max Iterations:", maxIteration);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}