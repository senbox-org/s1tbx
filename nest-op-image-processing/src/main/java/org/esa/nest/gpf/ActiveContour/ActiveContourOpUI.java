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
package org.esa.nest.gpf.ActiveContour;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.gpf.OperatorUIUtils;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ActiveContourOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JLabel stepLabel = new JLabel("Step");
    private final JTextField step = new JTextField("");
    // threshold of edges
    private final JLabel gradientThresholdLabel = new JLabel("Gradient Threshold");
    private final JTextField gradientThreshold = new JTextField("");
    // how far to look for edges
    private final JLabel maxDistanceLabel = new JLabel("Maximum Distance");
    private final JTextField maxDistance = new JTextField("");
    // maximum displacement
    private final JLabel maxDisplacementLabel = new JLabel("Maximum Displacement");
    private final JTextField maxDisplacement = new JTextField("");
    // regularization factors, min and max
    private final JLabel dRegularizationLabel = new JLabel("Regularization Factor");
    private final JTextField dRegularization = new JTextField("");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap,
            AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return panel;
    }

    @Override
    public void initParameters() {
        OperatorUIUtils.initParamList(bandList, getBandNames());
        gradientThreshold.setText(String.valueOf(paramMap.get("gradientThreshold")));
        maxDistance.setText(String.valueOf(paramMap.get("maxDistance")));
        maxDisplacement.setText(String.valueOf(paramMap.get("maxDisplacement")));
        step.setText(String.valueOf(paramMap.get("step")));
        dRegularization.setText(String.valueOf(paramMap.get("dRegularization")));
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("GradientThreshold", Integer.parseInt(gradientThreshold.getText()));
        paramMap.put("MaximumDistance", Integer.parseInt(maxDistance.getText()));
        paramMap.put("MaximumDisplacement", Double.parseDouble(maxDisplacement.getText()));
        paramMap.put("Step", Integer.parseInt(step.getText()));
        paramMap.put("RegularizationFactor", Double.parseDouble(dRegularization.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, gradientThresholdLabel, gradientThreshold);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, maxDisplacementLabel, maxDisplacement);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, maxDistanceLabel, maxDistance);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, stepLabel, step);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, dRegularizationLabel, dRegularization);
        gbc.gridy++;
        final int savedY = gbc.gridy;
        gbc.gridy++;
        gbc.gridy = savedY;
        gbc.weightx = 1.0;

        contentPane.add(new JPanel(), gbc);

        DialogUtils.enableComponents(gradientThresholdLabel, gradientThreshold, true);
        DialogUtils.enableComponents(maxDisplacementLabel, maxDisplacement, true);
        DialogUtils.enableComponents(maxDistanceLabel, maxDistance, true);
        DialogUtils.enableComponents(stepLabel, step, true);
        DialogUtils.enableComponents(dRegularizationLabel, dRegularization, true);
        return contentPane;
    }
}
