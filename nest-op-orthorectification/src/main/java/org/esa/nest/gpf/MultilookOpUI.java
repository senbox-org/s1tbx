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
package org.esa.nest.gpf;

import org.esa.nest.gpf.ui.BaseOperatorUI;
import org.esa.nest.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * User interface for Multilook
 */
public class MultilookOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();

    private final JTextField nRgLooks = new JTextField("");
    private final JTextField nAzLooks = new JTextField("");
    private final JTextField meanGRSqaurePixel = new JTextField("");

    private final JCheckBox grSquarePixelCheckBox = new JCheckBox("GR Square Pixel");
    private final JCheckBox independentLooksCheckBox = new JCheckBox("Independent Looks");
    private final JCheckBox outputIntensityCheckBox = new JCheckBox("Output Intensity");

    private Boolean outputIntensity = true;
    private Boolean grSquarePixel = true;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        outputIntensityCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                outputIntensity = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        grSquarePixelCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                grSquarePixel = (e.getStateChange() == ItemEvent.SELECTED);
                independentLooksCheckBox.setSelected(!grSquarePixel);
                if (grSquarePixel) {
                    nAzLooks.setText("");
                    nAzLooks.setEditable(false);
                }
                setAzimuthLooks();
            }
        });

        independentLooksCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                grSquarePixel = (e.getStateChange() != ItemEvent.SELECTED);
                grSquarePixelCheckBox.setSelected(grSquarePixel);
                if (!grSquarePixel) {
                    nAzLooks.setEditable(true);
                }
                setAzimuthLooks();
            }
        });

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        nRgLooks.setText(String.valueOf(paramMap.get("nRgLooks")));
        nAzLooks.setText(String.valueOf(paramMap.get("nAzLooks")));

        outputIntensity = (Boolean) paramMap.get("outputIntensity");
        if (outputIntensity != null) {
            outputIntensityCheckBox.setSelected(outputIntensity);
        }
        outputIntensityCheckBox.setVisible(isComplexSrcProduct());

        grSquarePixel = (Boolean) paramMap.get("grSquarePixel");
        if (grSquarePixel != null) {
            grSquarePixelCheckBox.setSelected(grSquarePixel);
            independentLooksCheckBox.setSelected(!grSquarePixel);
            if (grSquarePixel) {
                nAzLooks.setText("");
                nAzLooks.setEditable(false);
            } else {
                nAzLooks.setEditable(true);
            }
        }

        setAzimuthLooks();
    }

    private void setAzimuthLooks() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            try {
                if (grSquarePixelCheckBox.isSelected()) {
                    final MultilookOp.DerivedParams param = new MultilookOp.DerivedParams();
                    param.nRgLooks = Integer.parseInt(nRgLooks.getText());
                    MultilookOp.getDerivedParameters(sourceProducts[0], param);
                    nAzLooks.setText(String.valueOf(param.nAzLooks));
                    nRgLooks.setText(String.valueOf(param.nRgLooks));

                    final float meanSqaurePixel = param.meanGRSqaurePixel;
                    meanGRSqaurePixel.setText(String.valueOf(meanSqaurePixel));
                } else { // independent looks
                    meanGRSqaurePixel.setText("");
                }
            } catch (Exception e) {
                meanGRSqaurePixel.setText("");
            }
        }
    }

    @Override
    public UIValidation validateParameters() {

        if (sourceProducts != null) {
            if (!isComplexSrcProduct() && !outputIntensity) {
                return new UIValidation(UIValidation.State.ERROR, "Cannot output in complex," +
                        " please select output intensity");
            }
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        final String nRgLooksStr = nRgLooks.getText();
        final String nAzLooksStr = nAzLooks.getText();
        if (nRgLooksStr != null && !nRgLooksStr.isEmpty())
            paramMap.put("nRgLooks", Integer.parseInt(nRgLooksStr));
        if (nAzLooksStr != null && !nAzLooksStr.isEmpty())
            paramMap.put("nAzLooks", Integer.parseInt(nAzLooksStr));
        paramMap.put("outputIntensity", outputIntensity);
        paramMap.put("grSquarePixel", grSquarePixel);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(grSquarePixelCheckBox, gbc);

        gbc.gridx = 1;
        contentPane.add(independentLooksCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Number of Range Looks:", nRgLooks);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Number of Azimuth Looks:", nAzLooks);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Mean GR Square Pixel:", meanGRSqaurePixel);

        nAzLooks.setEditable(false);
        meanGRSqaurePixel.setEditable(false);

        nRgLooks.addFocusListener(new FocusListener() {

            public void focusGained(final FocusEvent e) {
            }

            public void focusLost(final FocusEvent e) {
                setAzimuthLooks();
            }
        });

        nRgLooks.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                setAzimuthLooks();
            }
        });

        gbc.gridy++;
        contentPane.add(outputIntensityCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "",
                new JTextArea("Note: Detection for complex data\nis done without resampling."));

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

}