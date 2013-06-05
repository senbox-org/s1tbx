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

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dataio.dem.DEMFactory;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.eo.Constants;
import org.esa.nest.eo.MapProjectionHandler;
import org.esa.nest.eo.SARGeocoding;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Map;

/**
 * User interface for RangeDopplerGeocodingOp
 */
public class RangeDopplerGeocodingOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    private final JComboBox<String> demName = new JComboBox<String>(DEMFactory.getDEMNameList());

    private static final String externalDEMStr = "External DEM";

    private JComboBox<String> demResamplingMethod = new JComboBox<String>(DEMFactory.getDEMResamplingMethods());

    final JComboBox<String> imgResamplingMethod = new JComboBox<String>(ResamplingFactory.resamplingNames);

    final JComboBox incidenceAngleForGamma0 = new JComboBox<String>(new String[] {Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM,
            Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
            Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID});

    final JComboBox incidenceAngleForSigma0 = new JComboBox<String>(new String[] {Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM,
            Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
            Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID});

    final JComboBox auxFile = new JComboBox<String>(new String[] {CalibrationOp.LATEST_AUX,
                                                          CalibrationOp.PRODUCT_AUX,
                                                          CalibrationOp.EXTERNAL_AUX});

    final JTextField pixelSpacingInMeter = new JTextField("");
    final JTextField pixelSpacingInDegree = new JTextField("");
    private final JTextField externalDEMFile = new JTextField("");
    private final JTextField externalDEMNoDataValue = new JTextField("");
    private final JButton externalDEMBrowseButton = new JButton("...");
    private final JLabel externalDEMFileLabel = new JLabel("External DEM:");
    private final JLabel externalDEMNoDataValueLabel = new JLabel("DEM No Data Value:");
    final JLabel sourcePixelSpacingsLabelPart1 = new JLabel("Source GR Pixel Spacings (az x rg):");
    final JLabel sourcePixelSpacingsLabelPart2 = new JLabel("0.0(m) x 0.0(m)");

    final JCheckBox nodataValueAtSeaCheckBox = new JCheckBox("Mask out areas without elevation");
    final JCheckBox saveDEMCheckBox = new JCheckBox("Save DEM band");
    final JCheckBox saveLocalIncidenceAngleCheckBox = new JCheckBox("Save local incidence angle band");
    final JCheckBox saveProjectedLocalIncidenceAngleCheckBox = new JCheckBox("Save projected local incidence angle band");
    final JCheckBox saveSelectedSourceBandCheckBox = new JCheckBox("Save selected source band");
    final JCheckBox applyRadiometricNormalizationCheckBox = new JCheckBox("Apply radiometric normalization");
    final JCheckBox saveBetaNoughtCheckBox = new JCheckBox("Save Beta0 band");
    final JCheckBox saveGammaNoughtCheckBox = new JCheckBox("Save Gamma0 band");
    final JCheckBox saveSigmaNoughtCheckBox = new JCheckBox("Save Sigma0 band");

    final JLabel auxFileLabel = new JLabel("Auxiliary File (ASAR only):");
    final JLabel externalAuxFileLabel = new JLabel("External Aux File:");
    final JTextField externalAuxFile = new JTextField("");
    final JButton externalAuxFileBrowseButton = new JButton("...");

    private Boolean nodataValueAtSea = true;
    private Boolean saveDEM = false;
    private Boolean saveLocalIncidenceAngle = false;
    private Boolean saveProjectedLocalIncidenceAngle = false;
    private Boolean saveSelectedSourceBand = false;
    private Boolean applyRadiometricNormalization = false;
    private Boolean saveBetaNought = false;
    private Boolean saveGammaNought = false;
    private Boolean saveSigmaNought = false;
    private Double extNoDataValue = 0.0;
    private Double azimuthPixelSpacing = 0.0;
    private Double rangePixelSpacing = 0.0;
    private Double pixMSaved = 0.0;
    private Double pixDSaved = 0.0;

    private String savedProductName = null;

    protected boolean useAvgSceneHeight = false;
    protected final JButton crsButton = new JButton();
    private final MapProjectionHandler mapProjHandler = new MapProjectionHandler();

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        demName.addItem(externalDEMStr);

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = new JScrollPane(createPanel());
        initParameters();

        demName.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if(((String)demName.getSelectedItem()).startsWith(externalDEMStr)) {
                    enableExternalDEM(true);
                } else {
                    externalDEMFile.setText("");
                    enableExternalDEM(false);
                }
            }
        });
        externalDEMFile.setColumns(24);
        enableExternalDEM(((String)demName.getSelectedItem()).startsWith(externalDEMStr));

        auxFile.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String item = (String)auxFile.getSelectedItem();
                if(item.equals(CalibrationOp.EXTERNAL_AUX)) {
                    enableExternalAuxFile(true);
                } else {
                    externalAuxFile.setText("");
                    enableExternalAuxFile(false);
                }
            }
        });
        externalAuxFile.setColumns(24);
        final String auxFileParam = (String)parameterMap.get("auxFile");
        if(auxFileParam != null) {
            auxFile.setSelectedItem(auxFileParam);
        }
        enableExternalAuxFile(false);

        externalDEMBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File file = VisatApp.getApp().showFileOpenDialog("External DEM File", false, null);
                externalDEMFile.setText(file.getAbsolutePath());
                extNoDataValue = OperatorUIUtils.getNoDataValue(file);
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        });

        nodataValueAtSeaCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    nodataValueAtSea = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });
        saveDEMCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveDEM = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });
        saveLocalIncidenceAngleCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveLocalIncidenceAngle = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });
        saveProjectedLocalIncidenceAngleCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveProjectedLocalIncidenceAngle = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });
        saveSelectedSourceBandCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveSelectedSourceBand = (e.getStateChange() == ItemEvent.SELECTED);
                }
        });

        applyRadiometricNormalizationCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {

                    applyRadiometricNormalization = (e.getStateChange() == ItemEvent.SELECTED);
                    if (applyRadiometricNormalization) {

                        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
                        if (absRoot != null) {
                            final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
                            final int isCalibrated = absRoot.getAttributeInt(AbstractMetadata.abs_calibration_flag, 0);

                            // todo ALOS
                            // todo move this into calibrator.canRadiometricallyNormalize(mission)
                            if (isCalibrated==0 && (mission.equals("ENVISAT") || mission.contains("ERS") ||
                                mission.equals("RS2") || mission.contains("TSX") || mission.contains("TDX") ||
                                mission.contains("ALOS") || mission.contains("CSKS"))) {

                                enableRadiometricNormalization(true);

                                saveSigmaNoughtCheckBox.setSelected(saveSigmaNought);
                                saveGammaNoughtCheckBox.setSelected(saveGammaNought);
                                saveBetaNoughtCheckBox.setSelected(saveBetaNought);
                                saveSelectedSourceBandCheckBox.setSelected(false);

                            } else {

                                enableRadiometricNormalization(false);
                                saveSelectedSourceBandCheckBox.setSelected(true);
                            }
                        }

                    } else {
                        enableRadiometricNormalization(false);
                        saveSelectedSourceBandCheckBox.setSelected(true);
                    }
                }
        });
        saveBetaNoughtCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveBetaNought = (e.getStateChange() == ItemEvent.SELECTED);
                    if (saveBetaNought) {
                        saveSigmaNoughtCheckBox.setSelected(true);
                        saveProjectedLocalIncidenceAngleCheckBox.setSelected(true);
                    }
                }
        });
        saveGammaNoughtCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveGammaNought = (e.getStateChange() == ItemEvent.SELECTED);
                    if (saveGammaNought) {
                        saveSigmaNoughtCheckBox.setSelected(true);
                        saveProjectedLocalIncidenceAngleCheckBox.setSelected(true);
                    }
                }
        });
        saveSigmaNoughtCheckBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    saveSigmaNought = (e.getStateChange() == ItemEvent.SELECTED);
                    if (saveSigmaNought) {
                        if (incidenceAngleForSigma0.getSelectedItem().equals(Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM)) {
                            saveProjectedLocalIncidenceAngleCheckBox.setSelected(false);
                        } else {
                            saveProjectedLocalIncidenceAngleCheckBox.setSelected(true);
                        }
                    }
                }
        });

        externalAuxFileBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File file = VisatApp.getApp().showFileOpenDialog("External Aux File", false, null);
                externalAuxFile.setText(file.getAbsolutePath());
            }
        });

        crsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapProjHandler.promptForFeatureCrs(sourceProducts);
                crsButton.setText(mapProjHandler.getCRSName());
            }
        });

        return panel;
    }

    @Override
    public void initParameters() {
        OperatorUIUtils.initParamList(bandList, getBandNames());

        final String demNameParam = (String)paramMap.get("demName");
        if(demNameParam != null)
            demName.setSelectedItem(DEMFactory.appendAutoDEM(demNameParam));

        demResamplingMethod.setSelectedItem(paramMap.get("demResamplingMethod"));
        imgResamplingMethod.setSelectedItem(paramMap.get("imgResamplingMethod"));
        incidenceAngleForGamma0.setSelectedItem(paramMap.get("incidenceAngleForGamma0"));
        incidenceAngleForSigma0.setSelectedItem(paramMap.get("incidenceAngleForSigma0"));

        final String mapProjection = (String)paramMap.get("mapProjection");
        mapProjHandler.initParameters(mapProjection, sourceProducts);
        crsButton.setText(mapProjHandler.getCRSName());

        pixMSaved = (Double)paramMap.get("pixelSpacingInMeter");
        if(pixMSaved != null && pixMSaved != 0.0) {
            pixelSpacingInMeter.setText(String.valueOf(pixMSaved));
        }

        pixDSaved = (Double)paramMap.get("pixelSpacingInDegree");
        if(pixDSaved != null && pixDSaved != 0.0) {
            pixelSpacingInDegree.setText(String.valueOf(pixDSaved));
        }

        if (sourceProducts != null) {
            final String productName = sourceProducts[0].getName();
            if(!productName.equals(savedProductName)) {
                savedProductName = productName;

                try {
                    azimuthPixelSpacing = SARGeocoding.getAzimuthPixelSpacing(sourceProducts[0]);
                    rangePixelSpacing = SARGeocoding.getRangePixelSpacing(sourceProducts[0]);
                    azimuthPixelSpacing = (double)((int)(azimuthPixelSpacing*100 + 0.5))/100.0;
                    rangePixelSpacing = (double)((int)(rangePixelSpacing*100 + 0.5))/100.0;
                } catch (Exception e) {
                    azimuthPixelSpacing = 0.0;
                    rangePixelSpacing = 0.0;
                }
                final String text = Double.toString(azimuthPixelSpacing) + "(m) x " + Double.toString(rangePixelSpacing) + "(m)";
                sourcePixelSpacingsLabelPart2.setText(text);
            }

            if(pixDSaved == null || pixDSaved == 0.0) {
                Double pixM, pixD;
                try {
                    pixM = Math.max(azimuthPixelSpacing, rangePixelSpacing);
                    pixD = SARGeocoding.getPixelSpacingInDegree(pixM);
                } catch (Exception e) {
                    pixM = 0.0;
                    pixD = 0.0;
                }
                pixelSpacingInMeter.setText(String.valueOf(pixM));
                pixelSpacingInDegree.setText(String.valueOf(pixD));
                pixMSaved = pixM;
                pixDSaved = pixD;
            }
        }

        final File extDEMFile = (File)paramMap.get("externalDEMFile");
        if(extDEMFile != null) {
            externalDEMFile.setText(extDEMFile.getAbsolutePath());
            extNoDataValue =  (Double)paramMap.get("externalDEMNoDataValue");
            if(extNoDataValue != null)
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
        }

        nodataValueAtSea = (Boolean)paramMap.get("nodataValueAtSea");
        if(nodataValueAtSea != null) {
            nodataValueAtSeaCheckBox.setSelected(nodataValueAtSea);
        }

        saveDEM = (Boolean)paramMap.get("saveDEM");
        if(saveDEM != null) {
            saveDEMCheckBox.setSelected(saveDEM);
        }

        saveLocalIncidenceAngle = (Boolean)paramMap.get("saveLocalIncidenceAngle");
        if(saveLocalIncidenceAngle != null) {
            saveLocalIncidenceAngleCheckBox.setSelected(saveLocalIncidenceAngle);
        }

        saveProjectedLocalIncidenceAngle = (Boolean)paramMap.get("saveProjectedLocalIncidenceAngle");
        if(saveProjectedLocalIncidenceAngle != null) {
            saveProjectedLocalIncidenceAngleCheckBox.setSelected(saveProjectedLocalIncidenceAngle);
        }

        saveSelectedSourceBand = (Boolean)paramMap.get("saveSelectedSourceBand");
        if(saveSelectedSourceBand != null) {
            saveSelectedSourceBandCheckBox.setSelected(saveSelectedSourceBand);
        }

        applyRadiometricNormalization = (Boolean)paramMap.get("applyRadiometricNormalization");
        if(applyRadiometricNormalization != null) {
            applyRadiometricNormalizationCheckBox.setSelected(applyRadiometricNormalization);

            incidenceAngleForGamma0.setEnabled(applyRadiometricNormalization);
            incidenceAngleForSigma0.setEnabled(applyRadiometricNormalization);
            saveSigmaNoughtCheckBox.setEnabled(applyRadiometricNormalization);
            saveGammaNoughtCheckBox.setEnabled(applyRadiometricNormalization);
            saveBetaNoughtCheckBox.setEnabled(applyRadiometricNormalization);
        }

        saveBetaNought = (Boolean)paramMap.get("saveBetaNought");
        if(saveBetaNought != null) {
            saveBetaNoughtCheckBox.setSelected(saveBetaNought);
        }

        saveGammaNought = (Boolean)paramMap.get("saveGammaNought");
        if(saveGammaNought != null) {
            saveGammaNoughtCheckBox.setSelected(saveGammaNought);
        }

        saveSigmaNought = (Boolean)paramMap.get("saveSigmaNought");
        if(saveSigmaNought != null) {
            saveSigmaNoughtCheckBox.setSelected(saveSigmaNought);
        }

        if(sourceProducts != null) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            if (absRoot != null) {
                final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE);
                if (sampleType.equals("COMPLEX")) {
                    auxFile.removeItem(CalibrationOp.PRODUCT_AUX);
                } else if (auxFile.getItemCount() == 2) {
                    auxFile.addItem(CalibrationOp.PRODUCT_AUX);
                }
            }
        }
        final String auxFileStr = (String)paramMap.get("auxFile");
        if(auxFileStr != null) {
            auxFile.setSelectedItem(auxFileStr);
        }
        final File extAuxFile = (File)paramMap.get("externalAuxFile");
        if(extAuxFile != null) {
            externalAuxFile.setText(extAuxFile.getAbsolutePath());
        }
        if(applyRadiometricNormalization != null) {
            auxFile.setEnabled(applyRadiometricNormalization);
            auxFileLabel.setEnabled(applyRadiometricNormalization);
            externalAuxFile.setEnabled(applyRadiometricNormalization);
            externalAuxFileLabel.setEnabled(applyRadiometricNormalization);
            externalAuxFileBrowseButton.setEnabled(applyRadiometricNormalization);
        }
    }

    @Override
    public UIValidation validateParameters() {

        if (sourceProducts != null) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]);
            if(absRoot != null) {
                final boolean antElevCorrFlag = absRoot.getAttributeInt(AbstractMetadata.ant_elev_corr_flag) != 0;
                final boolean multilookFlag = absRoot.getAttributeInt(AbstractMetadata.multilook_flag) != 0;
                final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
                final boolean isPolsar = absRoot.getAttributeInt(AbstractMetadata.polsarData, 0) == 1;

                if(isPolsar && applyRadiometricNormalization) {
                    applyRadiometricNormalization = false;
                    return new UIValidation(UIValidation.State.WARNING, "Radiometric normalization is" +
                            " not available for polarimetric matrix products");
                }
                
                if ((mission.equals("ENVISAT") || mission.contains("ERS")) &&
                     applyRadiometricNormalization && antElevCorrFlag && multilookFlag) {
                    return new UIValidation(UIValidation.State.WARNING, "For multilooked products only" +
                            " constant and incidence angle corrections will be performed for radiometric normalization");
                }

                if (!mission.equals("RS2") && !mission.contains("TSX") && !mission.contains("TDX") && !mission.equals("ENVISAT") &&
                    !mission.contains("ERS") && !mission.contains("CSK") && !mission.contains("ALOS") && !mission.equals(" ") &&
                        applyRadiometricNormalization) {
                    applyRadiometricNormalization = false;
                    return new UIValidation(UIValidation.State.WARNING, "Radiometric normalization currently is" +
                            " not available for "+mission+" products");
                }
            }
        }
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateParamList(bandList, paramMap, OperatorUIUtils.SOURCE_BAND_NAMES);

        paramMap.put("demName", (DEMFactory.getProperDEMName((String)demName.getSelectedItem())));
        paramMap.put("demResamplingMethod", demResamplingMethod.getSelectedItem());
        paramMap.put("imgResamplingMethod", imgResamplingMethod.getSelectedItem());
        paramMap.put("incidenceAngleForGamma0", incidenceAngleForGamma0.getSelectedItem());
        paramMap.put("incidenceAngleForSigma0", incidenceAngleForSigma0.getSelectedItem());
        if(pixelSpacingInMeter.getText().isEmpty()) {
            paramMap.put("pixelSpacingInMeter", 0.0);
        } else {
            paramMap.put("pixelSpacingInMeter", Double.parseDouble(pixelSpacingInMeter.getText()));
        }

        if(pixelSpacingInDegree.getText().isEmpty()) {
            paramMap.put("pixelSpacingInDegree", 0.0);
        } else {
            paramMap.put("pixelSpacingInDegree", Double.parseDouble(pixelSpacingInDegree.getText()));
        }

        final String extFileStr = externalDEMFile.getText();
        if(!extFileStr.isEmpty()) {
            paramMap.put("externalDEMFile", new File(extFileStr));
            paramMap.put("externalDEMNoDataValue", Double.parseDouble(externalDEMNoDataValue.getText()));
        }

        if(mapProjHandler.getCRS() != null) {
            paramMap.put("mapProjection", mapProjHandler.getCRS().toWKT());
        }

        paramMap.put("nodataValueAtSea", nodataValueAtSea);
        paramMap.put("saveDEM", saveDEM);
        paramMap.put("saveLocalIncidenceAngle", saveLocalIncidenceAngle);
        paramMap.put("saveProjectedLocalIncidenceAngle", saveProjectedLocalIncidenceAngle);
        paramMap.put("saveSelectedSourceBand", saveSelectedSourceBand);
        paramMap.put("applyRadiometricNormalization", applyRadiometricNormalization);
        paramMap.put("saveBetaNought", saveBetaNought);
        paramMap.put("saveGammaNought", saveGammaNought);
        paramMap.put("saveSigmaNought", saveSigmaNought);

        paramMap.put("auxFile", auxFile.getSelectedItem());
        final String extAuxFileStr = externalAuxFile.getText();
        if(!extAuxFileStr.isEmpty()) {
            paramMap.put("externalAuxFile", new File(extAuxFileStr));
        }
    }

    JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        if(!useAvgSceneHeight) {
            DialogUtils.addComponent(contentPane, gbc, "Digital Elevation Model:", demName);
            gbc.gridy++;
            DialogUtils.addInnerPanel(contentPane, gbc, externalDEMFileLabel, externalDEMFile, externalDEMBrowseButton);

            gbc.gridy++;
            DialogUtils.addComponent(contentPane, gbc, externalDEMNoDataValueLabel, externalDEMNoDataValue);
            gbc.gridy++;
            DialogUtils.addComponent(contentPane, gbc, "DEM Resampling Method:", demResamplingMethod);
            gbc.gridy++;
        }
        DialogUtils.addComponent(contentPane, gbc, "Image Resampling Method:", imgResamplingMethod);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, sourcePixelSpacingsLabelPart1, sourcePixelSpacingsLabelPart2);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Pixel Spacing (m):", pixelSpacingInMeter);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Pixel Spacing (deg):", pixelSpacingInDegree);

        pixelSpacingInMeter.addFocusListener(new PixelSpacingMeterListener());
        pixelSpacingInDegree.addFocusListener(new PixelSpacingDegreeListener());

        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Map Projection:", crsButton);

        if(!useAvgSceneHeight) {
            gbc.gridx = 0;
            gbc.gridy++;
            contentPane.add(nodataValueAtSeaCheckBox, gbc);
            gbc.gridy++;
            contentPane.add(saveSelectedSourceBandCheckBox, gbc);
            gbc.gridx = 1;
            contentPane.add(saveDEMCheckBox, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            contentPane.add(saveLocalIncidenceAngleCheckBox, gbc);
            gbc.gridx = 1;
            contentPane.add(saveProjectedLocalIncidenceAngleCheckBox, gbc);
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
            gbc.insets.left = 0;
            DialogUtils.addComponent(contentPane, gbc, auxFileLabel, auxFile);
            gbc.gridy++;
            DialogUtils.addInnerPanel(contentPane, gbc, externalAuxFileLabel, externalAuxFile, externalAuxFileBrowseButton);
        }
        return contentPane;
    }

    private void enableExternalDEM(boolean flag) {
        DialogUtils.enableComponents(externalDEMFileLabel, externalDEMFile, flag);
        DialogUtils.enableComponents(externalDEMNoDataValueLabel, externalDEMNoDataValue, flag);
        externalDEMBrowseButton.setVisible(flag);
    }

    private void enableExternalAuxFile(boolean flag) {
        DialogUtils.enableComponents(externalAuxFileLabel, externalAuxFile, flag);
        externalAuxFileBrowseButton.setVisible(flag);
    }

    private void enableRadiometricNormalization(final boolean flag) {
        saveSigmaNoughtCheckBox.setSelected(flag);
        saveSigmaNoughtCheckBox.setEnabled(flag);
        saveGammaNoughtCheckBox.setEnabled(flag);
        saveBetaNoughtCheckBox.setEnabled(flag);
        incidenceAngleForSigma0.setEnabled(flag);
        incidenceAngleForGamma0.setEnabled(flag);
        auxFile.setEnabled(flag);
        auxFileLabel.setEnabled(flag);
        externalAuxFile.setEnabled(flag);
        externalAuxFileLabel.setEnabled(flag);
        externalAuxFileBrowseButton.setEnabled(flag);
    }

    protected class PixelSpacingMeterListener implements FocusListener {

        public void focusGained(final FocusEvent e) {
        }
        public void focusLost(final FocusEvent e) {
            Double pixM = 0.0, pixD = 0.0;
            try {
                pixM = Double.parseDouble(pixelSpacingInMeter.getText());
                if (pixM != pixMSaved) {
                    pixD = SARGeocoding.getPixelSpacingInDegree(pixM);
                    pixelSpacingInDegree.setText(String.valueOf(pixD));
                    pixMSaved = pixM;
                    pixDSaved = pixD;
                }
            } catch (Exception ec) {
                pixD = 0.0;
            }
        }
    }

    protected class PixelSpacingDegreeListener implements FocusListener {

        public void focusGained(final FocusEvent e) {
        }
        public void focusLost(final FocusEvent e) {
            Double pixM = 0.0, pixD = 0.0;
            try {
                pixD = Double.parseDouble(pixelSpacingInDegree.getText());
                if (pixD != pixDSaved) {
                    pixM = SARGeocoding.getPixelSpacingInMeter(pixD);
                    pixelSpacingInMeter.setText(String.valueOf(pixM));
                    pixMSaved = pixM;
                    pixDSaved = pixD;
                }
            } catch (Exception ec) {
                pixM = 0.0;
            }
        }
    }
}