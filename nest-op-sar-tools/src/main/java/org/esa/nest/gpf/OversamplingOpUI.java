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
 * Operator UI for Oversampling
 */
public class OversamplingOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();

    private final JComboBox outputImageBy = new JComboBox(new String[] { UndersamplingOp.IMAGE_SIZE,
                                                                         UndersamplingOp.RATIO,
                                                                         UndersamplingOp.PIXEL_SPACING } );

    private final JTextField targetImageHeight = new JTextField("");
    private final JTextField targetImageWidth = new JTextField("");
    private final JTextField widthRatio = new JTextField("");
    private final JTextField heightRatio = new JTextField("");
    private final JTextField rangeSpacing = new JTextField("");
    private final JTextField azimuthSpacing = new JTextField("");

    private final JLabel targetImageHeightLabel = new JLabel("Rows:");
    private final JLabel targetImageWidthLabel = new JLabel("Columns:");
    private final JLabel widthRatioLabel = new JLabel("Width Ratio:");
    private final JLabel heightRatioLabel = new JLabel("Height Ratio:");
    private final JLabel rangeSpacingLabel = new JLabel("Range Spacing (m):");
    private final JLabel azimuthSpacingLabel = new JLabel("Azimuth Spacing (m):");
    private final JLabel outputImageByLabel = new JLabel("Output Image By:     ");

    private final JCheckBox usePRFTileSizeCheckBox = new JCheckBox("Use PRF Tile Size");
    private Boolean usePRFTileSize = false;

    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        outputImageBy.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateOutputImageBy(true);
            }
        });

        usePRFTileSizeCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    usePRFTileSize = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        return panel;
    }

    public void initParameters() {

        OperatorUIUtils.initBandList(bandList, getBandNames());

        outputImageBy.setSelectedItem(paramMap.get("outputImageBy"));
        targetImageHeight.setText(String.valueOf(paramMap.get("targetImageHeight")));
        targetImageWidth.setText(String.valueOf(paramMap.get("targetImageWidth")));
        widthRatio.setText(String.valueOf(paramMap.get("widthRatio")));
        heightRatio.setText(String.valueOf(paramMap.get("heightRatio")));
        rangeSpacing.setText(String.valueOf(paramMap.get("rangeSpacing")));
        azimuthSpacing.setText(String.valueOf(paramMap.get("azimuthSpacing")));

        usePRFTileSize = (Boolean)paramMap.get("usePRFTileSize");
        if(usePRFTileSize != null) {
            usePRFTileSizeCheckBox.setSelected(usePRFTileSize);
            usePRFTileSizeCheckBox.getModel().setPressed(usePRFTileSize);
        }

        updateOutputImageBy(true);
    }

    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    public void updateParameters() {

        OperatorUIUtils.updateBandList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("outputImageBy", outputImageBy.getSelectedItem());
        paramMap.put("targetImageHeight", Integer.parseInt(targetImageHeight.getText()));
        paramMap.put("targetImageWidth", Integer.parseInt(targetImageWidth.getText()));
        paramMap.put("widthRatio", Float.parseFloat(widthRatio.getText()));
        paramMap.put("heightRatio", Float.parseFloat(heightRatio.getText()));
        paramMap.put("rangeSpacing", Float.parseFloat(rangeSpacing.getText()));
        paramMap.put("azimuthSpacing", Float.parseFloat(azimuthSpacing.getText()));
        paramMap.put("usePRFTileSize", usePRFTileSize);
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, outputImageByLabel, outputImageBy);

        int savedY = ++gbc.gridy;
        DialogUtils.addComponent(contentPane, gbc, targetImageHeightLabel, targetImageHeight);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, targetImageWidthLabel, targetImageWidth);

        gbc.gridy = savedY;
        DialogUtils.addComponent(contentPane, gbc, widthRatioLabel, widthRatio);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, heightRatioLabel, heightRatio);

        gbc.gridy = savedY;
        DialogUtils.addComponent(contentPane, gbc, rangeSpacingLabel, rangeSpacing);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, azimuthSpacingLabel, azimuthSpacing);

        gbc.gridy++;
        contentPane.add(usePRFTileSizeCheckBox, gbc);
        DialogUtils.fillPanel(contentPane, gbc);
        
        return contentPane;
    }

    private void enableRowColumn(final boolean flag) {
        DialogUtils.enableComponents(targetImageWidthLabel, targetImageWidth, flag);
        DialogUtils.enableComponents(targetImageHeightLabel, targetImageHeight, flag);
    }

    private void enableRatio(final boolean flag) {
        DialogUtils.enableComponents(widthRatioLabel, widthRatio, flag);
        DialogUtils.enableComponents(heightRatioLabel, heightRatio, flag);
    }

    private void enablePixelSpacing(final boolean flag) {
        DialogUtils.enableComponents(rangeSpacingLabel, rangeSpacing, flag);
        DialogUtils.enableComponents(azimuthSpacingLabel, azimuthSpacing, flag);
    }

    private void updateOutputImageBy(final boolean show) {
        if(show) {
            outputImageBy.setVisible(true);
            outputImageByLabel.setVisible(true);

            final String item = (String)outputImageBy.getSelectedItem();
            if(item.equals(UndersamplingOp.IMAGE_SIZE)) {
                enableRowColumn(true);
                enableRatio(false);
                enablePixelSpacing(false);
            } else if(item.equals(UndersamplingOp.RATIO)){
                enableRowColumn(false);
                enableRatio(true);
                enablePixelSpacing(false);
            } else {
                enableRowColumn(false);
                enableRatio(false);
                enablePixelSpacing(true);
            }
        } else {
            outputImageBy.setVisible(false);
            outputImageByLabel.setVisible(false);
            enableRowColumn(false);
            enableRatio(false);
            enablePixelSpacing(false);
        }
    }
}