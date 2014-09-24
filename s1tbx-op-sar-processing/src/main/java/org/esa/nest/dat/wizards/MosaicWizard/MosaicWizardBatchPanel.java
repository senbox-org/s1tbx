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
package org.esa.nest.dat.wizards.MosaicWizard;

import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.wizards.WizardPanel;
import org.esa.snap.dat.dialogs.BatchGraphDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Processing
 */
public class MosaicWizardBatchPanel extends WizardPanel {

    private static final File graphFile = new File(wizardGraphPath, "Cal_ML_TC.xml");

    private BatchGraphDialog batchDlg;

    public MosaicWizardBatchPanel(final File[] productFileList, final File targetFolder) {
        super("Batch Process Terrain Correction");

        createPanel();

        batchProcess(productFileList, targetFolder, graphFile);
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean hasNextPanel() {
        if (batchDlg != null) {
            return !batchDlg.isProcessing();
        }
        return true;
    }

    public boolean canFinish() {
        return false;
    }

    public WizardPanel getNextPanel() {
        return new MosaicWizardMosaicPanel(getTargetFileList());
    }

    public boolean validateInput() {
        if (getTargetFileList().length == 0) {
            batchDlg.onApply();
            getOwner().updateState();
        }
        return getTargetFileList().length != 0;
    }

    private void createPanel() {

        final JPanel textPanel = createTextPanel("Instructions",
                "The Batch Processing dialog will now load the Cal_ML_TCGraph.xml\n" +
                        "and apply Calibration, Multilooking and Terrain Correction to your input products.\n" +
                        "At the end of the processing, press Next to mosaic them together.");
        this.add(textPanel, BorderLayout.NORTH);

        batchDlg = new BatchGraphDialog(VisatApp.getApp(),
                "Batch Processing", "batchProcessing", false);

        final JPanel batchPanel = new JPanel(new BorderLayout());
        batchPanel.add(batchDlg.getContent(), BorderLayout.CENTER);
        batchPanel.setBorder(BorderFactory.createTitledBorder("Batch Processing"));
        this.add(batchPanel, BorderLayout.CENTER);
    }

    private void batchProcess(final File[] fileList, final File targetFolder, final File graphFile) {

        batchDlg.setInputFiles(fileList);
        batchDlg.setTargetFolder(targetFolder);
        batchDlg.addListener(new MyBatchProcessListener());
        if (graphFile != null) {
            batchDlg.LoadGraphFile(graphFile);
        }
    }
}