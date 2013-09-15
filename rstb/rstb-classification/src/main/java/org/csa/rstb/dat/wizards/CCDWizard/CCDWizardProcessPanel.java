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
package org.csa.rstb.dat.wizards.CCDWizard;

import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;
import org.esa.nest.dat.wizards.WizardPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**

 */
public class CCDWizardProcessPanel extends WizardPanel {

    private final GraphBuilderDialog graphDialog;

    public CCDWizardProcessPanel(final File[] productFileList) {
        super("CCD");

        graphDialog = new GraphBuilderDialog(VisatApp.getApp(), "Coregistration", "Coregistration", false);

        final File graphFile =  new File(wizardGraphPath, "CoregisteredCoherenceML.xml");

        graphDialog.LoadGraph(graphFile);
        graphDialog.setInputFiles(productFileList);
        graphDialog.addListener(new GraphProcessListener());

        createPanel();
    }

    public void returnFromLaterStep() {

    }

    // on finish
    public void finish() {
        graphDialog.DoProcessing();
        finishing = true;
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean hasNextPanel() {
        return false;
    }

    public boolean canFinish() {
        return true;
    }

    public WizardPanel getNextPanel() {
        return null;
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        final JPanel textPanel = createTextPanel("Instructions",
                "In the Create Stack tab, specify one master band or one real imaginary pair.\n"+
                "The stack will use the geocoding of the master band.\n"+
                "Select the slave bands to include in the stack.\n"+
                "In the GCP Select tab, select the number of GCPs to use and the window size to perform a cross correlation.\n"+
                "In the Warp tab, select the desired pixel accuracy with the RMS threshold.\n"+
                "Press finish to complete the processing.");
        this.add(textPanel, BorderLayout.NORTH);

        this.add(graphDialog.getContent(), BorderLayout.CENTER);
    }
}