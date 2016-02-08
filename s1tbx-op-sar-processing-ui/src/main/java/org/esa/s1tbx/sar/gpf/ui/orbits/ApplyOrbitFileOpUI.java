/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.ui.orbits;

import org.esa.s1tbx.io.orbits.DelftOrbitFile;
import org.esa.s1tbx.io.orbits.DorisOrbitFile;
import org.esa.s1tbx.io.orbits.PrareOrbitFile;
import org.esa.s1tbx.io.orbits.SentinelPODOrbitFile;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for Apply Orbit
 */
public class ApplyOrbitFileOpUI extends BaseOperatorUI {

    private final JComboBox<String> orbitTypeCombo = new JComboBox<>();
    private final JTextField polyDegree = new JTextField("");
    private final JCheckBox continueOnFailCheckBox = new JCheckBox("Do not fail if new orbit file is not found");

    private Boolean continueOnFail = true;

    private final static String[] ORBIT_TYPES = new String[] {
            SentinelPODOrbitFile.PRECISE + " (Auto Download)",
            SentinelPODOrbitFile.RESTITUTED + " (Auto Download)",
            DorisOrbitFile.DORIS_POR + " (ENVISAT)",
            DorisOrbitFile.DORIS_VOR + " (ENVISAT)" + " (Auto Download)",
            DelftOrbitFile.DELFT_PRECISE + " (ENVISAT, ERS1&2)" + " (Auto Download)",
            PrareOrbitFile.PRARE_PRECISE + " (ERS1&2)" + " (Auto Download)"
    };

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();

        for(String item : ORBIT_TYPES) {
            orbitTypeCombo.addItem(item);
        }

        continueOnFailCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                continueOnFail = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        initParameters();
        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        polyDegree.setText(String.valueOf(paramMap.get("polyDegree")));

        continueOnFail = (Boolean) paramMap.get("continueOnFail");
        if (continueOnFail != null) {
            continueOnFailCheckBox.setSelected(continueOnFail);
        }

        if (sourceProducts != null && sourceProducts.length > 0) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);

            if (mission.equals("ENVISAT")) {
                populateOrbitTypes("ENVISAT");
                setSelectedOrbitType(DorisOrbitFile.DORIS_VOR);
            } else if (mission.equals("ERS1") || mission.equals("ERS2")) {
                populateOrbitTypes("ERS");
                setSelectedOrbitType(PrareOrbitFile.PRARE_PRECISE);
            } else if (mission.startsWith("SENTINEL")) {
                populateOrbitTypes("Sentinel");
                setSelectedOrbitType(SentinelPODOrbitFile.PRECISE);
            }
        }
    }

    private void populateOrbitTypes(final String mission) {
        orbitTypeCombo.removeAllItems();

        for(String item : ORBIT_TYPES) {
            if(item.contains(mission)) {
                orbitTypeCombo.addItem(item);
            }
        }
    }

    private void setSelectedOrbitType(final String defaultStr) {
        final String orbitTypeFromGraph = (String)paramMap.get("orbitType");

        for(int i=0; i < orbitTypeCombo.getItemCount(); ++i) {
            if(orbitTypeCombo.getItemAt(i).startsWith(orbitTypeFromGraph)) {
                orbitTypeCombo.setSelectedItem(orbitTypeCombo.getItemAt(i));
                return;
            }
        }
        for(int i=0; i < orbitTypeCombo.getItemCount(); ++i) {
            if(orbitTypeCombo.getItemAt(i).startsWith(defaultStr)) {
                orbitTypeCombo.setSelectedItem(orbitTypeCombo.getItemAt(i));
                return;
            }
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        final String polyDegreeStr = polyDegree.getText();
        if (polyDegreeStr != null && !polyDegreeStr.isEmpty())
            paramMap.put("polyDegree", Integer.parseInt(polyDegreeStr));
        paramMap.put("continueOnFail", continueOnFail);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Orbit State Vectors:", orbitTypeCombo);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Polynomial Degree:", polyDegree);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "", continueOnFailCheckBox);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}