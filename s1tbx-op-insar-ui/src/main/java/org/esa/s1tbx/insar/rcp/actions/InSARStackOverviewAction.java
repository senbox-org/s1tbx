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
package org.esa.s1tbx.insar.rcp.actions;

import org.esa.s1tbx.insar.rcp.dialogs.InSARStackOverviewDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

@ActionID(category = "Raster", id = "InSARStackOverviewAction")
@ActionRegistration(displayName = "#CTL_InSARStackOverviewActionName")
@ActionReference(path = "Menu/Radar/Interferometric", position = 600)
@NbBundle.Messages({
        "CTL_InSARStackOverviewActionName=InSAR Stack Overview",
        "CTL_InSARStackOverviewActionDescription=Show InSAR Stack Baselines"
})
public class InSARStackOverviewAction extends AbstractSnapAction {

    public InSARStackOverviewAction() {
        putValue(NAME, "");//Bundle.CTL_InSARStackOverviewActionName());
        putValue(SHORT_DESCRIPTION, "");//Bundle.CTL_InSARStackOverviewActionDescription());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final InSARStackOverviewDialog dialog = new InSARStackOverviewDialog();
        dialog.show();
    }
}
