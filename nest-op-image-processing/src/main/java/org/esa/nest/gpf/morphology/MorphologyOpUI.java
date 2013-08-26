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
package org.esa.nest.gpf.morphology;

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

public class MorphologyOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox operator = new JComboBox(new String[]{MorphologyOp.DILATE_OPERATOR,
                MorphologyOp.ERODE_OPERATOR,
                MorphologyOp.OPEN_OPERATOR,
                MorphologyOp.CLOSE_OPERATOR});
    private final JLabel nIterationsLabel = new JLabel("Iterations");
    private final JTextField nIterations = new JTextField("");

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

        operator.setSelectedItem(paramMap.get("operator"));
        nIterations.setText(String.valueOf(paramMap.get("nIterations")));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("operator", operator.getSelectedItem());
        paramMap.put("nIterations", Integer.parseInt(nIterations.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Operator:", operator);

        operator.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent event) {
                updateOperatorSelection();
            }
        });

        gbc.gridy++;
        final int savedY = gbc.gridy;
        DialogUtils.addComponent(contentPane, gbc, nIterationsLabel, nIterations);
        gbc.gridy++;
        gbc.gridy = savedY;
        gbc.weightx = 1.0;
        contentPane.add(new JPanel(), gbc);

        DialogUtils.enableComponents(nIterationsLabel, nIterations, true);
        return contentPane;
    }

    private void updateOperatorSelection() {
        final String item = (String) operator.getSelectedItem();
    }
}