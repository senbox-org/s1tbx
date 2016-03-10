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
package org.esa.s1tbx.insar.rcp.actions;

import org.esa.s1tbx.insar.rcp.dialogs.VirtualStackCoregistrationDialog;
import org.esa.snap.rcp.SnapApp;

import javax.swing.*;
import java.awt.event.ActionEvent;

/*
@ActionID(
        category = "Tools",
        id = "VirtualStackProcessingAction"
)
@ActionRegistration(
        displayName = "#CTL_VirtualStackProcessingAction_MenuText",
        popupText = "#CTL_VirtualStackProcessingAction_MenuText",
        iconBase = "org/esa/snap/graphbuilder/icons/batch24.png",
        lazy = true
)
@ActionReferences({
        @ActionReference(path = "Menu/Radar/Coregistration", position = 700)
})
@NbBundle.Messages({
        "CTL_VirtualStackProcessingAction_MenuText=VirtualStack Processing",
        "CTL_VirtualStackProcessingAction_ShortDescription=VirtualStack multi-output coregistration"
}) */
public class VirtualStackProcessingAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent event) {
        final VirtualStackCoregistrationDialog dialog = new VirtualStackCoregistrationDialog(
                SnapApp.getDefault().getAppContext(),
                "VirtualStack Processing", "VirtualStackProcessing", false);
        dialog.show();
    }

}
