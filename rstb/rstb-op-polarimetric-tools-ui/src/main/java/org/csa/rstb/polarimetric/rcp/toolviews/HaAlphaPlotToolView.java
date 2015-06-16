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
package org.csa.rstb.polarimetric.rcp.toolviews;

import org.esa.snap.rcp.statistics.AbstractStatisticsTopComponent;
import org.esa.snap.rcp.statistics.PagePanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

@TopComponent.Description(
        preferredID = "HaAlphaPlotTopComponent",
        iconBase = "org/csa/rstb/polarimetric/icons/h-a-alpha22.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS //todo define
)
@TopComponent.Registration(
        mode = "navigator",
        openAtStartup = false,
        position = 1
)
@ActionID(category = "Window", id = "org.csa.rstb.dat.toolviews.HaAlphaPlotTopComponent")
@ActionReferences({
        @ActionReference(path = "Menu/Window/Tool Windows/Analysis"),
        @ActionReference(path = "Toolbars/Analysis")
})
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_HaAlphaPlotTopComponent_Name",
        preferredID = "HaAlphaPlotTopComponent"
)
@NbBundle.Messages({
        "CTL_HaAlphaPlotTopComponent_Name=H-a Alpha Plot",
        "CTL_HaAlphaPlotTopComponent_HelpId=correlativePlotDialog"
})
/**
 * A window which displays the H-a alpha plane plot.
 */
public class HaAlphaPlotToolView extends AbstractStatisticsTopComponent {

    @Override
    protected PagePanel createPagePanel() {
        return new HaAlphaPlotPanel(this, "");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Bundle.CTL_HaAlphaPlotTopComponent_HelpId());
    }

}
