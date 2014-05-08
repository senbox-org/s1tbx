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
package org.esa.nest.gpf.filtering;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.gpf.OperatorUIUtils;
import org.esa.nest.util.DialogUtils;

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

    private final JComboBox filter = new JComboBox(new String[] { SpeckleFilterOp.MEAN_SPECKLE_FILTER,
                                                                  SpeckleFilterOp.MEDIAN_SPECKLE_FILTER,
                                                                  SpeckleFilterOp.FROST_SPECKLE_FILTER,
                                                                  SpeckleFilterOp.GAMMA_MAP_SPECKLE_FILTER,
                                                                  SpeckleFilterOp.LEE_SPECKLE_FILTER,
                                                                  SpeckleFilterOp.LEE_REFINED_FILTER } );

    private final JLabel dampingFactorLabel = new JLabel("Damping Factor:");
    private final JLabel edgeThresholdLabel = new JLabel("Edge Threshold:");
    private final JLabel filterSizeXLabel = new JLabel("Filter Size X:   ");
    private final JLabel filterSizeYLabel = new JLabel("Filter Size Y:   ");

    private final JTextField filterSizeX = new JTextField("");
    private final JTextField filterSizeY = new JTextField("");
    private final JTextField dampingFactor = new JTextField("");
    private final JTextField edgeThreshold = new JTextField("");

    private final JLabel estimateENLCheckBoxLabel = new JLabel("Estimate Equivalent Number of Looks");
    private final JCheckBox estimateENLCheckBox = new JCheckBox("");
    private final JLabel enlLabel = new JLabel("Number of Looks:   ");
    private final JTextField enl = new JTextField("");
    private Boolean estimateENL = true;

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

        return panel;
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        filter.setSelectedItem(paramMap.get("filter"));
        filterSizeX.setText(String.valueOf(paramMap.get("filterSizeX")));
        filterSizeY.setText(String.valueOf(paramMap.get("filterSizeY")));
        dampingFactor.setText(String.valueOf(paramMap.get("dampingFactor")));
        edgeThreshold.setText(String.valueOf(paramMap.get("edgeThreshold")));
        estimateENLCheckBox.setSelected(true);
        estimateENL = true;
        enl.setEnabled(false);
        enl.setText(String.valueOf(paramMap.get("enl")));
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
        paramMap.put("edgeThreshold", Double.parseDouble(edgeThreshold.getText()));
        paramMap.put("estimateENL", estimateENL);
        paramMap.put("enl", Double.parseDouble(enl.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Filter:", filter);

        filter.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateFilterSelection();
            }
        });

        gbc.gridy++;
        final int savedY = gbc.gridy;
        DialogUtils.addComponent(contentPane, gbc, filterSizeXLabel, filterSizeX);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, filterSizeYLabel, filterSizeY);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, dampingFactorLabel, dampingFactor);

        DialogUtils.addComponent(contentPane, gbc, estimateENLCheckBoxLabel, estimateENLCheckBox);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, enlLabel, enl);

        gbc.gridy = savedY;
        DialogUtils.addComponent(contentPane, gbc, edgeThresholdLabel, edgeThreshold);

        gbc.weightx = 1.0;
        contentPane.add(new JPanel(), gbc);

        DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, true);
        DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, true);
        DialogUtils.enableComponents(dampingFactorLabel, dampingFactor, false);
        DialogUtils.enableComponents(estimateENLCheckBoxLabel, estimateENLCheckBox, false);
        DialogUtils.enableComponents(enlLabel, enl, false);
        DialogUtils.enableComponents(edgeThresholdLabel, edgeThreshold, false);

        return contentPane;
    }

    private void updateFilterSelection() {
        final String item = (String)filter.getSelectedItem();
        if(item.equals(SpeckleFilterOp.FROST_SPECKLE_FILTER)) {
            DialogUtils.enableComponents(dampingFactorLabel, dampingFactor, true);
        } else {
            DialogUtils.enableComponents(dampingFactorLabel, dampingFactor, false);
        }

        if (item.equals(SpeckleFilterOp.LEE_REFINED_FILTER)) {
            DialogUtils.enableComponents(edgeThresholdLabel, edgeThreshold, true);
            DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, false);
            DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, false);
        } else {
            DialogUtils.enableComponents(edgeThresholdLabel, edgeThreshold, false);
            DialogUtils.enableComponents(filterSizeXLabel, filterSizeX, true);
            DialogUtils.enableComponents(filterSizeYLabel, filterSizeY, true);
        }

        if (item.equals(SpeckleFilterOp.GAMMA_MAP_SPECKLE_FILTER) ||
                item.equals(SpeckleFilterOp.LEE_SPECKLE_FILTER)) {
            DialogUtils.enableComponents(estimateENLCheckBoxLabel, estimateENLCheckBox, true);
            DialogUtils.enableComponents(enlLabel, enl, true);
            estimateENLCheckBox.setSelected(true);
            enl.setEnabled(false);
        } else {
            DialogUtils.enableComponents(estimateENLCheckBoxLabel, estimateENLCheckBox, false);
            DialogUtils.enableComponents(enlLabel, enl, false);
        }
    }
}