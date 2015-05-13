/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.classification.rcp.wizards.TerrainFlattenedClassification;

import org.esa.s1tbx.dat.wizards.AbstractInputPanel;
import org.esa.s1tbx.dat.wizards.WizardPanel;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.gpf.InputProductValidator;

/**
 * Input Panel
 */
public class TerrainFlattenedWizardInputPanel extends AbstractInputPanel {

    public TerrainFlattenedWizardInputPanel() {
    }

    public WizardPanel getNextPanel() {

        return new TerrainFlattenedWizardProcessPanel(sourcePanel.getSelectedSourceProduct());
    }

    public boolean validateInput() {
        if (!super.validateInput()) return false;

        try {
            final Product product = sourcePanel.getSelectedSourceProduct();
            final InputProductValidator validator = new InputProductValidator(product);
            validator.checkIfQuadPolSLC();
        } catch (Exception e){
            showErrorMsg("Invalid input product: "+e.getMessage());
            return false;
        }
        return true;
    }

    protected String getInstructions() {
        return "Select a RADARSAT-2, TerraSAR-X, or ALOS Quad Pol SLC product";
    }
}
