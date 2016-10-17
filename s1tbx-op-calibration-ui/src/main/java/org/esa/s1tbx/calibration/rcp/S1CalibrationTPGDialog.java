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
package org.esa.s1tbx.calibration.rcp;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.ui.GridBagUtils;
import org.esa.snap.ui.ModalDialog;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
    Prompt for calibration TPG inputs
 */
public class S1CalibrationTPGDialog extends ModalDialog {

    private final JCheckBox sigma0CheckBox = new JCheckBox("Sigma0 LUT       ");
    private final JCheckBox gammaCheckBox = new JCheckBox("Gamma LUT");
    private final JCheckBox beta0CheckBox = new JCheckBox("Beta0 LUT");
    private final JCheckBox dnCheckBox = new JCheckBox("DN LUT");
    private final JCheckBox noiseCheckBox = new JCheckBox("Noise LUT");

    private boolean ok = false;

    public S1CalibrationTPGDialog() {
        super(SnapApp.getDefault().getMainFrame(), "Create LUT Tie Point Grid", ModalDialog.ID_OK_CANCEL, null);

        final JPanel content = GridBagUtils.createPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        final JPanel calPanel = new JPanel();
        calPanel.setLayout(new BoxLayout(calPanel, BoxLayout.Y_AXIS));
        calPanel.setBorder(new TitledBorder("Calibration Vectors"));
        calPanel.add(sigma0CheckBox);
        calPanel.add(gammaCheckBox);
        calPanel.add(beta0CheckBox);
        calPanel.add(dnCheckBox);

        final JPanel noisePanel = new JPanel();
        noisePanel.setLayout(new BoxLayout(noisePanel, BoxLayout.Y_AXIS));
        noisePanel.setBorder(new TitledBorder("Noise Vectors"));
        noisePanel.add(noiseCheckBox);

        content.add(calPanel);
        content.add(noisePanel);

        getJDialog().setMinimumSize(new Dimension(300, 100));

        setContent(content);
    }

    protected void onOK() {
        ok = true;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }

    public boolean doSigma0() {
        return sigma0CheckBox.isSelected();
    }

    public boolean doGamma() {
        return gammaCheckBox.isSelected();
    }

    public boolean doBeta0() {
        return beta0CheckBox.isSelected();
    }

    public boolean doDN() {
        return dnCheckBox.isSelected();
    }

    public boolean doNoise() {
        return noiseCheckBox.isSelected();
    }

}
