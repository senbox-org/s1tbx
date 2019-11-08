/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.gpf.ui;

import org.csa.rstb.soilmoisture.gpf.IEMInverBase;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.SnapFileChooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Map;

/**
 * User interface for IEM operators
 */
public class IEMInverOpUI extends BaseOperatorUI {

    private final JLabel polarisationLabel = new JLabel("Polarisation:");
    private final JComboBox polarizationsComboBox = new JComboBox<>(
            new String[]{"HH1-HH2", "HH1-VV2", "VV1-VV2", "VV1-HH2"});

    private final JCheckBox outputRMSCheckBox = new JCheckBox("Output rms");
    private final JCheckBox outputCLCheckBox = new JCheckBox("Output correlation length");

    private final JComboBox lutFileComboBox = new JComboBox<String>();
    private final JButton fileChooserButton = new JButton(new FileChooserAction());
    private final JTextField numClosestSigmas = new JTextField("# closest matches from k-d tree search (N)");
    private final JTextField numRDCNeighbours = new JTextField("Length (pixels) of side of square neighbourhood (M)");
    private final JTextField thresholdRDC = new JTextField("RDC deviation threshold");
    private final JCheckBox filterCheckBox = new JCheckBox("Filter remaining outliers with mean");
    private File lutFolder;

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        final Dimension size = new Dimension(26, 16);
        fileChooserButton.setPreferredSize(size);
        fileChooserButton.setMinimumSize(size);

        populateLUTs(operatorName);

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();

        return panel;
    }

    private void populateLUTs(final String operatorName) {
        lutFolder = IEMInverBase.initializeLUTFolder();
        final File[] files = lutFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && (file.getName().endsWith(IEMInverBase.MAT_FILE_EXTENSION) ||
                        file.getName().endsWith(IEMInverBase.CSV_FILE_EXTENSION));
            }
        });
        if (files != null) {
            for (File file : files) {
                String name = file.getName().toUpperCase();
                if (operatorName.equals("IEM-Hybrid-Inversion")) {
                    if (name.startsWith("IEM_"))
                        lutFileComboBox.addItem(file.getAbsolutePath());
                } else {
                    if (name.startsWith("IEMC_"))
                        lutFileComboBox.addItem(file.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public void initParameters() {

        final File lutFile = (File) paramMap.get("lutFile");
        if (lutFile != null) {
            lutFileComboBox.setSelectedItem(lutFile.getAbsolutePath());
        }

        //check if parameters have polarisation
        final Object sigmaPol = paramMap.get("sigmaPol");
        if (sigmaPol != null) {
            final String selectedPol = (String) sigmaPol;
            polarizationsComboBox.setSelectedItem(selectedPol);
            showPolarisation(true);
        } else {
            showPolarisation(false);
        }

        final Boolean outputRMS = ((Boolean) paramMap.get("outputRMS"));
        if (outputRMS != null) {
            outputRMSCheckBox.setSelected(outputRMS);
        }

        // check if parameters have output cl
        final Boolean outputCL = (Boolean) paramMap.get("outputCL");
        if (outputCL != null) {
            outputCLCheckBox.setSelected(outputCL);
            outputCLCheckBox.setVisible(true);
        } else {
            outputCLCheckBox.setVisible(false);
        }

        // numClosestSigmas
        Integer numClosestSigmasVal = (Integer) paramMap.get("N");
        if (numClosestSigmasVal == null) {
            numClosestSigmasVal = 1;
        }
        numClosestSigmas.setText(String.valueOf(numClosestSigmasVal));

        // numRDCNeighbours
        Integer numRDCNeighboursVal = (Integer) paramMap.get("M");
        if (numRDCNeighboursVal == null) {
            numRDCNeighboursVal = 3;
        }
        numRDCNeighbours.setText(String.valueOf(numRDCNeighboursVal));

        // thresholdRDC
        Double thresholdRDCVal = (Double) paramMap.get("thresholdRDC");
        if (thresholdRDCVal == null) {
            thresholdRDCVal = 100.0;
        }
        thresholdRDC.setText(String.valueOf(thresholdRDCVal));

        // filterCheckBox
        Boolean doFilterVal = (Boolean) paramMap.get("doRemainingOutliersFilter");
        if (doFilterVal != null) {
            filterCheckBox.setSelected(doFilterVal);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        final String lutFileStr = (String) lutFileComboBox.getSelectedItem();
        if (lutFileStr != null && !lutFileStr.isEmpty()) {
            paramMap.put("lutFile", new File(lutFileStr));
        }
        if (polarizationsComboBox.isVisible()) {
            paramMap.put("sigmaPol", polarizationsComboBox.getSelectedItem());
        }

        paramMap.put("outputRMS", outputRMSCheckBox.isSelected());

        if (outputCLCheckBox.isVisible()) {
            paramMap.put("outputCL", outputCLCheckBox.isSelected());
        }

        paramMap.put("N", Integer.parseInt(numClosestSigmas.getText()));
        paramMap.put("M", Integer.parseInt(numRDCNeighbours.getText()));
        paramMap.put("thresholdRDC", Double.parseDouble(thresholdRDC.getText()));
        paramMap.put("doRemainingOutliersFilter", filterCheckBox.isSelected());
    }

    private void showPolarisation(final boolean flag) {
        polarisationLabel.setVisible(flag);
        polarizationsComboBox.setVisible(flag);
    }

    private JComponent createPanel() {

        final JPanel lutSubPanel = new JPanel(new BorderLayout(3, 3));
        lutSubPanel.add(lutFileComboBox, BorderLayout.CENTER);
        lutSubPanel.add(fileChooserButton, BorderLayout.EAST);

        final JPanel content = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();
        DialogUtils.addComponent(content, gbc, "IEM LUT:", lutSubPanel);
        gbc.gridy++;
        DialogUtils.addComponent(content, gbc, polarisationLabel, polarizationsComboBox);

        gbc.gridy++;
        gbc.gridx = 1;
        content.add(outputRMSCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        content.add(outputCLCheckBox, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        DialogUtils.addComponent(content, gbc, "# closest matches from k-d tree search (N):", numClosestSigmas);

        gbc.gridy++;
        gbc.gridx = 1;
        DialogUtils.addComponent(content, gbc, "Length (pixels) of side of square neighbourhood (M):", numRDCNeighbours);

        gbc.gridy++;
        gbc.gridx = 1;
        DialogUtils.addComponent(content, gbc, "RDC deviation threshold:", thresholdRDC);

        gbc.gridy++;
        gbc.gridx = 1;
        content.add(filterCheckBox, gbc);
        filterCheckBox.setToolTipText("Filter remaining outliers with mean of neighbouring RDC");

        DialogUtils.fillPanel(content, gbc);

        return content;
    }

    private class FileChooserAction extends AbstractAction {

        private String APPROVE_BUTTON_TEXT = "Select";
        private JFileChooser chooser;

        private FileChooserAction() {
            super("...");
            chooser = new SnapFileChooser();
            chooser.setDialogTitle("Find File");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final Window window = SwingUtilities.getWindowAncestor((JComponent) event.getSource());
            chooser.setCurrentDirectory(lutFolder);

            if (chooser.showDialog(window, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
                final File file = chooser.getSelectedFile();

                lutFileComboBox.addItem(file.getAbsolutePath());
                lutFileComboBox.setSelectedItem(file.getAbsolutePath());
            }
        }
    }
}