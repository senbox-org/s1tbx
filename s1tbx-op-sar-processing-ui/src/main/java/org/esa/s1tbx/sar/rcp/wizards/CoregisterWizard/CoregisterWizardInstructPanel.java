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

import org.esa.s1tbx.dat.wizards.AbstractInstructPanel;
import org.esa.s1tbx.dat.wizards.WizardPanel;
import org.esa.snap.util.ResourceUtils;

import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Instructions Panel
 */
public class CoregisterWizardInstructPanel extends AbstractInstructPanel {

    public final static String title = "Coregistration Wizard";

    public CoregisterWizardInstructPanel() {
        super(title);
        imgPosX = 100;
        imgPosY = 240;
        try {
            image = ImageIO.read(ResourceUtils.getResourceAsStream("org/esa/s1tbx/coreg.png", CoregisterWizardInstructPanel.class));
        } catch (IOException e) {
            image = null;
        }
    }

    public WizardPanel getNextPanel() {
        return new CoregisterWizardInputPanel();
    }

    protected String getDescription() {
        return "Welcome to the Coregistration Wizard.\n\n" +
                "With this wizard you will be able to select multiple overlapping products,\n\n" +
                "and precisely coregister them together into one product stack.";
    }

    protected String getInstructions() {
        return "Step 1: Select overlapping products\n\n" +
                "Step 2: View the products on the worldmap\n\n" +
                "Step 3: Use a graph to coregister the products into a stack\n\n";
    }
}
