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
package org.esa.nest.dat.views.polarview;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;

/**

 */
class ColourScaleDialog extends ModalDialog {

    private final JTextField min = new JTextField("");
    private final JTextField max = new JTextField("");

    private boolean ok = false;
    private final Axis colourAxis;

    public ColourScaleDialog(Axis colourAxis) {
        super(VisatApp.getApp().getMainFrame(), "Colour Scale", ModalDialog.ID_OK_CANCEL, null);

        this.colourAxis = colourAxis;
        final double[] range = colourAxis.getRange();
        min.setText(String.valueOf(range[0]));
        max.setText(String.valueOf(range[range.length-1]));

        setContent(createPanel());
    }

    @Override
    protected void onOK() {

        colourAxis.setDataRange(Double.parseDouble(min.getText()), Double.parseDouble(max.getText()));
        
        ok = true;
        hide();
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "min:", min);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "max:", max);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }
}