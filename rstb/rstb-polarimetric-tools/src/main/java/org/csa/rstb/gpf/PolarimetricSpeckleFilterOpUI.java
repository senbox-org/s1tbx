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

public class PolarimetricSpeckleFilterOpUI extends BaseOperatorUI {

    private final JComboBox filter = new JComboBox(new String[] { PolarimetricSpeckleFilterOp.BOXCAR_SPECKLE_FILTER,
                                                                  PolarimetricSpeckleFilterOp.IDAN_FILTER,
                                                                  PolarimetricSpeckleFilterOp.REFINED_LEE_FILTER,
                                                                  PolarimetricSpeckleFilterOp.LEE_SIGMA_FILTER } );

    private final JComboBox numLooks = new JComboBox(new String[] { PolarimetricSpeckleFilterOp.NUM_LOOKS_1,
                                                                    PolarimetricSpeckleFilterOp.NUM_LOOKS_2,
                                                                    PolarimetricSpeckleFilterOp.NUM_LOOKS_3,
                                                                    PolarimetricSpeckleFilterOp.NUM_LOOKS_4} );

    private final JComboBox windowSize = new JComboBox(new String[] { PolarimetricSpeckleFilterOp.WINDOW_SIZE_5x5,
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_7x7,
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_9x9,
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_11x11} );

    private final JComboBox filterWindowSize = new JComboBox(new String[] {
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_7x7,
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_9x9,
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_11x11 } );

    private final JComboBox targetWindowSize = new JComboBox(new String[] {
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_3x3,
                                                                      PolarimetricSpeckleFilterOp.WINDOW_SIZE_5x5 } );

    private final JComboBox sigmaStr = new JComboBox(new String[] {PolarimetricSpeckleFilterOp.SIGMA_50_PERCENT,
                                                                   PolarimetricSpeckleFilterOp.SIGMA_60_PERCENT,
                                                                   PolarimetricSpeckleFilterOp.SIGMA_70_PERCENT,
                                                                   PolarimetricSpeckleFilterOp.SIGMA_80_PERCENT,
                                                                   PolarimetricSpeckleFilterOp.SIGMA_90_PERCENT } );

    private static final JLabel filterLabel = new JLabel("Speckle Filter:");
    private static final JLabel filterSizeLabel = new JLabel("Filter Size:   ");
    private static final JLabel numLooksLabel = new JLabel("Number of Looks:");
    private static final JLabel windowSizeLabel = new JLabel("Window Size:");
    private static final JLabel filterWindowSizeLabel = new JLabel("Filter Window Size:");
    private static final JLabel targetWindowSizeLabel = new JLabel("Target Window Size:");
    private static final JLabel anSizeLabel = new JLabel("Adaptive Neighbourhood Size:");
    private static final JLabel sigmaStrLabel = new JLabel("Sigma:");

    private final JTextField filterSize = new JTextField("");
    private final JTextField anSize = new JTextField("");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        filter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateFilterSelection();
            }
        });
        updateFilterSelection();

        initParameters();

        return panel;
    }

    @Override
    public void initParameters() {

        filter.setSelectedItem(paramMap.get("filter"));
        filterSize.setText(String.valueOf(paramMap.get("filterSize")));
        numLooks.setSelectedItem(paramMap.get("numLooksStr"));
        windowSize.setSelectedItem(paramMap.get("windowSize"));
        filterWindowSize.setSelectedItem(paramMap.get("filterWindowSizeStr"));
        targetWindowSize.setSelectedItem(paramMap.get("targetWindowSizeStr"));
        sigmaStr.setSelectedItem(paramMap.get("sigmaStr"));
        anSize.setText(String.valueOf(paramMap.get("anSize")));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("filter", filter.getSelectedItem());
        paramMap.put("filterSize", Integer.parseInt(filterSize.getText()));
        paramMap.put("numLooksStr", numLooks.getSelectedItem());
        paramMap.put("windowSize", windowSize.getSelectedItem());
        paramMap.put("filterWindowSizeStr", filterWindowSize.getSelectedItem());
        paramMap.put("targetWindowSizeStr", targetWindowSize.getSelectedItem());
        paramMap.put("sigmaStr", sigmaStr.getSelectedItem());
        paramMap.put("anSize", Integer.parseInt(anSize.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, filterLabel, filter);

        int savedY = ++gbc.gridy;
        DialogUtils.addComponent(contentPane, gbc, filterSizeLabel, filterSize);
        DialogUtils.enableComponents(filterSizeLabel, filterSize, true);

        gbc.gridy = savedY+1;
        DialogUtils.addComponent(contentPane, gbc, numLooksLabel, numLooks);
        DialogUtils.enableComponents(numLooksLabel, numLooks, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, windowSizeLabel, windowSize);
        DialogUtils.enableComponents(windowSizeLabel, windowSize, false);

        DialogUtils.addComponent(contentPane, gbc, anSizeLabel, anSize);
        DialogUtils.enableComponents(anSizeLabel, anSize, false);

        DialogUtils.addComponent(contentPane, gbc, sigmaStrLabel, sigmaStr);
        DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, filterWindowSizeLabel, filterWindowSize);
        DialogUtils.enableComponents(filterWindowSizeLabel, filterWindowSize, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, targetWindowSizeLabel, targetWindowSize);
        DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, false);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void updateFilterSelection() {
        final String item = (String)filter.getSelectedItem();
        if (item.equals(PolarimetricSpeckleFilterOp.REFINED_LEE_FILTER)) {
            DialogUtils.enableComponents(numLooksLabel, numLooks, true);
            DialogUtils.enableComponents(windowSizeLabel, windowSize, true);
            DialogUtils.enableComponents(filterSizeLabel, filterSize, false);
            DialogUtils.enableComponents(anSizeLabel, anSize, false);
            DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, false);
            DialogUtils.enableComponents(filterWindowSizeLabel, filterWindowSize, false);
            DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, false);
        } else if (item.equals(PolarimetricSpeckleFilterOp.IDAN_FILTER)) {
            DialogUtils.enableComponents(numLooksLabel, numLooks, true);
            DialogUtils.enableComponents(anSizeLabel, anSize, true);
            DialogUtils.enableComponents(windowSizeLabel, windowSize, false);
            DialogUtils.enableComponents(filterSizeLabel, filterSize, false);
            DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, false);
            DialogUtils.enableComponents(filterWindowSizeLabel, filterWindowSize, false);
            DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, false);
        } else if (item.equals(PolarimetricSpeckleFilterOp.LEE_SIGMA_FILTER)) {
            DialogUtils.enableComponents(numLooksLabel, numLooks, true);
            DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, true);
            DialogUtils.enableComponents(filterWindowSizeLabel, filterWindowSize, true);
            DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, true);
            DialogUtils.enableComponents(anSizeLabel, anSize, false);
            DialogUtils.enableComponents(windowSizeLabel, windowSize, false);
            DialogUtils.enableComponents(filterSizeLabel, filterSize, false);
        } else { // boxcar
            DialogUtils.enableComponents(numLooksLabel, numLooks, false);
            DialogUtils.enableComponents(windowSizeLabel, windowSize, false);
            DialogUtils.enableComponents(filterSizeLabel, filterSize, true);
            DialogUtils.enableComponents(anSizeLabel, anSize, false);
            DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, false);
            DialogUtils.enableComponents(filterWindowSizeLabel, filterWindowSize, false);
            DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, false);
        }
    }

}