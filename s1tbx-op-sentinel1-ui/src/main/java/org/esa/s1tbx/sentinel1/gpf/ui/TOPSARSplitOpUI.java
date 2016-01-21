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
 * User interface for S-1 TOPSAR Split
 */
public class TOPSARSplitOpUI extends BaseOperatorUI {

    private final JComboBox<String> subswathCombo = new JComboBox<>();
    private final JList<String> polList = new JList<>();

    private final JLabel numBurstsLabelPart1 = new JLabel("Number of Bursts:");
    private final JLabel numBurstsLabelPart2 = new JLabel("0");
    private final JTextField firstBurstIndex = new JTextField("");
    private final JTextField lastBurstIndex = new JTextField("");
    private int numBursts = 0;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();
        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        firstBurstIndex.setText(String.valueOf(paramMap.get("firstBurstIndex")));
        lastBurstIndex.setText(String.valueOf(paramMap.get("lastBurstIndex")));

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
                    (String[]) paramMap.get("selectedPolarisations"));

            try {
                Sentinel1Utils su = new Sentinel1Utils(sourceProducts[0]);
                Sentinel1Utils.SubSwathInfo[] subSwathInfo = su.getSubSwath();
                for (int i = 0; i < subSwathInfo.length; i++) {
                    if (subSwathInfo[i].subSwathName.contains(subswath)) {
                        numBursts = subSwathInfo[i].numOfBursts;
                        break;
                    }
                }
            } catch (Exception e) {
                numBursts = 0;
            }

            final String text = Integer.toString(numBursts);
            numBurstsLabelPart2.setText(text);
        }
    }

    @Override
    public UIValidation validateParameters() {
        if (sourceProducts != null) {
            final int i0 = Integer.parseInt(firstBurstIndex.getText());
            final int i1 = Integer.parseInt(lastBurstIndex.getText());
            if (i1 < i0) {
                return new UIValidation(UIValidation.State.ERROR, "The last burst index must not be smaller than" +
                        " the first burst index");
            }

            if (i1 > numBursts) {
                return new UIValidation(UIValidation.State.ERROR, "The last burst index must not be greater than" +
                        " the total number of bursts");
            }
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        String subswathValue = (String)subswathCombo.getSelectedItem();
        if(subswathValue != null) {
            paramMap.put("subswath", subswathValue);
        }
        OperatorUIUtils.updateParamList(polList, paramMap, "selectedPolarisations");

        if (firstBurstIndex.getText().isEmpty()) {
            paramMap.put("firstBurstIndex", 0);
        } else {
            paramMap.put("firstBurstIndex", Integer.parseInt(firstBurstIndex.getText()));
        }

        if (lastBurstIndex.getText().isEmpty()) {
            paramMap.put("lastBurstIndex", 0);
        } else {
            paramMap.put("lastBurstIndex", Integer.parseInt(lastBurstIndex.getText()));
        }

    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Subswath:", subswathCombo);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Polarisations:", polList);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, numBurstsLabelPart1, numBurstsLabelPart2);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "First Burst Index:", firstBurstIndex);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Last Burst Index:", lastBurstIndex);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}
