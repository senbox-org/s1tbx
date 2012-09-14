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
import java.util.Map;

/**
 * User interface for RemoveAntennaPatternOp
 */
public class RemoveAntennaPatternOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return panel;
    }

    @Override
    public void initParameters() {
        OperatorUIUtils.initBandList(bandList, getBandNames());
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {
        OperatorUIUtils.updateBandList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Note:",
                new JLabel("This operator removes antenna pattern and range spreading loss "));
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "",
                new JLabel("for ASAR and ERS products. It also applies ADC to ERS product."));

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}