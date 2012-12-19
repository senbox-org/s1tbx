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

import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * User interface for CreateElevationBandOp
 */
public class CreateElevationOpUI extends BaseOperatorUI {

    private final JComboBox<String> demName = new JComboBox<String>(DEMFactory.getDEMNameList());

    private final JTextField elevationBandName = new JTextField("");
    private final JTextField externalDEM = new JTextField("");

    private final JComboBox demResamplingMethod = new JComboBox<String>(ResamplingFactory.resamplingNames);

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        demResamplingMethod.addItem(DEMFactory.DELAUNAY_INTERPOLATION);

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        final String demNameParam = (String)paramMap.get("demName");
        if(demNameParam != null)
            demName.setSelectedItem(DEMFactory.appendAutoDEM(demNameParam));
        elevationBandName.setText(String.valueOf(paramMap.get("elevationBandName")));
        externalDEM.setText(String.valueOf(paramMap.get("externalDEM")));
        demResamplingMethod.setSelectedItem(paramMap.get("resamplingMethod"));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("demName", ((String)demName.getSelectedItem()).replace(DEMFactory.AUTODEM, ""));
        paramMap.put("elevationBandName", elevationBandName.getText());
        paramMap.put("externalDEM", externalDEM.getText());
        paramMap.put("resamplingMethod", demResamplingMethod.getSelectedItem());
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Digital Elevation Model:", demName);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "External DEM:", externalDEM);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Resampling Method:", demResamplingMethod);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Elevation Band Name:", elevationBandName);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}