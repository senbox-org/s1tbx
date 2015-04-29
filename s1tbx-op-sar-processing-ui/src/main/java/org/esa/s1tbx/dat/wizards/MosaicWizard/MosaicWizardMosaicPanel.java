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
package org.esa.s1tbx.dat.wizards.MosaicWizard;

import org.esa.s1tbx.dat.wizards.WizardPanel;
import org.esa.snap.graphbuilder.rcp.dialogs.GraphBuilderDialog;
import org.esa.snap.rcp.SnapApp;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;

/**
 * Mosaic Wizard Mosaic panel
 */
public class MosaicWizardMosaicPanel extends WizardPanel {

    private final GraphBuilderDialog graphDialog;

    public MosaicWizardMosaicPanel(final File[] productFileList) {
        super("Mosaic");

        graphDialog = new GraphBuilderDialog(new SnapApp.SnapContext(), "Mosaic", "MosaicOp", false);

        final File graphPath = GraphBuilderDialog.getInternalGraphFolder();
        final File graphFile = new File(graphPath, "MosaicGraph.xml");

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
                "In the Mosaic tab, specify the either output pixel size or the scene pixel width and height\n" +
                        "Press Process to complete the mosaic.");
        this.add(textPanel, BorderLayout.NORTH);

        this.add(graphDialog.getContent(), BorderLayout.CENTER);
    }
}
