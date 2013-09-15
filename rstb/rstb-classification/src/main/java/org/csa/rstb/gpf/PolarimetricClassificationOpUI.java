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
package org.csa.rstb.gpf;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

public class PolarimetricClassificationOpUI extends BaseOperatorUI {

    private final JComboBox classification = new JComboBox(new String[] {
                                                          PolarimetricClassificationOp.UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION,
                                                          PolarimetricClassificationOp.UNSUPERVISED_WISHART_CLASSIFICATION,
                                                          PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION,
    } );

    private final JLabel windowSizeLabel = new JLabel("Window Size:");
    private final JTextField windowSize = new JTextField("");
    private final JLabel maxIterationsLabel = new JLabel("Max Iterations:");
    private final JTextField maxIterations = new JTextField("");
    private final JLabel numInitialClassesLabel = new JLabel("Initial Number of Classes:");
    private final JTextField numInitialClasses = new JTextField("");
    private final JLabel numFinalClassesLabel = new JLabel("Final Number of Classes:");
    private final JTextField numFinalClasses = new JTextField("");
    private final JLabel mixedCategoryThresholdLabel = new JLabel("Threshold for Mixed Category:");
    private final JTextField mixedCategoryThreshold = new JTextField("");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return panel;
    }

    @Override
    public void initParameters() {

        classification.setSelectedItem(paramMap.get("classification"));
        windowSize.setText(String.valueOf(paramMap.get("windowSize")));
		maxIterations.setText(String.valueOf(paramMap.get("maxIterations")));
        numInitialClasses.setText(String.valueOf(paramMap.get("numInitialClasses")));
        numFinalClasses.setText(String.valueOf(paramMap.get("numFinalClasses")));
        mixedCategoryThreshold.setText(String.valueOf(paramMap.get("mixedCategoryThreshold")));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("classification", classification.getSelectedItem());
        paramMap.put("windowSize", Integer.parseInt(windowSize.getText()));
        paramMap.put("maxIterations", Integer.parseInt(maxIterations.getText()));
        paramMap.put("numInitialClasses", Integer.parseInt(numInitialClasses.getText()));
        paramMap.put("numFinalClasses", Integer.parseInt(numFinalClasses.getText()));
        paramMap.put("mixedCategoryThreshold", Double.parseDouble(mixedCategoryThreshold.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        
        gbc.gridx = 0;
        contentPane.add(new JLabel("Classification:"), gbc);
        gbc.gridx = 1;
        contentPane.add(classification, gbc);

        classification.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
				String item = (String)classification.getSelectedItem();
                if (item.equals(PolarimetricClassificationOp.UNSUPERVISED_WISHART_CLASSIFICATION)) {
                    DialogUtils.enableComponents(maxIterationsLabel, maxIterations, true);
                } else {
                    DialogUtils.enableComponents(maxIterationsLabel, maxIterations, false);
                }

                if (item.equals(PolarimetricClassificationOp.UNSUPERVISED_TERRAIN_CLASSIFICATION)) {
                    DialogUtils.enableComponents(numInitialClassesLabel, numInitialClasses, true);
                    DialogUtils.enableComponents(numFinalClassesLabel, numFinalClasses, true);
                    DialogUtils.enableComponents(mixedCategoryThresholdLabel, mixedCategoryThreshold, true);
                } else {
                    DialogUtils.enableComponents(numInitialClassesLabel, numInitialClasses, false);
                    DialogUtils.enableComponents(numFinalClassesLabel, numFinalClasses, false);
                    DialogUtils.enableComponents(mixedCategoryThresholdLabel, mixedCategoryThreshold, false);
                }
            }
        });

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, windowSizeLabel, windowSize);
        DialogUtils.enableComponents(windowSizeLabel, windowSize, true);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, maxIterationsLabel, maxIterations);
        DialogUtils.enableComponents(maxIterationsLabel, maxIterations, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, numInitialClassesLabel, numInitialClasses);
        DialogUtils.enableComponents(numInitialClassesLabel, numInitialClasses, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, numFinalClassesLabel, numFinalClasses);
        DialogUtils.enableComponents(numFinalClassesLabel, numFinalClasses, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, mixedCategoryThresholdLabel, mixedCategoryThreshold);
        DialogUtils.enableComponents(mixedCategoryThresholdLabel, mixedCategoryThreshold, false);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}