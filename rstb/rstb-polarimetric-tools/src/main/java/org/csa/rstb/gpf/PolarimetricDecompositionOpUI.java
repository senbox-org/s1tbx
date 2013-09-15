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

public class PolarimetricDecompositionOpUI extends BaseOperatorUI {

    private final JComboBox decomposition = new JComboBox(new String[] {
                                                          PolarimetricDecompositionOp.SINCLAIR_DECOMPOSITION,
                                                          PolarimetricDecompositionOp.PAULI_DECOMPOSITION,
                                                          PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION,
                                                          PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION,
                                                          PolarimetricDecompositionOp.VANZYL_DECOMPOSITION,
                                                          PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION,
                                                          PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION,
                                                          PolarimetricDecompositionOp.TOUZI_DECOMPOSITION
    } );

    private final JLabel windowSizeLabel = new JLabel("Window Size:   ");
    private final JTextField windowSize = new JTextField("");

    private final JCheckBox outputHAAlphaCheckBox = new JCheckBox("Entropy (H), Anisotropy (A), Alpha");
    private final JCheckBox outputBetaDeltaGammaLambdaCheckBox = new JCheckBox("Beta, Delta, Gamma, Lambda");
    private final JCheckBox outputAlpha123CheckBox = new JCheckBox("Alpha 1, Alpha 2, Alpha 3");
    private final JCheckBox outputLambda123CheckBox = new JCheckBox("Lambda 1, Lambda 2, Lambda 3");

    private final JCheckBox outputTouziParamSet0CheckBox = new JCheckBox("Psi, Tau, Alpha, Phi");
    private final JCheckBox outputTouziParamSet1CheckBox = new JCheckBox("Psi 1, Tau 1, Alpha 1, Phi 1");
    private final JCheckBox outputTouziParamSet2CheckBox = new JCheckBox("Psi 2, Tau 2, Alpha 2, Phi 2");
    private final JCheckBox outputTouziParamSet3CheckBox = new JCheckBox("Psi 3, Tau 3, Alpha 3, Phi 3");

    private boolean outputHAAlpha = true;
    private boolean outputBetaDeltaGammaLambda = false;
    private boolean outputAlpha123 = false;
    private boolean outputLambda123 = false;

