/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for GCPSelectionOp
 */
public class GCPSelectionOpUI extends BaseOperatorUI {

    private final JComboBox coarseRegistrationWindowWidth = new JComboBox(
            new String[] { "32","64","128","256","512","1024","2048" } );
    private final JComboBox coarseRegistrationWindowHeight = new JComboBox(
            new String[] { "32","64","128","256","512","1024","2048" } );
    private final JComboBox rowInterpFactor = new JComboBox(
            new String[] { "2","4","8","16" } );
    private final JComboBox columnInterpFactor = new JComboBox(
            new String[] { "2","4","8","16" } );

    private final JTextField numGCPtoGenerate = new JTextField("");
    private final JTextField maxIteration = new JTextField("");
    private final JTextField gcpTolerance = new JTextField("");

    // for complex products
    final JCheckBox applyFineRegistrationCheckBox = new JCheckBox("Apply Fine Registration");
    private final JComboBox fineRegistrationWindowWidth = new JComboBox(
            new String[] { "8","16","32","64","128","256","512" } );
    private final JComboBox fineRegistrationWindowHeight = new JComboBox(
            new String[] { "8","16","32","64","128","256","512" } );

    private final JTextField coherenceWindowSize = new JTextField("");
    private final JTextField coherenceThreshold = new JTextField("");

    private final JRadioButton useSlidingWindow = new JRadioButton("Compute Coherence with Sliding Window");

    private boolean isComplex = false;
    private boolean applyFineRegistration = true;

    final JCheckBox computeOffsetCheckBox = new JCheckBox("Estimate Coarse Offset");
    private boolean computeOffset = false;
    final JCheckBox onlyGCPsOnLandCheckBox = new JCheckBox("Test GCPs are on land");
    private boolean onlyGCPsOnLand = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        applyFineRegistrationCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    applyFineRegistration = (e.getStateChange() == ItemEvent.SELECTED);
                    enableComplexFields();
                }
        });

        computeOffsetCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    computeOffset = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });
        onlyGCPsOnLandCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                onlyGCPsOnLand = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        numGCPtoGenerate.setText(String.valueOf(paramMap.get("numGCPtoGenerate")));
        coarseRegistrationWindowWidth.setSelectedItem(paramMap.get("coarseRegistrationWindowWidth"));
        coarseRegistrationWindowHeight.setSelectedItem(paramMap.get("coarseRegistrationWindowHeight"));
        rowInterpFactor.setSelectedItem(paramMap.get("rowInterpFactor"));
        columnInterpFactor.setSelectedItem(paramMap.get("columnInterpFactor"));
        maxIteration.setText(String.valueOf(paramMap.get("maxIteration")));
        gcpTolerance.setText(String.valueOf(paramMap.get("gcpTolerance")));

        if(sourceProducts != null && sourceProducts.length > 0) {
            isComplex = OperatorUtils.isComplex(sourceProducts[0]);
        }

        if(isComplex) {
            applyFineRegistration = (Boolean)paramMap.get("applyFineRegistration");
            applyFineRegistrationCheckBox.getModel().setPressed(applyFineRegistration);
            applyFineRegistrationCheckBox.setSelected(applyFineRegistration);
            
            fineRegistrationWindowWidth.setSelectedItem(paramMap.get("fineRegistrationWindowWidth"));
            fineRegistrationWindowHeight.setSelectedItem(paramMap.get("fineRegistrationWindowHeight"));
            coherenceWindowSize.setText(String.valueOf(paramMap.get("coherenceWindowSize")));
            coherenceThreshold.setText(String.valueOf(paramMap.get("coherenceThreshold")));
        }
        enableComplexFields();

        computeOffset = (Boolean)paramMap.get("computeOffset");
        computeOffsetCheckBox.setSelected(computeOffset);

        onlyGCPsOnLand = (Boolean)paramMap.get("onlyGCPsOnLand");
        onlyGCPsOnLandCheckBox.setSelected(onlyGCPsOnLand);
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("numGCPtoGenerate", Integer.parseInt(numGCPtoGenerate.getText()));
        paramMap.put("coarseRegistrationWindowWidth", coarseRegistrationWindowWidth.getSelectedItem());
        paramMap.put("coarseRegistrationWindowHeight", coarseRegistrationWindowHeight.getSelectedItem());
        paramMap.put("rowInterpFactor", rowInterpFactor.getSelectedItem());
        paramMap.put("columnInterpFactor", columnInterpFactor.getSelectedItem());
        paramMap.put("maxIteration", Integer.parseInt(maxIteration.getText()));
        paramMap.put("gcpTolerance", Double.parseDouble(gcpTolerance.getText()));

        if(isComplex) {
            paramMap.put("applyFineRegistration", applyFineRegistration);

            if (applyFineRegistration) {
                paramMap.put("fineRegistrationWindowWidth", fineRegistrationWindowWidth.getSelectedItem());
                paramMap.put("fineRegistrationWindowHeight", fineRegistrationWindowHeight.getSelectedItem());
                paramMap.put("coherenceThreshold", Double.parseDouble(coherenceThreshold.getText()));
                if(useSlidingWindow.isSelected()) {
                    paramMap.put("useSlidingWindow", true);
                    paramMap.put("coherenceWindowSize", Integer.parseInt(coherenceWindowSize.getText()));
                } else {
                    paramMap.put("useSlidingWindow", false);
                }
            }
        }

        paramMap.put("computeOffset", computeOffset);
        paramMap.put("onlyGCPsOnLand", onlyGCPsOnLand);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Number of GCPs:", numGCPtoGenerate);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coarse Registration Window Width:", coarseRegistrationWindowWidth);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coarse Registration Window Height:", coarseRegistrationWindowHeight);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Row Interpolation Factor:", rowInterpFactor);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Column Interpolation Factor:", columnInterpFactor);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Max Iterations:", maxIteration);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "GCP Tolerance:", gcpTolerance);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(applyFineRegistrationCheckBox, gbc);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coherence Window Size:", coherenceWindowSize);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Coherence Threshold:", coherenceThreshold);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Registration Window Width:", fineRegistrationWindowWidth);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Fine Registration Window Height:", fineRegistrationWindowHeight);
        gbc.gridy++;

        gbc.gridx = 0;
        contentPane.add(useSlidingWindow, gbc);

        enableComplexFields();

        useSlidingWindow.setSelected(true);
        useSlidingWindow.setActionCommand("Use Sliding Window:");
        RadioListener myListener = new RadioListener();
        useSlidingWindow.addActionListener(myListener);

        gbc.gridy++;
        contentPane.add(computeOffsetCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(onlyGCPsOnLandCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableComplexFields() {
        applyFineRegistrationCheckBox.setEnabled(isComplex);
        fineRegistrationWindowWidth.setEnabled(isComplex&&applyFineRegistration);
        fineRegistrationWindowHeight.setEnabled(isComplex&&applyFineRegistration);
        coherenceWindowSize.setEnabled(isComplex&&applyFineRegistration);
        coherenceThreshold.setEnabled(isComplex&&applyFineRegistration);
        useSlidingWindow.setEnabled(isComplex&&applyFineRegistration);
    }


    private class RadioListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            if (useSlidingWindow.isSelected()) {
                coherenceWindowSize.setEditable(true);
            } else {
                //coherenceWindowSize.setText("");
                coherenceWindowSize.setEditable(false);
            }
        }
    }

}