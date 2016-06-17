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
package org.esa.s1tbx.insar.gpf.ui.coregistration;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

/**
 * User interface for OffsetTrackingOp2
 */
public class OffsetTrackingOp2UI extends BaseOperatorUI {

    // Output grid parameters
    private final JTextField gridAzimuthSpacing = new JTextField("");
    private final JTextField gridRangeSpacing = new JTextField("");
    private final JTextField gridAzimuthSpacingInMeters = new JTextField("");
    private final JTextField gridRangeSpacingInMeters = new JTextField("");
    private final JTextField gridAzimuthDimension = new JTextField("");
    private final JTextField gridRangeDimension = new JTextField("");
    private final JTextField totalGridPoints = new JTextField("");

    // Registration parameters
    private final JComboBox<String> registrationWindowWidth = new JComboBox(
            new String[]{"32", "64", "128", "256", "512", "1024", "2048"});
    private final JComboBox<String> registrationWindowHeight = new JComboBox(
            new String[]{"32", "64", "128", "256", "512", "1024", "2048"});
    private final JTextField xCorrThreshold = new JTextField("");

    // Post processing parameters
    private final JComboBox<String> averageBoxSize = new JComboBox(new String[]{"3", "5", "7", "9"});
    private final JTextField maxVelocity = new JTextField("");
    private final JTextField radius = new JTextField("");

    // Other option
    private final JComboBox resamplingType = new JComboBox(ResamplingFactory.resamplingNames);


    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        gridAzimuthSpacing.setText(String.valueOf(paramMap.get("gridAzimuthSpacing")));
        gridRangeSpacing.setText(String.valueOf(paramMap.get("gridRangeSpacing")));

        registrationWindowWidth.setSelectedItem(paramMap.get("registrationWindowWidth"));
        registrationWindowHeight.setSelectedItem(paramMap.get("registrationWindowHeight"));
        xCorrThreshold.setText(String.valueOf(paramMap.get("xCorrThreshold")));

        averageBoxSize.setSelectedItem(paramMap.get("averageBoxSize"));
        maxVelocity.setText(String.valueOf(paramMap.get("maxVelocity")));
        radius.setText(String.valueOf(paramMap.get("radius")));
        resamplingType.setSelectedItem(paramMap.get("resamplingType"));

        gridAzimuthSpacingInMeters.setText("");
        gridAzimuthSpacingInMeters.setEditable(false);
        gridRangeSpacingInMeters.setText("");
        gridRangeSpacingInMeters.setEditable(false);
        gridAzimuthDimension.setText("");
        gridAzimuthDimension.setEditable(false);
        gridRangeDimension.setText("");
        gridRangeDimension.setEditable(false);
        totalGridPoints.setText("");
        totalGridPoints.setEditable(false);

        if (sourceProducts != null && sourceProducts.length > 0) {
            setDerivedAzimuthParameters();
            setDerivedRangeParameters();
        }
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("gridAzimuthSpacing", Integer.parseInt(gridAzimuthSpacing.getText()));
        paramMap.put("gridRangeSpacing", Integer.parseInt(gridRangeSpacing.getText()));

        paramMap.put("registrationWindowWidth", registrationWindowWidth.getSelectedItem());
        paramMap.put("registrationWindowHeight", registrationWindowHeight.getSelectedItem());
        paramMap.put("xCorrThreshold", Double.parseDouble(xCorrThreshold.getText()));

        paramMap.put("averageBoxSize", averageBoxSize.getSelectedItem());
        paramMap.put("maxVelocity", Double.parseDouble(maxVelocity.getText()));
        paramMap.put("radius", Integer.parseInt(radius.getText()));
        paramMap.put("resamplingType", resamplingType.getSelectedItem());

