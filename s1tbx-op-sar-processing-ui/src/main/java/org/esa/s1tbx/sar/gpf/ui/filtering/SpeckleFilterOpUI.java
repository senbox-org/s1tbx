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
package org.esa.s1tbx.sar.gpf.ui.filtering;

import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.snap.engine_utilities.gpf.FilterWindow;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Feb 12, 2008
 * Time: 1:52:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpeckleFilterOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();

    private final JComboBox<String> filter = new JComboBox(new String[]{
            SpeckleFilterOp.NONE,
            SpeckleFilterOp.BOXCAR_SPECKLE_FILTER,
            SpeckleFilterOp.MEDIAN_SPECKLE_FILTER,
            SpeckleFilterOp.FROST_SPECKLE_FILTER,
            SpeckleFilterOp.GAMMA_MAP_SPECKLE_FILTER,
            SpeckleFilterOp.LEE_SPECKLE_FILTER,
            SpeckleFilterOp.LEE_REFINED_FILTER,
            SpeckleFilterOp.LEE_SIGMA_FILTER,
            SpeckleFilterOp.IDAN_FILTER});

    private final JComboBox<String> numLooks = new JComboBox(new String[]{SpeckleFilterOp.NUM_LOOKS_1,
            SpeckleFilterOp.NUM_LOOKS_2,
            SpeckleFilterOp.NUM_LOOKS_3,
            SpeckleFilterOp.NUM_LOOKS_4});

    private final JComboBox<String> windowSize = new JComboBox(new String[]{
            FilterWindow.SIZE_5x5, FilterWindow.SIZE_7x7, FilterWindow.SIZE_9x9, FilterWindow.SIZE_11x11,
            FilterWindow.SIZE_13x13, FilterWindow.SIZE_15x15, FilterWindow.SIZE_17x17});

    private final JComboBox<String> targetWindowSize = new JComboBox(new String[]{
            FilterWindow.SIZE_3x3,
            FilterWindow.SIZE_5x5});

    private final JComboBox<String> sigmaStr = new JComboBox(new String[]{
            SpeckleFilterOp.SIGMA_50_PERCENT,
            SpeckleFilterOp.SIGMA_60_PERCENT,
            SpeckleFilterOp.SIGMA_70_PERCENT,
            SpeckleFilterOp.SIGMA_80_PERCENT,
            SpeckleFilterOp.SIGMA_90_PERCENT});

    private static final JLabel dampingFactorLabel = new JLabel("Damping Factor:");
    private static final JLabel filterSizeXLabel = new JLabel("Filter Size X (odd number):   ");
    private static final JLabel filterSizeYLabel = new JLabel("Filter Size Y (odd number):   ");

    private static final JLabel numLooksLabel = new JLabel("Number of Looks:");
    private static final JLabel windowSizeLabel = new JLabel("Window Size:");
    private static final JLabel sigmaStrLabel = new JLabel("Sigma:");
    private static final JLabel targetWindowSizeLabel = new JLabel("Target Window Size:");

    private final JTextField filterSizeX = new JTextField("");
    private final JTextField filterSizeY = new JTextField("");
    private final JTextField dampingFactor = new JTextField("");

    private final JLabel estimateENLCheckBoxLabel = new JLabel("Estimate Equivalent Number of Looks");
    private final JCheckBox estimateENLCheckBox = new JCheckBox("");
    private final JLabel enlLabel = new JLabel("Number of Looks:   ");
    private final JTextField enl = new JTextField("");
    private Boolean estimateENL = true;

    private final JLabel anSizeLabel = new JLabel("Adaptive Neighbour Size");
    private final JTextField anSize = new JTextField("");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        estimateENLCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {

                estimateENL = (e.getStateChange() == ItemEvent.SELECTED);
                if (estimateENL) {
                    enl.setEnabled(false);
                } else {
                    enl.setEnabled(true);
                }
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        final String value = (String)paramMap.get("filter");
        if (value.equals(SpeckleFilterOp.MEAN_SPECKLE_FILTER)) {
            filter.setSelectedItem(SpeckleFilterOp.BOXCAR_SPECKLE_FILTER);
        } else {
            filter.setSelectedItem(paramMap.get("filter"));
        }

        filterSizeX.setText(String.valueOf(paramMap.get("filterSizeX")));
        filterSizeY.setText(String.valueOf(paramMap.get("filterSizeY")));
        dampingFactor.setText(String.valueOf(paramMap.get("dampingFactor")));
        estimateENLCheckBox.setSelected(true);
        estimateENL = true;
        enl.setEnabled(false);
        enl.setText(String.valueOf(paramMap.get("enl")));

        numLooks.setSelectedItem(paramMap.get("numLooksStr"));
        windowSize.setSelectedItem(paramMap.get("windowSize"));
        targetWindowSize.setSelectedItem(paramMap.get("targetWindowSizeStr"));
        sigmaStr.setSelectedItem(paramMap.get("sigmaStr"));

        Integer anSizeStr = (Integer)paramMap.get("anSize");
        anSize.setText(String.valueOf(anSizeStr == null ? 50 : anSizeStr));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("filter", filter.getSelectedItem());
        paramMap.put("filterSizeX", Integer.parseInt(filterSizeX.getText()));
        paramMap.put("filterSizeY", Integer.parseInt(filterSizeY.getText()));
        paramMap.put("dampingFactor", Integer.parseInt(dampingFactor.getText()));
        paramMap.put("estimateENL", estimateENL);
        paramMap.put("enl", Double.parseDouble(enl.getText()));

        paramMap.put("numLooksStr", numLooks.getSelectedItem());
        paramMap.put("windowSize", windowSize.getSelectedItem());
        paramMap.put("targetWindowSizeStr", targetWindowSize.getSelectedItem());
        paramMap.put("sigmaStr", sigmaStr.getSelectedItem());
        paramMap.put("anSize", Integer.parseInt(anSize.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Filter:", filter);

        filter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateFilterSelection();
            }
        });

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, filterSizeXLabel, filterSizeX);
        DialogUtils.addComponent(contentPane, gbc, numLooksLabel, numLooks);
        DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, true);
        DialogUtils.enableComponents(numLooksLabel, numLooks, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, filterSizeYLabel, filterSizeY);
        DialogUtils.addComponent(contentPane, gbc, windowSizeLabel, windowSize);
        DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, true);
        DialogUtils.enableComponents(windowSizeLabel, windowSize, false);
        DialogUtils.addComponent(contentPane, gbc, anSizeLabel, anSize);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, dampingFactorLabel, dampingFactor);
        DialogUtils.addComponent(contentPane, gbc, estimateENLCheckBoxLabel, estimateENLCheckBox);
        DialogUtils.addComponent(contentPane, gbc, sigmaStrLabel, sigmaStr);
        DialogUtils.enableComponents(dampingFactorLabel, dampingFactor, false);
        DialogUtils.enableComponents(estimateENLCheckBoxLabel, estimateENLCheckBox, false);
        DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, false);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, enlLabel, enl);
        DialogUtils.addComponent(contentPane, gbc, targetWindowSizeLabel, targetWindowSize);
        DialogUtils.enableComponents(enlLabel, enl, false);
        DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, false);

        gbc.weightx = 1.0;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void updateFilterSelection() {
        final String item = (String) filter.getSelectedItem();

        DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, false);
        DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, false);
        DialogUtils.enableComponents(dampingFactorLabel, dampingFactor, false);
        DialogUtils.enableComponents(estimateENLCheckBoxLabel, estimateENLCheckBox, false);
        DialogUtils.enableComponents(enlLabel, enl, false);
        DialogUtils.enableComponents(numLooksLabel, numLooks, false);
        DialogUtils.enableComponents(windowSizeLabel, windowSize, false);
        DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, false);
        DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, false);
        DialogUtils.enableComponents(anSizeLabel, anSize, false);

        if (item.equals(SpeckleFilterOp.BOXCAR_SPECKLE_FILTER) || item.equals(SpeckleFilterOp.MEDIAN_SPECKLE_FILTER)) {
            DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, true);
            DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, true);
        }

        if (item.equals(SpeckleFilterOp.FROST_SPECKLE_FILTER)) {
            DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, true);
            DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, true);
            DialogUtils.enableComponents(dampingFactorLabel, dampingFactor, true);
        }

        if (item.equals(SpeckleFilterOp.GAMMA_MAP_SPECKLE_FILTER) ||
                item.equals(SpeckleFilterOp.LEE_SPECKLE_FILTER)) {
            DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, true);
            DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, true);
            DialogUtils.enableComponents(estimateENLCheckBoxLabel, estimateENLCheckBox, true);
            DialogUtils.enableComponents(enlLabel, enl, true);
            estimateENLCheckBox.setSelected(true);
            enl.setEnabled(false);
        }

        if (item.equals(SpeckleFilterOp.LEE_SIGMA_FILTER)) {
            DialogUtils.enableComponents(numLooksLabel, numLooks, true);
            DialogUtils.enableComponents(windowSizeLabel, windowSize, true);
            DialogUtils.enableComponents(targetWindowSizeLabel, targetWindowSize, true);
            DialogUtils.enableComponents(sigmaStrLabel, sigmaStr, true);
        }

        if (item.equals(SpeckleFilterOp.IDAN_FILTER)) {
            DialogUtils.enableComponents(numLooksLabel, numLooks, true);
            DialogUtils.enableComponents(anSizeLabel, anSize, true);
        }
    }
}
