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
package org.csa.rstb.dat.wizards.PolarimetryWizard;

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
public class PolSARWizardProcessPanel extends WizardPanel {

    private final GraphBuilderDialog graphDialog;
    private final static String wishartGraph = "T3_Spk_WishartGraph.xml";

    public PolSARWizardProcessPanel(final Product srcProduct) {
        super("Classification");

        graphDialog = new GraphBuilderDialog(VisatApp.getApp(), "Classification", "Classification", false);

        final File graphFile =  new File(wizardGraphPath, wishartGraph);

        graphDialog.LoadGraph(graphFile);
        graphDialog.setInputFile(srcProduct);
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
                "In the Polarimetric Matrices tab, select a Coherency T3 or Covariance C3 matrix.\n"+
                "Speckle filter using the Refined Lee filter.\n"+
                "Classify with the Wishart Classifier"+
                "Press finish to complete the processing.");
        this.add(textPanel, BorderLayout.NORTH);

        this.add(graphDialog.getContent(), BorderLayout.CENTER);
    }
}