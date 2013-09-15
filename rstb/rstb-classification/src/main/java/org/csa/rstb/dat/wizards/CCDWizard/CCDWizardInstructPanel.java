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

import org.esa.nest.dat.wizards.AbstractInstructPanel;
import org.esa.nest.dat.wizards.WizardPanel;
import org.esa.nest.util.ResourceUtils;

import java.io.File;

/**
    Instructions Panel
 */
public class CCDWizardInstructPanel extends AbstractInstructPanel {

    public final static String title = "CCD Wizard";

    public CCDWizardInstructPanel() {
        super(title);
        imgPosX = 100;
        imgPosY = 240;
        image = ResourceUtils.loadImage(new File(ResourceUtils.getResFolder(), "ccd.png"));
    }

    public WizardPanel getNextPanel() {
        return new CCDWizardInputPanel();
    }

    protected String getDescription() {
        return "Welcome to the Coherent Change Detection Wizard.\n\n" +
                "With this wizard you will be able to select multiple complex data products,\n\n"+
                "precisely coregister them together and generate a coherence map";
    }

    protected String getInstructions() {
        return "Step 1: Select overlapping complex products\n\n"+
                "Step 2: View the products on the worldmap\n\n"+
                "Step 3: Use a graph to coregister the products into a stack\n\n";
    }
}