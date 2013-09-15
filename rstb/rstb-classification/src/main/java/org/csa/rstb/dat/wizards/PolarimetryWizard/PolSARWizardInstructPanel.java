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

import org.esa.nest.dat.wizards.AbstractInstructPanel;
import org.esa.nest.dat.wizards.WizardPanel;

/**
    Instructions Panel
 */
public class PolSARWizardInstructPanel extends AbstractInstructPanel {

    public final static String title = "Polarimetric Classification Wizard";

    public PolSARWizardInstructPanel() {
        super(title);
        imgPosX = 100;
        imgPosY = 240;
    }

    public WizardPanel getNextPanel() {
        return new PolSARWizardInputPanel();
    }

    protected String getDescription() {
        return "Welcome to the Polarimetric Classification Wizard.\n\n" +
               "With this wizard you will be able classify a fully polarimetric product.";
    }

    protected String getInstructions() {
        return "Step 1: Select a Quad Pol SLC product\n\n"+
               "Step 2: Create a T3 matrix, speckle filter and classify\n\n";
    }
}