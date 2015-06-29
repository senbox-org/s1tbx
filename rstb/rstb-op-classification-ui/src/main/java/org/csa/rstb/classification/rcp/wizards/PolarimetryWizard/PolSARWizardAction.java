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
package org.csa.rstb.classification.rcp.wizards.PolarimetryWizard;

import org.esa.s1tbx.dat.wizards.WizardDialog;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.util.IconUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
/*
@ActionID(
        category = "Wizards",
        id = "PolSARWizardAction"
)
@ActionRegistration(
        displayName = "#CTL_PolSARWizardAction_MenuText",
        popupText = "#CTL_PolSARWizardAction_MenuText",
        lazy = true
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Help/Wizards",
                position = 700
        )
})
@NbBundle.Messages({
        "CTL_PolSARWizardAction_MenuText=Polarimetric Classification Wizard",
        "CTL_PolSARWizardAction_ShortDescription=Polarimetric Classification Wizard"
})*/
public class PolSARWizardAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent event) {
        final WizardDialog dialog = new WizardDialog(SnapApp.getDefault().getMainFrame(), false,
                PolSARWizardInstructPanel.title, "PolSARWizard", new PolSARWizardInstructPanel());
        dialog.setIcon(IconUtils.rstbIcon);
        dialog.setVisible(true);
    }
}
