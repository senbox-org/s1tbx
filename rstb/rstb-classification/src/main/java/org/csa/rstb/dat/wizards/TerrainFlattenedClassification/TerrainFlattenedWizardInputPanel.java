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
import org.esa.nest.dat.wizards.AbstractInputPanel;
import org.esa.nest.dat.wizards.WizardPanel;
import org.esa.nest.gpf.OperatorUtils;

/**
    Input Panel
 */
public class TerrainFlattenedWizardInputPanel extends AbstractInputPanel {

    public TerrainFlattenedWizardInputPanel() {
    }

    public WizardPanel getNextPanel() {

        return new TerrainFlattenedWizardProcessPanel(sourcePanel.getSelectedSourceProduct());
    }

    public boolean validateInput() {
        if(!super.validateInput()) return false;

        final Product product = sourcePanel.getSelectedSourceProduct();
        if(!OperatorUtils.isQuadPol(product)) {
            showErrorMsg("The product is not fully polarimetric.\nPlease select a Quad Pol SLC product");
            return false;
        }
        if(!OperatorUtils.isComplex(product)) {
            showErrorMsg("The product is not complex.\nPlease select a Quad Pol SLC product");
            return false;
        }
        return true;
    }

    protected String getInstructions() {
        return "Select a RADARSAT-2, TerraSAR-X, or ALOS Quad Pol SLC product";
    }
}