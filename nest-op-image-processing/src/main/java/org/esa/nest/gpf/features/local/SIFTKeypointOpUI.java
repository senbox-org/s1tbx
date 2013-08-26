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
package org.esa.nest.gpf.features.local;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.gpf.OperatorUIUtils;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class SIFTKeypointOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JLabel highThresholdLabel = new JLabel("HighThreshold");
    private final JTextField highThreshold = new JTextField("");
    private final JLabel lowThresholdLabel = new JLabel("LowThreshold");
    private final JTextField lowThreshold = new JTextField("");

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
        highThreshold.setText(String.valueOf(paramMap.get("highThreshold")));
        lowThreshold.setText(String.valueOf(paramMap.get("lowThreshold")));
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);
        paramMap.put("lowThreshold", Float.parseFloat(lowThreshold.getText()));
        paramMap.put("highThreshold", Float.parseFloat(highThreshold.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, lowThresholdLabel, lowThreshold);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, highThresholdLabel, highThreshold);
        final int savedY = gbc.gridy;
        gbc.gridy++;
        gbc.gridy = savedY;
        gbc.weightx = 1.0;

        contentPane.add(new JPanel(), gbc);

        DialogUtils.enableComponents(lowThresholdLabel, lowThreshold, true);
        DialogUtils.enableComponents(highThresholdLabel, highThreshold, true);
        return contentPane;
    }
}
