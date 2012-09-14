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

import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for SARSimTerrainCorrectionOp
 */
public class SARSimTerrainCorrectionOpUI extends RangeDopplerGeocodingOpUI {

    private final JTextField rmsThreshold = new JTextField("");
    private final JTextField warpPolynomialOrder = new JTextField("");
    private final JCheckBox openShiftsFileCheckBox = new JCheckBox("Show Range and Azimuth Shifts");
    private boolean openShiftsFile = false;
    private final JCheckBox openResidualsFileCheckBox = new JCheckBox("Show Residuals");
    private boolean openResidualsFile = false;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {
        final JComponent pane = super.CreateOpTab(operatorName, parameterMap, appContext);

        openShiftsFileCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    openShiftsFile = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        openResidualsFileCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    openResidualsFile = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        return new JScrollPane(pane);
    }

    @Override
    public void initParameters() {
        super.initParameters();

        float threshold = (Float)paramMap.get("rmsThreshold");
        rmsThreshold.setText(String.valueOf(threshold));

        int order = (Integer)paramMap.get("warpPolynomialOrder");
        warpPolynomialOrder.setText(String.valueOf(order));

        openShiftsFile = (Boolean)paramMap.get("openShiftsFile");
        openShiftsFileCheckBox.getModel().setPressed(openShiftsFile);

        openResidualsFile = (Boolean)paramMap.get("openResidualsFile");
        openResidualsFileCheckBox.getModel().setPressed(openResidualsFile);
    }

    @Override
    public void updateParameters() {
        super.updateParameters();

        paramMap.put("rmsThreshold", Float.parseFloat(rmsThreshold.getText()));
        paramMap.put("warpPolynomialOrder", Integer.parseInt(warpPolynomialOrder.getText()));
        paramMap.put("openShiftsFile", openShiftsFile);
        paramMap.put("openResidualsFile", openResidualsFile);
    }

    @Override
    protected JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "RMS Threshold (pixel accuracy):", rmsThreshold);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "WARP Polynomial Order:", warpPolynomialOrder);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Image Resampling Method:", imgResamplingMethod);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, sourcePixelSpacingsLabelPart1, sourcePixelSpacingsLabelPart2);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Pixel Spacing (m):", pixelSpacingInMeter);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Pixel Spacing (deg):", pixelSpacingInDegree);

        pixelSpacingInMeter.addFocusListener(new PixelSpacingMeterListener());
        pixelSpacingInDegree.addFocusListener(new PixelSpacingDegreeListener());
        
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Map Projection:", crsButton);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(saveLocalIncidenceAngleCheckBox, gbc);
        gbc.gridx = 1;
        contentPane.add(saveProjectedLocalIncidenceAngleCheckBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(saveSelectedSourceBandCheckBox, gbc);
        gbc.gridx = 1;
        contentPane.add(saveDEMCheckBox, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(applyRadiometricNormalizationCheckBox, gbc);
        gbc.gridy++;
        gbc.insets.left = 20;
        contentPane.add(saveSigmaNoughtCheckBox, gbc);
        gbc.gridx = 1;
        gbc.insets.left = 1;
        contentPane.add(incidenceAngleForSigma0, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets.left = 20;
        contentPane.add(saveGammaNoughtCheckBox, gbc);
        gbc.gridx = 1;
        gbc.insets.left = 1;
        contentPane.add(incidenceAngleForGamma0, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets.left = 20;
        contentPane.add(saveBetaNoughtCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets.left = 20;
        DialogUtils.addComponent(contentPane, gbc, auxFileLabel, auxFile);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, externalAuxFileLabel, externalAuxFile);
        gbc.gridx = 2;
        contentPane.add(externalAuxFileBrowseButton, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets.left = 1;
        contentPane.add(openShiftsFileCheckBox, gbc);
        gbc.gridy++;
        contentPane.add(openResidualsFileCheckBox, gbc);

        return contentPane;
    }
}