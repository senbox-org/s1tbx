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
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Feb 12, 2008
 * Time: 1:52:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class UndersamplingOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox method = new JComboBox(new String[] {       UndersamplingOp.SUB_SAMPLING,
                                                                        UndersamplingOp.KERNEL_FILTERING });

    private final JTextField subSamplingX = new JTextField("");
    private final JTextField subSamplingY = new JTextField("");
    /*
    private final JComboBox filterType = new JComboBox(new String[] {   UndersamplingOp.SUMMARY,
                                                                        UndersamplingOp.EDGE_DETECT,
                                                                        UndersamplingOp.EDGE_ENHANCEMENT,
                                                                        UndersamplingOp.LOW_PASS,
                                                                        UndersamplingOp.HIGH_PASS,
                                                                        UndersamplingOp.HORIZONTAL,
                                                                        UndersamplingOp.VERTICAL,
                                                                        UndersamplingOp.USER_DEFINED } );
    */
    private final JComboBox filterSize = new JComboBox(new String[] {   UndersamplingOp.FILTER_SIZE_3x3,
                                                                        UndersamplingOp.FILTER_SIZE_5x5,
                                                                        UndersamplingOp.FILTER_SIZE_7x7 } );

    private final JComboBox outputImageBy = new JComboBox(new String[] { UndersamplingOp.IMAGE_SIZE,
                                                                         UndersamplingOp.RATIO,
                                                                         UndersamplingOp.PIXEL_SPACING } );

    private final JTextField targetImageHeight = new JTextField("");
    private final JTextField targetImageWidth = new JTextField("");
    private final JTextField widthRatio = new JTextField("");
    private final JTextField heightRatio = new JTextField("");
    private final JTextField rangeSpacing = new JTextField("");
    private final JTextField azimuthSpacing = new JTextField("");

    private final JLabel subSamplingXLabel = new JLabel("Sub-Sampling in X:");
    private final JLabel subSamplingYLabel = new JLabel("Sub-Sampling in Y:");
//    private final JLabel filterTypeLabel = new JLabel("Filter Type:");
    private final JLabel filterSizeLabel = new JLabel("Filter Size:");
    private final JLabel targetImageHeightLabel = new JLabel("Rows:");
    private final JLabel targetImageWidthLabel = new JLabel("Columns:");
    private final JLabel widthRatioLabel = new JLabel("Width Ratio:");
    private final JLabel heightRatioLabel = new JLabel("Height Ratio:");
    private final JLabel rangeSpacingLabel = new JLabel("Range Spacing (m):");
    private final JLabel azimuthSpacingLabel = new JLabel("Azimuth Spacing (m):");
    private final JLabel outputImageByLabel = new JLabel("Output Image By:");

//    private final JLabel kernelFileLabel = new JLabel("Kernel File:");
//    private final JTextField kernelFile = new JTextField("");

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initParamList(bandList, getBandNames());

        method.setSelectedItem(paramMap.get("method"));
        subSamplingX.setText(String.valueOf(paramMap.get("subSamplingX")));
        subSamplingY.setText(String.valueOf(paramMap.get("subSamplingY")));

//        filterType.setSelectedItem(paramMap.get("filterType"));
        filterSize.setSelectedItem(paramMap.get("filterSize"));
//        File kFile = (File)paramMap.get("kernelFile");
//        if(kFile != null)
//            kernelFile.setText(kFile.getAbsolutePath());

        outputImageBy.setSelectedItem(paramMap.get("outputImageBy"));
        targetImageHeight.setText(String.valueOf(paramMap.get("targetImageHeight")));
        targetImageWidth.setText(String.valueOf(paramMap.get("targetImageWidth")));
        widthRatio.setText(String.valueOf(paramMap.get("widthRatio")));
        heightRatio.setText(String.valueOf(paramMap.get("heightRatio")));
        rangeSpacing.setText(String.valueOf(paramMap.get("rangeSpacing")));
        azimuthSpacing.setText(String.valueOf(paramMap.get("azimuthSpacing")));

        updateMethod();
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("method", method.getSelectedItem());
        paramMap.put("subSamplingX", Integer.parseInt(subSamplingX.getText()));
        paramMap.put("subSamplingY", Integer.parseInt(subSamplingY.getText()));

