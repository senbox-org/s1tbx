/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for WarpOp
 */
public class WarpOpUI extends BaseOperatorUI {

    private final JComboBox warpPolynomialOrder = new JComboBox(new String[] { "1","2","3" } );
    private final JComboBox interpolationMethod = new JComboBox(new String[] {
           WarpOp.NEAREST_NEIGHBOR, WarpOp.BILINEAR, WarpOp.BICUBIC, WarpOp.BICUBIC2,
           WarpOp.TRI, WarpOp.CC4P, WarpOp.CC6P, WarpOp.TS6P, WarpOp.TS8P, WarpOp.TS16P} );

    private final JTextField rmsThreshold = new JTextField("");

    final JCheckBox openResidualsFileCheckBox = new JCheckBox("Show Residuals");
    boolean openResidualsFile;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        openResidualsFileCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    openResidualsFile = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        rmsThreshold.setText(String.valueOf(paramMap.get("rmsThreshold")));
        warpPolynomialOrder.setSelectedItem(paramMap.get("warpPolynomialOrder"));

        if(sourceProducts != null && sourceProducts.length > 0) {
            final boolean isComplex = OperatorUtils.isComplex(sourceProducts[0]);
            if(!isComplex) {
                interpolationMethod.removeAllItems();
                interpolationMethod.addItem(WarpOp.NEAREST_NEIGHBOR);
                interpolationMethod.addItem(WarpOp.BILINEAR);
                interpolationMethod.addItem(WarpOp.BICUBIC);
                interpolationMethod.addItem(WarpOp.BICUBIC2);
            }
        }

        interpolationMethod.setSelectedItem(paramMap.get("interpolationMethod"));
    }


    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("rmsThreshold", Float.parseFloat(rmsThreshold.getText()));
        paramMap.put("warpPolynomialOrder", Integer.parseInt((String)warpPolynomialOrder.getSelectedItem()));
        paramMap.put("interpolationMethod", interpolationMethod.getSelectedItem());
        paramMap.put("openResidualsFile", openResidualsFile);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "RMS Threshold (pixel accuracy):", rmsThreshold);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Warp Polynomial Order:", warpPolynomialOrder);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Interpolation Method:", interpolationMethod);
        gbc.gridy++;

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(openResidualsFileCheckBox, gbc);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}
