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
package org.esa.s1tbx.insar.gpf.ui.coregistration;

import org.esa.snap.engine_utilities.gpf.InputProductValidator;
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
 * User interface for GCPSelectionOp
 */
public class CrossCorrelationOpUI extends BaseOperatorUI {

    private final JComboBox coarseRegistrationWindowWidth = new JComboBox(
            new String[]{"32", "64", "128", "256", "512", "1024", "2048"});
    private final JComboBox coarseRegistrationWindowHeight = new JComboBox(
            new String[]{"32", "64", "128", "256", "512", "1024", "2048"});
    private final JComboBox rowInterpFactor = new JComboBox(
            new String[]{"2", "4", "8", "16"});
    private final JComboBox columnInterpFactor = new JComboBox(
            new String[]{"2", "4", "8", "16"});

    private final JTextField numGCPtoGenerate = new JTextField("");
    private final JTextField maxIteration = new JTextField("");
    private final JTextField gcpTolerance = new JTextField("");

    // for complex products
    final JCheckBox applyFineRegistrationCheckBox = new JCheckBox("Apply Fine Registration");
    final JCheckBox inSAROptimizedCheckBox = new JCheckBox("Optimize for InSAR");
    private final JComboBox fineRegistrationWindowWidth = new JComboBox(
            new String[]{"8", "16", "32", "64", "128", "256", "512"});
    private final JComboBox fineRegistrationWindowHeight = new JComboBox(
            new String[]{"8", "16", "32", "64", "128", "256", "512"});
    private final JComboBox fineRegistrationWindowAccAzimuth = new JComboBox(new String[]{"2", "4", "8", "16", "64"});
    private final JComboBox fineRegistrationWindowAccRange = new JComboBox(new String[]{"2", "4", "8", "16", "64"});
    private final JComboBox fineRegistrationOversampling = new JComboBox(new String[]{"2", "4", "8", "16", "32", "64"});

    private final JTextField coherenceWindowSize = new JTextField("");
    private final JTextField coherenceThreshold = new JTextField("");

    private final JCheckBox useSlidingWindowCheckBox = new JCheckBox("Compute Coherence with Sliding Window");
    private boolean useSlidingWindow = false;

    private boolean isComplex = false;
    private boolean applyFineRegistration = true;
    private boolean inSAROptimized = true;

