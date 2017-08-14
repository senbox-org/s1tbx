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
package org.esa.s1tbx.sar.gpf.ui.geometric;

import org.esa.s1tbx.sar.gpf.geometric.SRGROp;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for SRGROp
 */
public class SRGROpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JScrollPane bandListPane = new JScrollPane(bandList);
    private final JLabel bandListLabel = new JLabel("Source Bands:");

    private final JComboBox<Integer> warpPolynomialOrder = new JComboBox(new Integer[]{1, 2, 3, 4});
    //private final JComboBox<String> warpPolynomialOrder = new JComboBox<>(new String[]{"1", "2", "3", "4"});
    private final JComboBox<String> interpolationMethod = new JComboBox<>(new String[]{
            SRGROp.nearestNeighbourStr, SRGROp.linearStr, SRGROp.cubicStr, SRGROp.cubic2Str, SRGROp.sincStr});

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        if (sourceProducts != null) {
            DialogUtils.enableComponents(bandListLabel, bandListPane, true);
        }
        warpPolynomialOrder.setSelectedItem(paramMap.get("warpPolynomialOrder"));

        interpolationMethod.setSelectedItem(paramMap.get("interpolationMethod"));
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        //paramMap.put("warpPolynomialOrder", Integer.parseInt((String) warpPolynomialOrder.getSelectedItem()));
        paramMap.put("warpPolynomialOrder", warpPolynomialOrder.getSelectedItem());

        paramMap.put("interpolationMethod", interpolationMethod.getSelectedItem());
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Warp Polynomial Order:", warpPolynomialOrder);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Interpolation Method:", interpolationMethod);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}
