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
package org.esa.s1tbx.insar.gpf.ui.coregistration;

import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.dataop.resamp.ResamplingFactory;
import org.esa.snap.framework.ui.AppContext;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User interface for DEMBasedCoregistrationOp
 */
public class DEMBasedCoregistrationOpUI extends BaseOperatorUI {

    private final JList mstBandList = new JList();
    private final JList slvBandList = new JList();

    private final JComboBox<String> demName = new JComboBox<>(DEMFactory.getDEMNameList());
    private final JComboBox demResamplingMethod = new JComboBox<>(ResamplingFactory.resamplingNames);
    private final JComboBox resamplingType = new JComboBox(ResamplingFactory.resamplingNames);

    private final JTextField externalDEMFile = new JTextField("");
    private final JTextField externalDEMNoDataValue = new JTextField("");
    private final JButton externalDEMBrowseButton = new JButton("...");
    private final JLabel externalDEMFileLabel = new JLabel("External DEM:");
    private final JLabel externalDEMNoDataValueLabel = new JLabel("DEM No Data Value:");
    private static final String externalDEMStr = "External DEM";
    private Double extNoDataValue = 0.0;

    private final DialogUtils.TextAreaKeyListener textAreaKeyListener = new DialogUtils.TextAreaKeyListener();

    private final List<Integer> defaultMasterBandIndices = new ArrayList<>(2);
    private final List<Integer> defaultSlaveBandIndices = new ArrayList<>(2);

