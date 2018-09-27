/*
 * Copyright (C) 2018 Skywatch Space Applications Inc. https://www.skywatch.co
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

import org.csa.rstb.polarimetric.gpf.CompactPolDecompositionOp;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

public class CompactPolDecompositionOpUI extends BaseOperatorUI {

    private final JComboBox decomposition = new JComboBox(new String[]{
            CompactPolDecompositionOp.M_CHI_DECOMPOSITION,
            CompactPolDecompositionOp.M_DELTA_DECOMPOSITION,
            CompactPolDecompositionOp.H_ALPHA_DECOMPOSITION,
            CompactPolDecompositionOp.RVOG_DECOMPOSITION
    });

    private final JComboBox windowSizeXStr = new JComboBox(new String[]{"3", "5", "7", "9", "11", "13", "15", "17", "19"});
    private final JComboBox windowSizeYStr = new JComboBox(new String[]{"3", "5", "7", "9", "11", "13", "15", "17", "19"});
    private final JCheckBox alphaByC2CheckBox = new JCheckBox("Compute Alpha By C2");
    private final JCheckBox alphaByT3CheckBox = new JCheckBox("Compute Alpha By T3");
    private boolean computeAlphaByT3 = true;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        alphaByT3CheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                computeAlphaByT3 = (e.getStateChange() == ItemEvent.SELECTED);
                alphaByC2CheckBox.setSelected(!computeAlphaByT3);
            }
        });

        alphaByC2CheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                computeAlphaByT3 = (e.getStateChange() != ItemEvent.SELECTED);
                alphaByT3CheckBox.setSelected(computeAlphaByT3);
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        decomposition.setSelectedItem(paramMap.get("decomposition"));
        windowSizeXStr.setSelectedItem(paramMap.get("windowSizeXStr"));
        windowSizeYStr.setSelectedItem(paramMap.get("windowSizeYStr"));

        Boolean paramVal = (Boolean) paramMap.get("computeAlphaByT3");
        if (paramVal != null) {
            computeAlphaByT3 = paramVal;
            alphaByT3CheckBox.setSelected(computeAlphaByT3);
            alphaByC2CheckBox.setSelected(!computeAlphaByT3);
        }
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("decomposition", decomposition.getSelectedItem());
        paramMap.put("windowSizeXStr", windowSizeXStr.getSelectedItem());
        paramMap.put("windowSizeYStr", windowSizeYStr.getSelectedItem());
        paramMap.put("computeAlphaByT3", computeAlphaByT3);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Decomposition:", decomposition);

        decomposition.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != ItemEvent.SELECTED)
                    return;
                String item = (String) decomposition.getSelectedItem();

                if (item.equals(CompactPolDecompositionOp.H_ALPHA_DECOMPOSITION)) {
                    alphaByT3CheckBox.setVisible(true);
                    alphaByC2CheckBox.setVisible(true);
                    alphaByT3CheckBox.setSelected(true);
                    alphaByC2CheckBox.setSelected(false);
                } else {
                    alphaByT3CheckBox.setVisible(false);
                    alphaByC2CheckBox.setVisible(false);
                }

            }
        });

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Window Size X:", windowSizeXStr);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Window Size Y:", windowSizeYStr);

        gbc.gridy++;
        contentPane.add(alphaByT3CheckBox, gbc);
        alphaByT3CheckBox.setVisible(false);

        gbc.gridy++;
        contentPane.add(alphaByC2CheckBox, gbc);
        alphaByC2CheckBox.setVisible(false);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}