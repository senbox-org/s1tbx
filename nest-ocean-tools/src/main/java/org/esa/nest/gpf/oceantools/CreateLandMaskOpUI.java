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
package org.esa.nest.gpf.oceantools;

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.gpf.OperatorUIUtils;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for CreateLandMaskOp
 */
public class CreateLandMaskOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox geometries = new JComboBox();

    private final JRadioButton landMask = new JRadioButton("Mask out the Land");
    private final JRadioButton seaMask = new JRadioButton("Mask out the Sea");
    private final JCheckBox useSRTMCheckBox = new JCheckBox("Use SRTM 3sec");
    private final JRadioButton geometryMask = new JRadioButton("Use Vector as Mask");
    private final JCheckBox invertGeometryCheckBox = new JCheckBox("Invert Vector");
    private final JCheckBox byPassCheckBox = new JCheckBox("Bypass");
    private boolean byPass = false;
    private boolean invertGeometry = false;
    private boolean useSRTM = true;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        useSRTMCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    useSRTM = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });
        byPassCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    byPass = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });
        invertGeometryCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    invertGeometry = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        final RadioListener myListener = new RadioListener();
        landMask.addActionListener(myListener);
        seaMask.addActionListener(myListener);
        geometryMask.addActionListener(myListener);

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        final Boolean doLandMask = (Boolean)paramMap.get("landMask");
        if(doLandMask != null && doLandMask) {
            landMask.setSelected(true);
        } else {
            seaMask.setSelected(true);
        }
        geometries.removeAllItems();
        final String[] geometryNames = getGeometries();
        for(String g : geometryNames) {
            geometries.addItem(g);
        }
        final String selectedGeometry = (String)paramMap.get("geometry");
        if(selectedGeometry != null) {
            geometryMask.setSelected(true);
            geometries.setSelectedItem(selectedGeometry);
        }

        useSRTM = (Boolean)paramMap.get("useSRTM");
        useSRTMCheckBox.setSelected(useSRTM);
        byPassCheckBox.setSelected((Boolean)paramMap.get("byPass"));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("landMask", landMask.isSelected());
        if(geometryMask.isSelected()) {
            paramMap.put("geometry", geometries.getSelectedItem());
            paramMap.put("invertGeometry", invertGeometry);
        }
        paramMap.put("byPass", byPass);
        paramMap.put("useSRTM", useSRTM);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(landMask, gbc);
        gbc.gridy++;
        contentPane.add(seaMask, gbc);
        gbc.gridy++;
        contentPane.add(useSRTMCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(geometryMask, gbc);
        gbc.gridy++;
        gbc.gridx = 1;
        contentPane.add(geometries, gbc);
        gbc.gridy++;
        contentPane.add(invertGeometryCheckBox, gbc);

        final ButtonGroup group = new ButtonGroup();
    	group.add(landMask);
	    group.add(seaMask);
        group.add(geometryMask);

        geometries.setEnabled(false);
        invertGeometryCheckBox.setEnabled(false);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(byPassCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private class RadioListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            boolean b = geometryMask.isSelected();
            geometries.setEnabled(b);
            invertGeometryCheckBox.setEnabled(b);
        }
    }
}