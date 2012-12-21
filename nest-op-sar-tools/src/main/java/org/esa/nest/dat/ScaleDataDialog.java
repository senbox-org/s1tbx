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
package org.esa.nest.dat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Scale data action dialog
 */
class ScaleDataDialog extends ModelessDialog {

    private final JTextField gainField = new JTextField("1");
    private final JTextField biasField = new JTextField("0");
    private final JTextField expField = new JTextField("1");
    private final JCheckBox logCheck = new JCheckBox();

    private final Product _product;
    private final Band _band;

    public ScaleDataDialog(final String title, final Product product, final Band band) {
        super(VisatApp.getApp().getMainFrame(), title, ModalDialog.ID_OK_CANCEL, null);

        this._product = product;
        this._band = band;

        setContent(createEditPanel());
    }

    private JPanel createEditPanel() {
        final JPanel editPanel = new JPanel();
        editPanel.setPreferredSize(new Dimension(400, 200));
        editPanel.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        gbc.ipady = 5;
        gbc.weightx = 1;

        gbc.gridy++;
        DialogUtils.addComponent(editPanel, gbc, "Gain:", gainField);
        gbc.gridy++;
        DialogUtils.addComponent(editPanel, gbc, "Bias:", biasField);
        gbc.gridy++;
        DialogUtils.addComponent(editPanel, gbc, "Exponential Scaling:", expField);
        gbc.gridy++;
        DialogUtils.addComponent(editPanel, gbc, "Logarithmic Scaling:", logCheck);
        gbc.gridy++;

        DialogUtils.fillPanel(editPanel, gbc);

        return editPanel;
    }

    @Override
    protected void onOK() {

        try {
            final double gain = Double.parseDouble(gainField.getText());
            final double bias = Double.parseDouble(biasField.getText());
            final double exp = Double.parseDouble(expField.getText());
            final boolean isLog = logCheck.isSelected();

            applyScaling(_product, _band, gain, bias, exp, isLog);
            hide();
        } catch(Exception e) {
            VisatApp.getApp().showErrorDialog(e.getMessage());
        }
    }

    private static void applyScaling(final Product product, final Band band,
                              final double gain, final double bias, final double exp, final boolean isLog) {
        final String bandName = band.getName();
        final String unit = band.getUnit();

        String expression = gain + " * " +bandName+ " + " +bias;

        String targetName = bandName + "_Scaled";
        int cnt = 0;
        while(product.getBand(targetName) != null) {
            ++cnt;
            targetName = bandName + "_Scaled" + cnt;
        }

        if(exp != 1) {
            expression = "pow( " +expression+ ", " +exp+ " )";
        }

        if(isLog) {
            expression = bandName+"==0 ? 0 : "+"log10( "+expression+" )";
        }

        final VirtualBand virtBand = new VirtualBand(targetName,
                ProductData.TYPE_FLOAT32,
                band.getSceneRasterWidth(),
                band.getSceneRasterHeight(),
                expression);
        virtBand.setUnit(unit);
        virtBand.setDescription(band.getDescription());
        virtBand.setNoDataValueUsed(true);
        product.addBand(virtBand);
    }

}