    private boolean outputTouziParamSet0 = true;
    private boolean outputTouziParamSet1 = false;
    private boolean outputTouziParamSet2 = false;
    private boolean outputTouziParamSet3 = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        outputHAAlphaCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputHAAlpha = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        outputBetaDeltaGammaLambdaCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputBetaDeltaGammaLambda = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        outputAlpha123CheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputAlpha123 = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        outputLambda123CheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputLambda123 = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        outputTouziParamSet0CheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputTouziParamSet0 = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        outputTouziParamSet1CheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputTouziParamSet1 = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        outputTouziParamSet2CheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputTouziParamSet2 = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        outputTouziParamSet3CheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    outputTouziParamSet3 = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        return panel;
    }

    @Override
    public void initParameters() {

        decomposition.setSelectedItem(paramMap.get("decomposition"));
        windowSize.setText(String.valueOf(paramMap.get("windowSize")));

        outputHAAlpha = (Boolean)paramMap.get("outputHAAlpha");
        outputHAAlphaCheckBox.getModel().setPressed(outputHAAlpha);

        outputBetaDeltaGammaLambda = (Boolean)paramMap.get("outputBetaDeltaGammaLambda");
        outputBetaDeltaGammaLambdaCheckBox.getModel().setPressed(outputBetaDeltaGammaLambda);

        outputAlpha123 = (Boolean)paramMap.get("outputAlpha123");
        outputAlpha123CheckBox.getModel().setPressed(outputAlpha123);

        outputLambda123 = (Boolean)paramMap.get("outputLambda123");
        outputLambda123CheckBox.getModel().setPressed(outputLambda123);

        outputTouziParamSet0 = (Boolean)paramMap.get("outputTouziParamSet0");
        outputTouziParamSet0CheckBox.getModel().setPressed(outputTouziParamSet0);

        outputTouziParamSet1 = (Boolean)paramMap.get("outputTouziParamSet1");
        outputTouziParamSet1CheckBox.getModel().setPressed(outputTouziParamSet1);

        outputTouziParamSet2 = (Boolean)paramMap.get("outputTouziParamSet2");
        outputTouziParamSet2CheckBox.getModel().setPressed(outputTouziParamSet2);

        outputTouziParamSet3 = (Boolean)paramMap.get("outputTouziParamSet3");
        outputTouziParamSet3CheckBox.getModel().setPressed(outputTouziParamSet3);
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("decomposition", decomposition.getSelectedItem());
        paramMap.put("windowSize", Integer.parseInt(windowSize.getText()));
        paramMap.put("outputHAAlpha", outputHAAlpha);
        paramMap.put("outputBetaDeltaGammaLambda", outputBetaDeltaGammaLambda);
        paramMap.put("outputAlpha123", outputAlpha123);
        paramMap.put("outputLambda123", outputLambda123);
        paramMap.put("outputTouziParamSet0", outputTouziParamSet0);
        paramMap.put("outputTouziParamSet1", outputTouziParamSet1);
        paramMap.put("outputTouziParamSet2", outputTouziParamSet2);
        paramMap.put("outputTouziParamSet3", outputTouziParamSet3);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Decomposition:", decomposition);

        decomposition.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                String item = (String)decomposition.getSelectedItem();
                if (item.equals(PolarimetricDecompositionOp.FREEMAN_DURDEN_DECOMPOSITION)||
                    item.equals(PolarimetricDecompositionOp.YAMAGUCHI_DECOMPOSITION)||
                    item.equals(PolarimetricDecompositionOp.VANZYL_DECOMPOSITION)||
                    item.equals(PolarimetricDecompositionOp.CLOUDE_DECOMPOSITION) ||
                    item.equals(PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION) ||
                    item.equals(PolarimetricDecompositionOp.TOUZI_DECOMPOSITION)) {
                    DialogUtils.enableComponents(windowSizeLabel, windowSize, true);
                } else {
                    DialogUtils.enableComponents(windowSizeLabel, windowSize, false);
                }

                if(item.equals(PolarimetricDecompositionOp.H_A_ALPHA_DECOMPOSITION)) {
                    outputHAAlphaCheckBox.setVisible(true);
                    outputBetaDeltaGammaLambdaCheckBox.setVisible(true);
                    outputAlpha123CheckBox.setVisible(true);
                    outputLambda123CheckBox.setVisible(true);

                    outputHAAlphaCheckBox.setSelected(true);
                    outputBetaDeltaGammaLambdaCheckBox.setSelected(false);
                    outputAlpha123CheckBox.setSelected(false);
                    outputLambda123CheckBox.setSelected(false);
                } else {
                    outputHAAlphaCheckBox.setVisible(false);
                    outputBetaDeltaGammaLambdaCheckBox.setVisible(false);
                    outputAlpha123CheckBox.setVisible(false);
                    outputLambda123CheckBox.setVisible(false);
                }

                if (item.equals(PolarimetricDecompositionOp.TOUZI_DECOMPOSITION)) {
                    outputTouziParamSet0CheckBox.setVisible(true);
                    outputTouziParamSet1CheckBox.setVisible(true);
                    outputTouziParamSet2CheckBox.setVisible(true);
                    outputTouziParamSet3CheckBox.setVisible(true);

                    outputTouziParamSet0CheckBox.setSelected(true);
                    outputTouziParamSet1CheckBox.setSelected(false);
                    outputTouziParamSet2CheckBox.setSelected(false);
                    outputTouziParamSet3CheckBox.setSelected(false);
                } else {
                    outputTouziParamSet0CheckBox.setVisible(false);
                    outputTouziParamSet1CheckBox.setVisible(false);
                    outputTouziParamSet2CheckBox.setVisible(false);
                    outputTouziParamSet3CheckBox.setVisible(false);
                }
            }
        });

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, windowSizeLabel, windowSize);
        DialogUtils.enableComponents(windowSizeLabel, windowSize, false);

        gbc.gridx = 0;
        gbc.gridy++;
        int gridySaved = gbc.gridy;
        contentPane.add(outputHAAlphaCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputBetaDeltaGammaLambdaCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputAlpha123CheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputLambda123CheckBox, gbc);

        gbc.gridy = gridySaved;
        contentPane.add(outputTouziParamSet0CheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputTouziParamSet1CheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputTouziParamSet2CheckBox, gbc);
        gbc.gridy++;
        contentPane.add(outputTouziParamSet3CheckBox, gbc);

        outputHAAlphaCheckBox.setVisible(false);
        outputBetaDeltaGammaLambdaCheckBox.setVisible(false);
        outputAlpha123CheckBox.setVisible(false);
        outputLambda123CheckBox.setVisible(false);

        outputTouziParamSet0CheckBox.setVisible(false);
        outputTouziParamSet1CheckBox.setVisible(false);
        outputTouziParamSet2CheckBox.setVisible(false);
        outputTouziParamSet3CheckBox.setVisible(false);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}