        setDerivedAzimuthParameters();
        setDerivedRangeParameters();
    }

    private JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel(" "), gbc);
        gbc.gridy++;

        final JPanel gridPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc2 = DialogUtils.createGridBagConstraints();
        gridPanel.setBorder(BorderFactory.createTitledBorder("Output Grid"));

        DialogUtils.addComponent(gridPanel, gbc2, "Grid Azimuth Spacing (in pixels):", gridAzimuthSpacing);
        gbc2.gridy++;
        DialogUtils.addComponent(gridPanel, gbc2, "Grid Range Spacing (in pixels):", gridRangeSpacing);
        gbc2.gridy++;
        DialogUtils.addComponent(gridPanel, gbc2, "Grid Azimuth Spacing (in meters):", gridAzimuthSpacingInMeters);
        gbc2.gridy++;
        DialogUtils.addComponent(gridPanel, gbc2, "Grid Range Spacing (in meters):", gridRangeSpacingInMeters);
        gbc2.gridy++;
        DialogUtils.addComponent(gridPanel, gbc2, "Grid Azimuth Dimension:", gridAzimuthDimension);
        gbc2.gridy++;
        DialogUtils.addComponent(gridPanel, gbc2, "Grid Range Dimension:", gridRangeDimension);
        gbc2.gridy++;
        DialogUtils.addComponent(gridPanel, gbc2, "Total Grid Points:", totalGridPoints);

        gridAzimuthSpacing.setDocument(new gridAzimuthSpacingDocument());
        gridRangeSpacing.setDocument(new gridRangeSpacingDocument());

        final JPanel registrationPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc3 = DialogUtils.createGridBagConstraints();
        registrationPanel.setBorder(BorderFactory.createTitledBorder("Registration"));

        DialogUtils.addComponent(registrationPanel, gbc3, "Registration Window Width:", registrationWindowWidth);
        gbc3.gridy++;
        DialogUtils.addComponent(registrationPanel, gbc3, "Registration Window Height:", registrationWindowHeight);
        gbc3.gridy++;
        DialogUtils.addComponent(registrationPanel, gbc3, "Cross-Correlation Threshold:", xCorrThreshold);
        gbc3.gridy++;
        DialogUtils.addComponent(registrationPanel, gbc3, "Average Box Size:", averageBoxSize);
        gbc3.gridy++;
        DialogUtils.addComponent(registrationPanel, gbc3, "Max Velocity (m/day):", maxVelocity);
        gbc3.gridy++;
        DialogUtils.addComponent(registrationPanel, gbc3, "Radius for Hole Filling:", radius);
        gbc3.gridy++;

        contentPane.add(gridPanel, gbc);
        gbc.gridx = 1;
        contentPane.add(registrationPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        DialogUtils.addComponent(contentPane, gbc, "Resampling Type:", resamplingType);
        gbc.gridy++;

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private synchronized void setDerivedAzimuthParameters() {

        if (sourceProducts != null && sourceProducts.length > 0) {
            try {
                final int sourceImageWidth = sourceProducts[0].getSceneRasterWidth();
                final int sourceImageHeight = sourceProducts[0].getSceneRasterHeight();
                final int gridRgSpacingInPixel = Integer.parseInt(gridRangeSpacing.getText());
                final int gridAzSpacingInPixel = Integer.parseInt(gridAzimuthSpacing.getText());

                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
                double azPixelSpacing = 0.0;
                if (absRoot != null) {
                    azPixelSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.azimuth_spacing);
                }

                if (azPixelSpacing != 0.0) {
                    final double azSpacingInM = azPixelSpacing * gridAzSpacingInPixel;
                    gridAzimuthSpacingInMeters.setText(String.valueOf(azSpacingInM));
                }

                final int azimuthDimension = sourceImageHeight / gridAzSpacingInPixel;
                gridAzimuthDimension.setText(String.valueOf(azimuthDimension));

                final int rangeDimension = sourceImageWidth / gridRgSpacingInPixel;
                final int totalPoints = rangeDimension * azimuthDimension;
                totalGridPoints.setText(String.valueOf(totalPoints));

            } catch (Exception e) {
                gridAzimuthSpacingInMeters.setText("");
                gridAzimuthDimension.setText("");
            }
        }
    }

    private synchronized void setDerivedRangeParameters() {

        if (sourceProducts != null && sourceProducts.length > 0) {
            try {
                final int sourceImageWidth = sourceProducts[0].getSceneRasterWidth();
                final int sourceImageHeight = sourceProducts[0].getSceneRasterHeight();
                final int gridRgSpacingInPixel = Integer.parseInt(gridRangeSpacing.getText());
                final int gridAzSpacingInPixel = Integer.parseInt(gridAzimuthSpacing.getText());

                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
                double rgPixelSpacing = 0.0;
                if (absRoot != null) {
                    rgPixelSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);
                }

                if (rgPixelSpacing != 0.0) {
                    final double rgSpacingInM = rgPixelSpacing * gridRgSpacingInPixel;
                    gridRangeSpacingInMeters.setText(String.valueOf(rgSpacingInM));
                }

                final int rangeDimension = sourceImageWidth / gridRgSpacingInPixel;
                gridRangeDimension.setText(String.valueOf(rangeDimension));

                final int azimuthDimension = sourceImageHeight / gridAzSpacingInPixel;
                final int totalPoints = rangeDimension * azimuthDimension;
                totalGridPoints.setText(String.valueOf(totalPoints));

            } catch (Exception e) {
                gridRangeSpacingInMeters.setText("");
                gridRangeDimension.setText("");
            }
        }
    }

    private class gridAzimuthSpacingDocument extends PlainDocument {

        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(offset, length, text, attrs);

            setDerivedAzimuthParameters();
        }
    }

    private class gridRangeSpacingDocument extends PlainDocument {

        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(offset, length, text, attrs);

            setDerivedRangeParameters();
        }
    }
}
