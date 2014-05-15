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
import org.esa.nest.gpf.ui.BaseOperatorUI;
import org.esa.nest.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for S-1 thermal noise correction operator
 */
public class Sentinel1RemoveThermalNoiseOpUI extends BaseOperatorUI {

    private final JList<String> polList = new JList<String>();
    private final JCheckBox removeThermalNoiseCheckBox = new JCheckBox("Remove Thermal Noise");
    private final JCheckBox reIntroduceThermalNoiseCheckBox = new JCheckBox("Re-Introduce Thermal Noise");
    private boolean removeThermalNoise = false;
    private boolean reIntroduceThermalNoise = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        removeThermalNoiseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                removeThermalNoise = (e.getStateChange() == ItemEvent.SELECTED);
                if (removeThermalNoise) {
                    reIntroduceThermalNoise = false;
                    reIntroduceThermalNoiseCheckBox.setSelected(false);
                } else {
                    reIntroduceThermalNoise = true;
                    reIntroduceThermalNoiseCheckBox.setSelected(true);
                }
            }
        });

        reIntroduceThermalNoiseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reIntroduceThermalNoise = (e.getStateChange() == ItemEvent.SELECTED);
                if (reIntroduceThermalNoise) {
                    removeThermalNoise = false;
                    removeThermalNoiseCheckBox.setSelected(false);
                } else {
                    removeThermalNoise = true;
                    removeThermalNoiseCheckBox.setSelected(true);
                }
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        if (sourceProducts != null && sourceProducts.length > 0) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            final String[] polarisations = Sentinel1Utils.getProductPolarizations(absRoot);
            polList.setListData(polarisations);

            OperatorUIUtils.initParamList(polList, polarisations);
        }

        Boolean paramVal;
        paramVal = (Boolean) paramMap.get("removeThermalNoise");
        if (paramVal != null) {
            removeThermalNoise = paramVal;
            removeThermalNoiseCheckBox.setSelected(removeThermalNoise);
        }

        paramVal = (Boolean) paramMap.get("reIntroduceThermalNoise");
        if (paramVal != null) {
            reIntroduceThermalNoise = paramVal;
            reIntroduceThermalNoiseCheckBox.setSelected(reIntroduceThermalNoise);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(polList, paramMap, "selectedPolarisations");

        paramMap.put("removeThermalNoise", removeThermalNoise);
        paramMap.put("reIntroduceThermalNoise", reIntroduceThermalNoise);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Polarisations:", polList);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(removeThermalNoiseCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(reIntroduceThermalNoiseCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}