    private Product masterProduct = null;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        demName.addItem(externalDEMStr);
        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        demName.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (((String) demName.getSelectedItem()).startsWith(externalDEMStr)) {
                    enableExternalDEM(true);
                } else {
                    externalDEMFile.setText("");
                    enableExternalDEM(false);
                }
            }
        });
        externalDEMFile.setColumns(30);
        enableExternalDEM(((String) demName.getSelectedItem()).startsWith(externalDEMStr));

        externalDEMBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File file = SnapDialogs.requestFileForOpen("External DEM File", false, null, null);
                externalDEMFile.setText(file.getAbsolutePath());
                extNoDataValue = OperatorUIUtils.getNoDataValue(file);
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        });

        externalDEMNoDataValue.addKeyListener(textAreaKeyListener);

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        //updateMasterSlaveSelections();

        final String demNameParam = (String) paramMap.get("demName");
        if (demNameParam != null)
            demName.setSelectedItem(DEMFactory.appendAutoDEM(demNameParam));
        demResamplingMethod.setSelectedItem(paramMap.get("demResamplingMethod"));

        final File extFile = (File) paramMap.get("externalDEMFile");
        if (extFile != null) {
            externalDEMFile.setText(extFile.getAbsolutePath());
            extNoDataValue = (Double) paramMap.get("externalDEMNoDataValue");
            if (extNoDataValue != null && !textAreaKeyListener.isChangedByUser()) {
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        }

        resamplingType.setSelectedItem(paramMap.get("resamplingType"));

    }

    private static List<Integer> getSelectedIndices(final String[] allBandNames,
                                                    final String[] selBandNames,
                                                    final List<Integer> defaultIndices) {
        final List<Integer> bandIndices = new ArrayList<>(2);
        if (selBandNames != null && selBandNames.length > 0) {
            int i = 0;
            for (String bandName : allBandNames) {
                for (String selName : selBandNames) {
                    if (bandName.equals(selName)) {
                        bandIndices.add(i);
                    }
                }
                ++i;
            }
        }

        if (bandIndices.isEmpty())
            return defaultIndices;
        return bandIndices;
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        //OperatorUIUtils.updateParamList(mstBandList, paramMap, "masterBandNames");
        //OperatorUIUtils.updateParamList(slvBandList, paramMap, "slaveBandNames");

        paramMap.put("demName", DEMFactory.getProperDEMName((String) demName.getSelectedItem()));
        paramMap.put("demResamplingMethod", demResamplingMethod.getSelectedItem());

        final String extFileStr = externalDEMFile.getText();
        if (!extFileStr.isEmpty()) {
            paramMap.put("externalDEMFile", new File(extFileStr));
            paramMap.put("externalDEMNoDataValue", Double.parseDouble(externalDEMNoDataValue.getText()));
        }

        paramMap.put("resamplingType", resamplingType.getSelectedItem());

    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        /*
        contentPane.add(new JLabel("Master Bands:"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(mstBandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;

        contentPane.add(new JLabel("Slave Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(slvBandList), gbc);
        */
        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Digital Elevation Model:", demName);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, externalDEMFileLabel, externalDEMFile);
        gbc.gridx = 2;
        contentPane.add(externalDEMBrowseButton, gbc);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, externalDEMNoDataValueLabel, externalDEMNoDataValue);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "DEM Resampling Method:", demResamplingMethod);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Resampling Type:", resamplingType);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void updateMasterSlaveSelections() {
        final String bandNames[] = getBandNames();
        OperatorUIUtils.initParamList(mstBandList, bandNames);
        OperatorUIUtils.initParamList(slvBandList, bandNames);

        OperatorUIUtils.setSelectedListIndices(mstBandList, getSelectedIndices(bandNames,
                new String[]{}, //String[])paramMap.get("masterBandNames"),
                defaultMasterBandIndices));
        OperatorUIUtils.setSelectedListIndices(slvBandList, getSelectedIndices(bandNames,
                new String[]{}, //(String[])paramMap.get("slaveBandNames"),
                defaultSlaveBandIndices));
    }

    @Override
    protected String[] getBandNames() {
        if (sourceProducts == null) {
            return new String[]{};
        }
        if (masterProduct == null && sourceProducts.length > 0) {
            masterProduct = sourceProducts[0];
        }
        defaultMasterBandIndices.clear();
        defaultSlaveBandIndices.clear();

        if (sourceProducts.length > 1) {
            for (int i = 1; i < sourceProducts.length; ++i) {
                if (sourceProducts[i].getDisplayName().equals(masterProduct.getDisplayName())) {
                    masterProduct = null;
                    return new String[]{};
                }
            }
        }

        final List<String> bandNames = new ArrayList<>(5);
        boolean masterBandsSelected = false;
        for (Product prod : sourceProducts) {
            if (sourceProducts.length > 1) {

                final Band[] bands = prod.getBands();
                for (int i = 0; i < bands.length; ++i) {
                    final Band band = bands[i];
                    bandNames.add(band.getName() + "::" + prod.getName());
                    final int index = bandNames.size() - 1;

                    if (!(band instanceof VirtualBand)) {

                        if (prod == masterProduct && !masterBandsSelected) {
                            defaultMasterBandIndices.add(index);
                            if (band.getUnit() != null && band.getUnit().equals(Unit.REAL)) {
                                if (i + 1 < bands.length) {
                                    final Band qBand = bands[i + 1];
                                    if (qBand.getUnit() != null && qBand.getUnit().equals(Unit.IMAGINARY)) {
                                        defaultMasterBandIndices.add(index + 1);
                                        bandNames.add(qBand.getName() + "::" + prod.getName());
                                        ++i;
                                    }
                                }
                            }
                            masterBandsSelected = true;
                        } else {
                            defaultSlaveBandIndices.add(index);
                        }
                    }
                }
            } else {
                bandNames.addAll(Arrays.asList(prod.getBandNames()));
            }
        }

        return bandNames.toArray(new String[bandNames.size()]);
    }

    private void enableExternalDEM(boolean flag) {
        DialogUtils.enableComponents(externalDEMFileLabel, externalDEMFile, flag);
        DialogUtils.enableComponents(externalDEMNoDataValueLabel, externalDEMNoDataValue, flag);
        externalDEMBrowseButton.setVisible(flag);
    }

}
