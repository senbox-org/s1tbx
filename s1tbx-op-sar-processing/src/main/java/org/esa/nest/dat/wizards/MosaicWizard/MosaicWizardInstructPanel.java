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

import org.esa.nest.dat.wizards.AbstractInstructPanel;
import org.esa.nest.dat.wizards.WizardPanel;

/**
 * Instructions Panel
 */
public class MosaicWizardInstructPanel extends AbstractInstructPanel {


    public MosaicWizardInstructPanel() {
        super("Ortho-Mosaic Wizard");
    }

    public WizardPanel getNextPanel() {
        return new MosaicWizardInputPanel();
    }

    public boolean validateInput() {
        return true;
    }

    protected String getDescription() {
        return "Welcome to the Ortho-Mosaic Wizard.\n\n" +
                "With this wizard you will be able to select multiple products,\n\n" +
                "orthorectify them and combine them into a mosaic.";
    }

    protected String getInstructions() {
        return "Step 1: Select overlapping products\n\n" +

                "Step 2: View the products on the worldmap\n\n" +
                "Step 3: Use a graph to calibrate and terrain correct in a Batch process\n\n" +
                "Step 4: Mosaic the resulting products together";
    }
}
