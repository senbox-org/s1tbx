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
package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.DialogUtils;
import org.jlinda.core.stacks.MasterSelection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User interface for CreateStackOp
 */
public class CreateStackOpUI extends BaseOperatorUI {

    private final JList mstBandList = new JList();
    private final JList slvBandList = new JList();

    private final List<Integer> defaultMasterBandIndices = new ArrayList<Integer>(2);
    private final List<Integer> defaultSlaveBandIndices = new ArrayList<Integer>(2);

    private final JComboBox resamplingType = new JComboBox(ResamplingFactory.resamplingNames);

    private final JComboBox extent = new JComboBox(new String[] {   CreateStackOp.MASTER_EXTENT,
                                                                    CreateStackOp.MIN_EXTENT,
                                                                    CreateStackOp.MAX_EXTENT });
    private final JButton optimalMasterButton = new JButton("Find Optimal Master");
    private Product masterProduct = null;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        resamplingType.addItem("NONE");

        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        enableOptimalMasterButton();
        updateMasterSlaveSelections();

        resamplingType.setSelectedItem(paramMap.get("resamplingType"));
        extent.setSelectedItem(paramMap.get("extent"));
    }

    private static List<Integer> getSelectedIndices(final String[] allBandNames,
                                                         final String[] selBandNames,
                                                         final List<Integer> defaultIndices) {
        final List<Integer> bandIndices = new ArrayList<Integer>(2);
        if(selBandNames != null && selBandNames.length > 0) {
            int i=0;
            for(String bandName : allBandNames) {
                for(String selName : selBandNames) {
                    if(bandName.equals(selName)) {
                        bandIndices.add(i);
                    }
                }
                ++i;
            }
        }

        if(bandIndices.isEmpty())
            return defaultIndices;
        return bandIndices;
    }

    @Override
    public UIValidation validateParameters() {

        if(resamplingType.getSelectedItem().equals("NONE") && sourceProducts != null) {
            try {
                CreateStackOp.checkPixelSpacing(sourceProducts);
            } catch(Exception e) {
                return new UIValidation(UIValidation.State.WARNING, "Resampling type cannot be NONE" +
                                " because pixel spacings are different for master and slave products");
            }
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(mstBandList, paramMap, "masterBandNames");
        OperatorUIUtils.updateParamList(slvBandList, paramMap, "slaveBandNames");

        paramMap.put("resamplingType", resamplingType.getSelectedItem());
        paramMap.put("extent", extent.getSelectedItem());
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

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
        gbc.gridx = 0;
        gbc.gridy++;

        DialogUtils.addComponent(contentPane, gbc, "Resampling Type:", resamplingType);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Output Extents:", extent);
        gbc.gridy++;

        optimalMasterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(sourceProducts != null) {
                    masterProduct = MasterSelection.findOptimalMasterProduct(sourceProducts);
                }
                updateMasterSlaveSelections();
            }
        });
        gbc.fill = GridBagConstraints.VERTICAL;
        contentPane.add(optimalMasterButton, gbc);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableOptimalMasterButton() {
        if(sourceProducts == null) return;

        for(Product prod : sourceProducts) {
            if(!OperatorUtils.isComplex(prod)) {
                optimalMasterButton.setEnabled(false);
                return;
            }
        }
    }

    private void updateMasterSlaveSelections() {
        final String bandNames[] = getBandNames();
        OperatorUIUtils.initParamList(mstBandList, bandNames);
        OperatorUIUtils.initParamList(slvBandList, bandNames);

        OperatorUIUtils.setSelectedListIndices(mstBandList, getSelectedIndices(bandNames,
                                                                (String[])paramMap.get("masterBandNames"),
                                                                defaultMasterBandIndices));
        OperatorUIUtils.setSelectedListIndices(slvBandList, getSelectedIndices(bandNames,
                                                                (String[])paramMap.get("slaveBandNames"),
                                                                defaultSlaveBandIndices));
    }

    @Override
    protected String[] getBandNames() {
        if(sourceProducts == null) {
            return new String[] {};
        }
        if(masterProduct == null && sourceProducts.length > 0) {
            masterProduct = sourceProducts[0];
        }
        defaultMasterBandIndices.clear();
        defaultSlaveBandIndices.clear();

        if(sourceProducts.length > 1) {
            for(int i=1; i < sourceProducts.length; ++i) {
                if(sourceProducts[i].getDisplayName().equals(masterProduct.getDisplayName())) {
                    masterProduct = null;
                    return new String[] {};
                }
            }
        }

        final List<String> bandNames = new ArrayList<String>(5);
        boolean masterBandsSelected = false;
        for(Product prod : sourceProducts) {
            if(sourceProducts.length > 1) {

                final Band[] bands = prod.getBands();
                for(int i=0; i < bands.length; ++i) {
                    final Band band = bands[i];
                    bandNames.add(band.getName()+"::"+prod.getName());
                    final int index = bandNames.size()-1;

                    if(!(band instanceof VirtualBand)) {

                        if(prod == masterProduct && !masterBandsSelected) {
                            defaultMasterBandIndices.add(index);
                            if(band.getUnit() != null && band.getUnit().equals(Unit.REAL)) {
                                if(i+1 < bands.length) {
                                    final Band qBand = bands[i+1];
                                    if(qBand.getUnit() != null && qBand.getUnit().equals(Unit.IMAGINARY)) {
                                        defaultMasterBandIndices.add(index+1);
                                        bandNames.add(qBand.getName()+"::"+prod.getName());
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

}