//        paramMap.put("filterType", filterType.getSelectedItem());
        paramMap.put("filterSize", filterSize.getSelectedItem());
//        paramMap.put("kernelFile", new File(kernelFile.getText()));

        paramMap.put("outputImageBy", outputImageBy.getSelectedItem());
        paramMap.put("targetImageHeight", Integer.parseInt(targetImageHeight.getText()));
        paramMap.put("targetImageWidth", Integer.parseInt(targetImageWidth.getText()));
        paramMap.put("widthRatio", Float.parseFloat(widthRatio.getText()));
        paramMap.put("heightRatio", Float.parseFloat(heightRatio.getText()));
        paramMap.put("rangeSpacing", Float.parseFloat(rangeSpacing.getText()));
        paramMap.put("azimuthSpacing", Float.parseFloat(azimuthSpacing.getText()));
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        DialogUtils.addComponent(contentPane, gbc, "Source Bands:", new JScrollPane(bandList));

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Under-Sampling Method:", method);
        method.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateMethod();
            }
        });

        int savedY = ++gbc.gridy;
        DialogUtils.addComponent(contentPane, gbc, subSamplingXLabel, subSamplingX);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, subSamplingYLabel, subSamplingY);

        gbc.gridy = savedY;
        DialogUtils.addComponent(contentPane, gbc, filterSizeLabel, filterSize);
        /*
        contentPane.add(filterTypeLabel, gbc);
        gbc.gridx = 1;
        contentPane.add(filterType, gbc);
        filterType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                String item = (String)filterType.getSelectedItem();
                if(item.equals(UndersamplingOp.USER_DEFINED)) {
                    DialogUtils.enableComponents(kernelFileLabel, kernelFile, true);
                    DialogUtils.enableComponents(filterSizeLabel, filterSize, false);
                } else {
                    DialogUtils.enableComponents(kernelFileLabel, kernelFile, false);
                    DialogUtils.enableComponents(filterSizeLabel, filterSize, true);
                }
            }
        });

        savedY = ++gbc.gridy;
        gbc.gridx = 0;
        */
        enableKernelFiltering(false);
        /*
        gbc.gridy = savedY;
        DialogUtils.addComponent(contentPane, gbc, kernelFileLabel, kernelFile);
        DialogUtils.enableComponents(kernelFileLabel, kernelFile, false);
        */
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, outputImageByLabel, outputImageBy);
        outputImageBy.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                updateOutputImageBy(true);
            }
        });

        savedY = ++gbc.gridy;
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

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableSubSampling(boolean flag) {
        DialogUtils.enableComponents(subSamplingXLabel, subSamplingX, flag);
        DialogUtils.enableComponents(subSamplingYLabel, subSamplingY, flag);
    }

    private void enableKernelFiltering(boolean flag) {
//        DialogUtils.enableComponents(filterTypeLabel, filterType, flag);
        DialogUtils.enableComponents(filterSizeLabel, filterSize, flag);
//        if(!flag)
//            DialogUtils.enableComponents(kernelFileLabel, kernelFile, flag);
    }

    private void enableRowColumn(boolean flag) {
        DialogUtils.enableComponents(targetImageWidthLabel, targetImageWidth, flag);
        DialogUtils.enableComponents(targetImageHeightLabel, targetImageHeight, flag);
    }

    private void enableRatio(boolean flag) {
        DialogUtils.enableComponents(widthRatioLabel, widthRatio, flag);
        DialogUtils.enableComponents(heightRatioLabel, heightRatio, flag);
    }

    private void enablePixelSpacing(boolean flag) {
        DialogUtils.enableComponents(rangeSpacingLabel, rangeSpacing, flag);
        DialogUtils.enableComponents(azimuthSpacingLabel, azimuthSpacing, flag);
    }

    private void updateMethod() {
        final String item = (String)method.getSelectedItem();
        if(item.equals(UndersamplingOp.SUB_SAMPLING)) {
            enableKernelFiltering(false);
            enableSubSampling(true);
            updateOutputImageBy(false);
        } else {
            enableKernelFiltering(true);
            enableSubSampling(false);
            updateOutputImageBy(true);
        }
    }

    private void updateOutputImageBy(boolean show) {
        if(show) {
            outputImageBy.setVisible(true);
            outputImageByLabel.setVisible(true);

            String item = (String)outputImageBy.getSelectedItem();
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