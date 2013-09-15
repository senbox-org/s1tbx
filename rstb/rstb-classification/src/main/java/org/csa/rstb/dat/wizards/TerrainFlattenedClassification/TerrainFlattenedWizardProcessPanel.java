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
package org.csa.rstb.dat.wizards.TerrainFlattenedClassification;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.plugins.graphbuilder.GraphBuilderDialog;
import org.esa.nest.dat.wizards.WizardPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
    Processing
 */
public class TerrainFlattenedWizardProcessPanel extends WizardPanel {

    private final GraphBuilderDialog graphDialog;

    public TerrainFlattenedWizardProcessPanel(final Product srcProduct) {
        super("Terrain Flattened Terrain Correction");

        graphDialog = new GraphBuilderDialog(VisatApp.getApp(), "TerrainFlatten", "Terrain Flattened Terrain Correction", false);

        final File graphFile =  new File(wizardGraphPath, "TerrainFlattenedT3.xml");

        graphDialog.LoadGraph(graphFile);
        graphDialog.setInputFile(srcProduct);
        graphDialog.addListener(new GraphProcessListener());

        createPanel();
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean hasNextPanel() {
        if(graphDialog != null) {
            return !graphDialog.isProcessing();
        }
        return true;
    }

    public boolean canFinish() {
        return false;
    }

    public WizardPanel getNextPanel() {
        return new TerrainFlattenedWizardClassifyPanel(getTargetFileList());
    }

    public boolean validateInput() {
        if(getTargetFileList().length == 0) {
            graphDialog.DoProcessing();
            getOwner().updateState();
        }
        return getTargetFileList().length != 0;
    }

    private void createPanel() {

        final JPanel textPanel = createTextPanel("Instructions",
                "In the Terrain Flatten tab, select a DEM to use.\n"+
                "In the Terrain Correction tab, select the DEM and the output pixel spacing.\n"+
                "Press finish to complete the processing.");
        this.add(textPanel, BorderLayout.NORTH);

        this.add(graphDialog.getContent(), BorderLayout.CENTER);
    }
}