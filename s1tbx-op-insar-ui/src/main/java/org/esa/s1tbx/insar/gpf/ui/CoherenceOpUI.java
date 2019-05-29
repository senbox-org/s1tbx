/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.gpf.ui;

import org.esa.s1tbx.insar.gpf.CoherenceOp;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.dem.dataio.DEMFactory;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.OperatorUIUtils;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Map;

/**
 * User interface for CreateCoherenceImageOp
 */
public class CoherenceOpUI extends BaseOperatorUI {

    private final JCheckBox singleMasterCheckBox = new JCheckBox("Single Master");
    private final JCheckBox squarePixelCheckBox = new JCheckBox("Square Pixel");
    private final JCheckBox independentWindowSizeCheckBox = new JCheckBox("Independent Window Sizes");
    private final JCheckBox subtractFlatEarthPhaseCheckBox = new JCheckBox("Subtract flat-earth phase");
    private final JCheckBox subtractTopographicPhaseCheckBox = new JCheckBox("Subtract topographic phase");

    private final JTextField cohWinAz = new JTextField("");
    private final JTextField cohWinRg = new JTextField("");

    private final JComboBox<Integer> srpPolynomialDegreeStr = new JComboBox(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8});
    private final JComboBox<Integer> srpNumberPointsStr = new JComboBox(new Integer[]{301, 401, 501, 601, 701, 801, 901, 1001});
    private final JComboBox<Integer> orbitDegreeStr = new JComboBox(new Integer[]{1, 2, 3, 4, 5});

    private static final JLabel cohWinAzLabel = new JLabel("Coherence Azimuth Window Size");
    private static final JLabel cohWinRgLabel = new JLabel("Coherence Range Window Size");
    private static final JLabel srpPolynomialDegreeStrLabel = new JLabel("Degree of \"Flat Earth\" polynomial");
    private static final JLabel srpNumberPointsStrLabel = new JLabel("Number of \"Flat Earth\" estimation points");
    private static final JLabel orbitDegreeStrLabel = new JLabel("Orbit interpolation degree");

    private Boolean squarePixel = true;
    private Boolean subtractFlatEarthPhase = false;
    private Boolean singleMaster = true;
    private final CoherenceOp.DerivedParams param = new CoherenceOp.DerivedParams();

    private Boolean subtractTopographicPhase = false;
    private static final String[] demValueSet = DEMFactory.getDEMNameList();
    //    private final JTextField orbitDegree = new JTextField("");
    private final JComboBox<String> demName = new JComboBox<>(demValueSet);
    private static final String externalDEMStr = "External DEM";
    private final JTextField externalDEMFile = new JTextField("");
    private final JTextField externalDEMNoDataValue = new JTextField("");
    private final JButton externalDEMBrowseButton = new JButton("...");
    private final JLabel externalDEMFileLabel = new JLabel("External DEM:");
    private final JLabel externalDEMNoDataValueLabel = new JLabel("DEM No Data Value:");
    private final JCheckBox externalDEMApplyEGMCheckBox = new JCheckBox("Apply Earth Gravitational Model");
    private final DialogUtils.TextAreaKeyListener textAreaKeyListener = new DialogUtils.TextAreaKeyListener();
    private final JComboBox<String> tileExtensionPercent = new JComboBox<>(new String[]{"20", "40", "60", "80", "100", "150", "200"});
    private Double extNoDataValue = 0.0;
    private Boolean externalDEMApplyEGM = true;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final JComponent panel = new JScrollPane(createPanel());
        initParameters();

        subtractFlatEarthPhaseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                subtractFlatEarthPhase = (e.getStateChange() == ItemEvent.SELECTED);
                enableSubtractFlatEarthPhaseParameters(subtractFlatEarthPhase);
            }
        });

        squarePixelCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                squarePixel = (e.getStateChange() == ItemEvent.SELECTED);
                independentWindowSizeCheckBox.setSelected(!squarePixel);
                if (squarePixel) {
                    cohWinAz.setText("2");
                    cohWinAz.setEditable(false);
                }
                setCohWinAz();
                setCohWinRg();
            }
        });

        independentWindowSizeCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                squarePixel = (e.getStateChange() != ItemEvent.SELECTED);
                squarePixelCheckBox.setSelected(squarePixel);
                if (!squarePixel) {
                    cohWinAz.setEditable(true);
                }
                setCohWinAz();
                setCohWinRg();
            }
        });

        subtractTopographicPhaseCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                subtractTopographicPhase = (e.getStateChange() == ItemEvent.SELECTED);
                enableSubtractTopographicPhaseParameters(subtractTopographicPhase);
            }
        });

        singleMasterCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                singleMaster = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        demName.addItem(externalDEMStr);

        demName.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String item = ((String) demName.getSelectedItem()).replace(DEMFactory.AUTODEM, "");
                if (item.equals(externalDEMStr)) {
                    enableExternalDEM(true);
                } else {
                    externalDEMFile.setText("");
                    enableExternalDEM(false);
                }
            }
        });
        externalDEMFile.setColumns(30);
        enableExternalDEM(((String) demName.getSelectedItem()).startsWith(externalDEMStr));

        externalDEMBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final File file = Dialogs.requestFileForOpen("External DEM File", false, null, DEMFactory.LAST_EXTERNAL_DEM_DIR_KEY);
                if (file != null) {
                    externalDEMFile.setText(file.getAbsolutePath());
                    extNoDataValue = OperatorUIUtils.getNoDataValue(file);
                }
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }
        });

        externalDEMApplyEGMCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                externalDEMApplyEGM = (e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        externalDEMNoDataValue.addKeyListener(textAreaKeyListener);

        return panel;
    }

    @Override
    public void initParameters() {

        Boolean paramVal;
        paramVal = (Boolean) paramMap.get("subtractFlatEarthPhase");
        if (paramVal != null) {
            subtractFlatEarthPhase = paramVal;
            subtractFlatEarthPhaseCheckBox.setSelected(subtractFlatEarthPhase);
            enableSubtractFlatEarthPhaseParameters(subtractFlatEarthPhase);
        }

        srpPolynomialDegreeStr.setSelectedItem(paramMap.get("srpPolynomialDegree"));
        srpNumberPointsStr.setSelectedItem(paramMap.get("srpNumberPoints"));
        orbitDegreeStr.setSelectedItem(paramMap.get("orbitDegree"));

        if(sourceProducts != null && sourceProducts.length > 0) {
            boolean isComplex = AbstractMetadata.getAbstractedMetadata(sourceProducts[0]).
                    getAttributeString(AbstractMetadata.SAMPLE_TYPE).contains("COMPLEX");

            enableControls(isComplex);
        }

        paramVal = (Boolean) paramMap.get("subtractTopographicPhase");
        if (paramVal != null) {
            subtractTopographicPhase = paramVal;
            subtractTopographicPhaseCheckBox.setSelected(subtractTopographicPhase);
            enableSubtractTopographicPhaseParameters(subtractTopographicPhase);
        }

        paramVal = (Boolean) paramMap.get("singleMaster");
        if (paramVal != null) {
            singleMaster = paramVal;
            singleMasterCheckBox.setSelected(singleMaster);
        }

//        orbitDegree.setText(String.valueOf(paramMap.get("orbitDegree")));
        final String demNameParam = (String) paramMap.get("demName");
        if (demNameParam != null) {
            ElevationModelDescriptor descriptor = ElevationModelRegistry.getInstance().getDescriptor(demNameParam);
            if(descriptor != null) {
                demName.setSelectedItem(DEMFactory.getDEMDisplayName(descriptor));
            } else {
                demName.setSelectedItem(demNameParam);
            }
        }

        final File extFile = (File)paramMap.get("externalDEMFile");
        if(extFile != null) {
            externalDEMFile.setText(extFile.getAbsolutePath());
            extNoDataValue =  (Double)paramMap.get("externalDEMNoDataValue");
            if(extNoDataValue != null && !textAreaKeyListener.isChangedByUser()) {
                externalDEMNoDataValue.setText(String.valueOf(extNoDataValue));
            }

            paramVal = (Boolean) paramMap.get("externalDEMApplyEGM");
            if (paramVal != null) {
                externalDEMApplyEGM = paramVal;
                externalDEMApplyEGMCheckBox.setSelected(externalDEMApplyEGM);
            }
        }

        tileExtensionPercent.setSelectedItem(paramMap.get("tileExtensionPercent"));

        cohWinAz.setText(String.valueOf(paramMap.get("cohWinAz")));
        cohWinRg.setText(String.valueOf(paramMap.get("cohWinRg")));

        squarePixel = (Boolean) paramMap.get("squarePixel");
        if (squarePixel != null) {
            squarePixelCheckBox.setSelected(squarePixel);
            independentWindowSizeCheckBox.setSelected(!squarePixel);
            if (squarePixel) {
                cohWinAz.setText("2");
                cohWinAz.setEditable(false);
            } else {
                cohWinAz.setEditable(true);
            }
        }

        setCohWinAz();
        setCohWinRg();
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("subtractFlatEarthPhase", subtractFlatEarthPhase);

        if (subtractFlatEarthPhase) {
			paramMap.put("srpPolynomialDegree", srpPolynomialDegreeStr.getSelectedItem());
            paramMap.put("srpNumberPoints", srpNumberPointsStr.getSelectedItem());
			paramMap.put("orbitDegree", orbitDegreeStr.getSelectedItem());
		}

        paramMap.put("singleMaster", singleMaster);

        paramMap.put("subtractTopographicPhase", subtractTopographicPhase);
        if (subtractTopographicPhase) {
//          paramMap.put("orbitDegree", Integer.parseInt(orbitDegree.getText()));
            final String properDEMName = (DEMFactory.getProperDEMName((String) demName.getSelectedItem()));
            paramMap.put("demName", (DEMFactory.getProperDEMName((String) demName.getSelectedItem())));
            if(properDEMName.equals(externalDEMStr)) {
                final String extFileStr = externalDEMFile.getText();
                paramMap.put("externalDEMFile", new File(extFileStr));
                paramMap.put("externalDEMNoDataValue", Double.parseDouble(externalDEMNoDataValue.getText()));
                paramMap.put("externalDEMApplyEGM", externalDEMApplyEGM);
            }
            paramMap.put("tileExtensionPercent", tileExtensionPercent.getSelectedItem());
        }

        final String cohWinRgStr = cohWinRg.getText();
        final String cohWinAzStr = cohWinAz.getText();
        if (cohWinRgStr != null && !cohWinRgStr.isEmpty())
            paramMap.put("cohWinRg", Integer.parseInt(cohWinRg.getText()));

        if (cohWinAzStr != null && !cohWinAzStr.isEmpty())
            paramMap.put("cohWinAz", Integer.parseInt(cohWinAz.getText()));

        paramMap.put("squarePixel", squarePixel);
    }

    JComponent createPanel() {

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(singleMasterCheckBox, gbc);

        gbc.gridy++;
        contentPane.add(subtractFlatEarthPhaseCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, srpPolynomialDegreeStrLabel, srpPolynomialDegreeStr);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, srpNumberPointsStrLabel, srpNumberPointsStr);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, orbitDegreeStrLabel, orbitDegreeStr);

        srpPolynomialDegreeStr.setEnabled(false);
        srpNumberPointsStr.setEnabled(false);
        orbitDegreeStr.setEnabled(false);

        gbc.gridy++;
        contentPane.add(subtractTopographicPhaseCheckBox, gbc);