    final JCheckBox computeOffsetCheckBox = new JCheckBox("Estimate Initial Coarse Offset");
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
        inSAROptimizedCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                inSAROptimized = (e.getStateChange() == ItemEvent.SELECTED);
                enableComplexFields();
            }
        });

        useSlidingWindowCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                useSlidingWindow = (e.getStateChange() == ItemEvent.SELECTED);;
                coherenceWindowSize.setEditable(useSlidingWindow);
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

        if (sourceProducts != null && sourceProducts.length > 0) {
            final InputProductValidator validator = new InputProductValidator(sourceProducts[0]);
            isComplex = validator.isComplex();
        }

        if (isComplex) {
            applyFineRegistration = (Boolean) paramMap.get("applyFineRegistration");
            applyFineRegistrationCheckBox.setSelected(applyFineRegistration);
            inSAROptimizedCheckBox.setSelected(inSAROptimized);

            fineRegistrationWindowWidth.setSelectedItem(paramMap.get("fineRegistrationWindowWidth"));
            fineRegistrationWindowHeight.setSelectedItem(paramMap.get("fineRegistrationWindowHeight"));
            fineRegistrationWindowAccAzimuth.setSelectedItem(paramMap.get("fineRegistrationWindowAccAzimuth"));
            fineRegistrationWindowAccRange.setSelectedItem(paramMap.get("fineRegistrationWindowAccRange"));
            fineRegistrationOversampling.setSelectedItem(paramMap.get("fineRegistrationOversampling"));

            Boolean useSlidingWindowVal = (Boolean) paramMap.get("useSlidingWindow");
            if (useSlidingWindowVal != null) {
                useSlidingWindow = useSlidingWindowVal;
            }
            useSlidingWindowCheckBox.setSelected(useSlidingWindow);
            coherenceWindowSize.setText(String.valueOf(paramMap.get("coherenceWindowSize")));
            coherenceThreshold.setText(String.valueOf(paramMap.get("coherenceThreshold")));
        }
        enableComplexFields();

        Boolean offsetValue = (Boolean) paramMap.get("computeOffset");
        if (offsetValue != null) {
            computeOffset = offsetValue;
        }
        computeOffsetCheckBox.setSelected(computeOffset);

        Boolean gcpOnLandValue = (Boolean) paramMap.get("onlyGCPsOnLand");
        if (gcpOnLandValue != null) {
            onlyGCPsOnLand = gcpOnLandValue;
        }
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

        if (isComplex) {
            paramMap.put("applyFineRegistration", applyFineRegistration);

            if (applyFineRegistration) {
                paramMap.put("inSAROptimized", inSAROptimized);
                paramMap.put("fineRegistrationWindowWidth", fineRegistrationWindowWidth.getSelectedItem());
                paramMap.put("fineRegistrationWindowHeight", fineRegistrationWindowHeight.getSelectedItem());
                paramMap.put("fineRegistrationWindowAccAzimuth", fineRegistrationWindowAccAzimuth.getSelectedItem());
                paramMap.put("fineRegistrationWindowAccRange", fineRegistrationWindowAccRange.getSelectedItem());
                paramMap.put("fineRegistrationOversampling", fineRegistrationOversampling.getSelectedItem());

                paramMap.put("coherenceThreshold", Double.parseDouble(coherenceThreshold.getText()));
                paramMap.put("useSlidingWindow", useSlidingWindow);
                paramMap.put("coherenceWindowSize", Integer.parseInt(coherenceWindowSize.getText()));
            }
        }

        paramMap.put("computeOffset", computeOffset);
        paramMap.put("onlyGCPsOnLand", onlyGCPsOnLand);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Number of GCPs:", numGCPtoGenerate);
        gbc.gridy++;

        contentPane.add(onlyGCPsOnLandCheckBox, gbc);

        gbc.gridx = 1;
        contentPane.add(applyFineRegistrationCheckBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(new JLabel(" "), gbc);
        gbc.gridy++;

        final JPanel coarsePanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc2 = DialogUtils.createGridBagConstraints();
        coarsePanel.setBorder(BorderFactory.createTitledBorder("Coarse Registration"));

        coarsePanel.add(computeOffsetCheckBox, gbc2);
        gbc2.gridy++;

        DialogUtils.addComponent(coarsePanel, gbc2, "Coarse Window Width:", coarseRegistrationWindowWidth);
        gbc2.gridy++;
        DialogUtils.addComponent(coarsePanel, gbc2, "Coarse Window Height:", coarseRegistrationWindowHeight);
        gbc2.gridy++;
        DialogUtils.addComponent(coarsePanel, gbc2, "Row Interpolation Factor:", rowInterpFactor);
        gbc2.gridy++;
        DialogUtils.addComponent(coarsePanel, gbc2, "Column Interpolation Factor:", columnInterpFactor);
        gbc2.gridy++;
        DialogUtils.addComponent(coarsePanel, gbc2, "Max Iterations:", maxIteration);
        gbc2.gridy++;
        DialogUtils.addComponent(coarsePanel, gbc2, "GCP Tolerance:", gcpTolerance);

        final JPanel finePanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc3 = DialogUtils.createGridBagConstraints();
        finePanel.setBorder(BorderFactory.createTitledBorder("Fine Registration"));

        DialogUtils.addComponent(finePanel, gbc3, "Fine Window Width:", fineRegistrationWindowWidth);
        gbc3.gridy++;
        DialogUtils.addComponent(finePanel, gbc3, "Fine Window Height:", fineRegistrationWindowHeight);
        gbc3.gridy++;

        finePanel.add(useSlidingWindowCheckBox, gbc3);
        gbc3.gridy++;
        DialogUtils.addComponent(finePanel, gbc3, "Coherence Window Size:", coherenceWindowSize);
        gbc3.gridy++;
        DialogUtils.addComponent(finePanel, gbc3, "Coherence Threshold:", coherenceThreshold);
        gbc3.gridy++;

        finePanel.add(inSAROptimizedCheckBox, gbc3);
        gbc3.gridy++;

        DialogUtils.addComponent(finePanel, gbc3, "Fine Accuracy in Azimuth:", fineRegistrationWindowAccAzimuth);
        gbc3.gridy++;
        DialogUtils.addComponent(finePanel, gbc3, "Fine Accuracy in Range:", fineRegistrationWindowAccRange);
        gbc3.gridy++;
        DialogUtils.addComponent(finePanel, gbc3, "Fine Window oversampling factor:", fineRegistrationOversampling);
        gbc3.gridy++;

        enableComplexFields();

        contentPane.add(coarsePanel, gbc);
        gbc.gridx = 1;
        contentPane.add(finePanel, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableComplexFields() {
        applyFineRegistrationCheckBox.setEnabled(isComplex);
        inSAROptimizedCheckBox.setEnabled(isComplex && applyFineRegistration);

        fineRegistrationWindowWidth.setEnabled(isComplex && applyFineRegistration);
        fineRegistrationWindowHeight.setEnabled(isComplex && applyFineRegistration);
        fineRegistrationWindowAccAzimuth.setEnabled(isComplex && applyFineRegistration && inSAROptimized);
        fineRegistrationWindowAccRange.setEnabled(isComplex && applyFineRegistration && inSAROptimized);
        fineRegistrationOversampling.setEnabled(isComplex && applyFineRegistration && inSAROptimized);

        coherenceWindowSize.setEnabled(isComplex && applyFineRegistration && useSlidingWindow && !inSAROptimized);
        coherenceThreshold.setEnabled(isComplex && applyFineRegistration);
        useSlidingWindowCheckBox.setEnabled(isComplex && applyFineRegistration && !inSAROptimized);
    }
}
