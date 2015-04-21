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

import org.esa.s1tbx.dat.wizards.AbstractMapPanel;
import org.esa.s1tbx.dat.wizards.WizardPanel;

import java.io.File;

/**
 * Map Panel
 */
public class MosaicWizardMapPanel extends AbstractMapPanel {

    public MosaicWizardMapPanel(final File[] productFileList, final File targetFolder) {
        super(productFileList, targetFolder);
    }

    public WizardPanel getNextPanel() {
        return new MosaicWizardBatchPanel(productFileList, targetFolder);
    }

    protected String getInstructions() {
        return super.getInstructions() +
                "Press Next to begin terrain correcting the products in a batch process.";
    }
}