//        gbc.gridy++;
//        DialogUtils.addComponent(contentPane, gbc, "Orbit Interpolation Degree:", orbitDegree);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Digital Elevation Model:", demName);
        gbc.gridy++;
        DialogUtils.addInnerPanel(contentPane, gbc, externalDEMFileLabel, externalDEMFile, externalDEMBrowseButton);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, externalDEMNoDataValueLabel, externalDEMNoDataValue);
        gbc.gridy++;
        gbc.gridx = 1;
        contentPane.add(externalDEMApplyEGMCheckBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = gbc.gridy + 10;
        DialogUtils.addComponent(contentPane, gbc, "Tile Extension [%]", tileExtensionPercent);

        gbc.gridx = 0;
        gbc.gridy++;
        contentPane.add(squarePixelCheckBox, gbc);

        gbc.gridx = 1;
        contentPane.add(independentWindowSizeCheckBox, gbc);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWinRgLabel, cohWinRg);

        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, cohWinAzLabel, cohWinAz);
        cohWinAz.setEditable(false);
        cohWinRg.setDocument(new CohWinRgDocument());

        demName.setEnabled(false);
        tileExtensionPercent.setEnabled(false);

        DialogUtils.fillPanel(contentPane, gbc);

        return contentPane;
    }

    private void enableControls(final boolean flag) {
        subtractFlatEarthPhaseCheckBox.setEnabled(flag);
        subtractTopographicPhaseCheckBox.setEnabled(flag);
    }

    private synchronized void setCohWinAz() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            try {
                if (squarePixelCheckBox.isSelected()) {
                    param.cohWinRg = Integer.parseInt(cohWinRg.getText());
                    CoherenceOp.getDerivedParameters(sourceProducts[0], param);
                    cohWinAz.setText(String.valueOf(param.cohWinAz));
                }
            } catch (Exception e) {
            }
        }
    }

    private void setCohWinRg() {
        if (sourceProducts != null && sourceProducts.length > 0) {
            if (squarePixelCheckBox.isSelected()) {
                cohWinRg.setText(String.valueOf(param.cohWinRg));
            }
        }
    }

    @SuppressWarnings("serial")
    private class CohWinRgDocument extends PlainDocument {

        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(offset, length, text, attrs);

            setCohWinAz();
        }
    }

    private void enableExternalDEM(boolean flag) {
        DialogUtils.enableComponents(externalDEMFileLabel, externalDEMFile, flag);
        DialogUtils.enableComponents(externalDEMNoDataValueLabel, externalDEMNoDataValue, flag);
        if(!flag) {
            externalDEMFile.setText("");
        }
        externalDEMBrowseButton.setVisible(flag);
        externalDEMApplyEGMCheckBox.setVisible(flag);
    }

    private void enableSubtractFlatEarthPhaseParameters(boolean flag) {
        srpPolynomialDegreeStr.setEnabled(flag);
        srpNumberPointsStr.setEnabled(flag);
        orbitDegreeStr.setEnabled(flag);
    }

    private void enableSubtractTopographicPhaseParameters(boolean flag) {
        demName.setEnabled(flag);
        tileExtensionPercent.setEnabled(flag);
    }
}
