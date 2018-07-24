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
package org.esa.s1tbx.calibration.gpf.ui;

import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * User interface for S-1 RemoveGRDBorderNoise operator
 */
public class RemoveGRDBorderNoiseOpUI extends BaseOperatorUI {

    private final JList<String> polList = new JList<>();
    private final JTextField borderLimit = new JTextField("");
    private final JTextField trimThreshold = new JTextField("");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();
        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        if (sourceProducts != null && sourceProducts.length > 0) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);

            OperatorUIUtils.initParamList(polList, Sentinel1Utils.getProductPolarizations(absRoot),
                    (String[])paramMap.get("selectedPolarisations"));
        }

        if(paramMap.get("borderLimit") != null) {
            borderLimit.setText(String.valueOf(paramMap.get("borderLimit")));
        }
        if(paramMap.get("trimThreshold") != null) {
            trimThreshold.setText(String.valueOf(paramMap.get("trimThreshold")));
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(polList, paramMap, "selectedPolarisations");
        if(!borderLimit.getText().isEmpty()) {
            paramMap.put("borderLimit", Integer.parseInt(borderLimit.getText()));
        }
        if(!trimThreshold.getText().isEmpty()) {
            paramMap.put("trimThreshold", Double.parseDouble(trimThreshold.getText()));
        }
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Polarisations:", polList);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Border margin limit [pixels]:", borderLimit);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Threshold:", trimThreshold);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}
