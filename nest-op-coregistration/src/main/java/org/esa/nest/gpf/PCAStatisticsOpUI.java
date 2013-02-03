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

import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * UI for PCA operator
 */
public class PCAStatisticsOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();

    private final JComboBox selectEigenvaluesBy = new JComboBox(new String[] { PCAOp.EIGENVALUE_THRESHOLD,
                                                                         PCAOp.NUMBER_EIGENVALUES } );

    private final JTextField eigenvalueThreshold = new JTextField("");
    private final JTextField numberOfEigenvalues = new JTextField("");
    private final JCheckBox showEigenvalues = new JCheckBox("Show Eigenvalues");
    private final JCheckBox subtractMeanImage = new JCheckBox("Subtract Mean Image");

    private final JLabel eigenvalueThresholdLabel = new JLabel("Eigenvalue Threshold (%):");
    private final JLabel numberOfEigenvaluesLabel = new JLabel("Number Of Eigenvalues:");
    private final JLabel selectEigenvaluesByLabel = new JLabel("Select Eigenvalues By:     ");

    private boolean showEigenvaluesFlag = false;
    private boolean subtractMeanImageFlag = false;

    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        
        bandList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(final ListSelectionEvent e) {
                    setNumberOfEigenvalues();
                }
        });

        showEigenvalues.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    showEigenvaluesFlag = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        subtractMeanImage.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    subtractMeanImageFlag = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        initParameters();

        return panel;
    }

    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        selectEigenvaluesBy.setSelectedItem(paramMap.get("selectEigenvaluesBy"));
        eigenvalueThreshold.setText(String.valueOf(paramMap.get("eigenvalueThreshold")));
        setNumberOfEigenvalues();
    }

    private void setNumberOfEigenvalues() {
        if(sourceProducts != null && sourceProducts.length > 0) {
            if (bandList.getSelectedValues().length > 0) {
                numberOfEigenvalues.setText(String.valueOf(bandList.getSelectedValues().length));
            } else {
                numberOfEigenvalues.setText(String.valueOf(getBandNames().length));
            }
        } else {
            numberOfEigenvalues.setText(String.valueOf(paramMap.get("numPCA")));
        }
    }

    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    public void updateParameters() {

        if (sourceProducts == null) {
            OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);
        } else {
            if (bandList.getSelectedValues().length > 0) {
                OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);
            } else {
                paramMap.put("sourceBandNames", sourceProducts[0].getBandNames());
            }
        }

        paramMap.put("selectEigenvaluesBy", selectEigenvaluesBy.getSelectedItem());
        paramMap.put("eigenvalueThreshold", Double.parseDouble(eigenvalueThreshold.getText()));
        paramMap.put("numPCA", Integer.parseInt(numberOfEigenvalues.getText()));
        paramMap.put("showEigenvalues", showEigenvaluesFlag);
        paramMap.put("subtractMeanImage", subtractMeanImageFlag);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints _gbc = GridBagUtils.createDefaultConstraints();
        _gbc.fill = GridBagConstraints.HORIZONTAL;
        _gbc.anchor = GridBagConstraints.NORTHWEST;
        _gbc.insets.top = 2;
        _gbc.insets.bottom = 2;

        _gbc.gridx = 0;
        _gbc.gridy = 0;
        contentPane.add(new JLabel("Source Bands:"), _gbc);
        _gbc.fill = GridBagConstraints.BOTH;
        _gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), _gbc);
        _gbc.fill = GridBagConstraints.HORIZONTAL;

        _gbc.gridy++;
        _gbc.gridx = 0;
        contentPane.add(selectEigenvaluesByLabel, _gbc);
        _gbc.gridx = 1;
        contentPane.add(selectEigenvaluesBy, _gbc);
        selectEigenvaluesBy.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateSelectEigenvaluesBy(true);
            }
        });

        int savedY = ++_gbc.gridy;
        DialogUtils.addComponent(contentPane, _gbc, eigenvalueThresholdLabel, eigenvalueThreshold);

        _gbc.gridy = savedY;
        DialogUtils.addComponent(contentPane, _gbc, numberOfEigenvaluesLabel, numberOfEigenvalues);

        _gbc.gridy++;
        _gbc.gridx = 0;        
        contentPane.add(showEigenvalues, _gbc);

        _gbc.gridy++;
        contentPane.add(subtractMeanImage, _gbc);

        updateSelectEigenvaluesBy(true);

        return contentPane;
    }

    private void enableEigenvalueThreshold(boolean flag) {
        DialogUtils.enableComponents(eigenvalueThresholdLabel, eigenvalueThreshold, flag);
    }

    private void enableNumberOfEigenvalues(boolean flag) {
        if (flag) {
            setNumberOfEigenvalues();
        }
        DialogUtils.enableComponents(numberOfEigenvaluesLabel, numberOfEigenvalues, flag);
    }

    private void updateSelectEigenvaluesBy(boolean show) {
        if(show) {
            selectEigenvaluesBy.setVisible(true);
            selectEigenvaluesByLabel.setVisible(true);

            String item = (String)selectEigenvaluesBy.getSelectedItem();
            if(item.equals(PCAOp.EIGENVALUE_THRESHOLD)) {
                enableEigenvalueThreshold(true);
                enableNumberOfEigenvalues(false);
            } else {
                enableEigenvalueThreshold(false);
                enableNumberOfEigenvalues(true);
            }
        } else {
            selectEigenvaluesBy.setVisible(false);
            selectEigenvaluesByLabel.setVisible(false);
            enableEigenvalueThreshold(false);
            enableNumberOfEigenvalues(false);
        }
    }
}