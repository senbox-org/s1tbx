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

import org.esa.s1tbx.dat.wizards.WizardDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.util.IconUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
/*
@ActionID(
        category = "Wizards",
        id = "TerrainFlattenedClassificationWizardAction"
)
@ActionRegistration(
        displayName = "#CTL_TerrainFlattenedClassificationWizardAction_MenuText",
        popupText = "#CTL_TerrainFlattenedClassificationWizardAction_MenuText",
        lazy = true
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Help/Wizards",
                position = 800
        )
})
@NbBundle.Messages({
        "CTL_TerrainFlattenedClassificationWizardAction_MenuText=Terrain Flattened Classification Wizard",
        "CTL_TerrainFlattenedClassificationWizardAction_ShortDescription=Terrain Flattened Classification Wizard"
})*/
public class TerrainFlattenedClassificationWizardAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent event) {
        final WizardDialog dialog = new WizardDialog(SnapApp.getDefault().getMainFrame(), false,
                TerrainFlattenedWizardInstructPanel.title, "TerrainFlattenedClassificationWizard",
                new TerrainFlattenedWizardInstructPanel());
        dialog.setIcon(IconUtils.rstbIcon);
        dialog.setVisible(true);
    }
}
