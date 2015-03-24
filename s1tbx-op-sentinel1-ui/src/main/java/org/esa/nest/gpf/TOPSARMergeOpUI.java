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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.ui.AppContext;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.gpf.ui.OperatorUIUtils;
import org.esa.snap.gpf.ui.BaseOperatorUI;
import org.esa.snap.gpf.ui.UIValidation;
import org.esa.snap.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * User interface for TOPSAR merge operator
 */
public class TOPSARMergeOpUI extends BaseOperatorUI {

    private final JList<String> polList = new JList<>();

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
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(polList, paramMap, "selectedPolarisations");
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Polarisations:", polList);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}