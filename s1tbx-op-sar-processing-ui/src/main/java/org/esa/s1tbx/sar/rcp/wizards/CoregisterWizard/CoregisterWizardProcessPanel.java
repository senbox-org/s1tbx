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
package org.esa.s1tbx.sar.rcp.wizards.CoregisterWizard;

import org.esa.s1tbx.dat.wizards.WizardPanel;
import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.rcp.SnapApp;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;

/**

 */
public class CoregisterWizardProcessPanel extends WizardPanel {

    private final GraphBuilderDialog graphDialog;

    public CoregisterWizardProcessPanel(final File[] productFileList) {
        super("Coregistration");

        graphDialog = new GraphBuilderDialog(new SnapApp.SnapContext(),
                "Coregistration", "Coregistration", false);

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile = new File(graphPath, "CoregistrationGraph.xml");

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
                "In the Create Stack tab, specify one master band or one real imaginary pair.\n" +
                        "The stack will use the geocoding of the master product.\n" +
                        "Select the slave bands to include in the stack.\n" +
                        "In the GCP Selection tab, select the number of GCPs to use\nand the window size for cross correlation.\n" +
                        "In the Warp tab, select the desired pixel accuracy with the RMS threshold.\n" +
                        "Press finish to complete the processing.");
        this.add(textPanel, BorderLayout.NORTH);

        this.add(graphDialog.getContent(), BorderLayout.CENTER);
    }
}
