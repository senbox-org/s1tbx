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
package org.esa.s1tbx.sentinel1.gpf.ui;

import org.esa.s1tbx.insar.gpf.Sentinel1Utils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.util.DialogUtils;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

/**
 * User interface for S-1 TOPSAR Split
 */
public class TOPSARSplitOpUI extends BaseOperatorUI {

    private final JComboBox<String> subswathCombo = new JComboBox<>();
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

            final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
            subswathCombo.removeAllItems();
            if(acquisitionMode.equals("IW")) {
                subswathCombo.addItem("IW1");
                subswathCombo.addItem("IW2");
                subswathCombo.addItem("IW3");
            } else if(acquisitionMode.equals("EW")) {
                subswathCombo.addItem("EW1");
                subswathCombo.addItem("EW2");
                subswathCombo.addItem("EW3");
                subswathCombo.addItem("EW4");
                subswathCombo.addItem("EW5");
            }
            String subswath = (String)paramMap.get("subswath");
            if(subswath == null) {
                subswath = acquisitionMode+'1';
            }
            subswathCombo.setSelectedItem(subswath);

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

        String subswathValue = (String)subswathCombo.getSelectedItem();
        if(subswathValue != null) {
            paramMap.put("subswath", subswathValue);
        }
        OperatorUIUtils.updateParamList(polList, paramMap, "selectedPolarisations");
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Subswath:", subswathCombo);
        ++gbc.gridy;
        DialogUtils.addComponent(contentPane, gbc, "Polarisations:", polList);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}
