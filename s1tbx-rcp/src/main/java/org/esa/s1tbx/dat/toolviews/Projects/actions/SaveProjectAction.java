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
package org.esa.s1tbx.dat.toolviews.Projects.actions;

import org.esa.s1tbx.dat.toolviews.Projects.Project;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;

/**
 * This action opens a project.
 *
 * @author lveci
 * @version $Revision: 1.3 $ $Date: 2011-04-08 18:23:59 $
 */

@ActionID(category = "Projects", id = "SaveProjectAction")
@ActionRegistration(
        displayName = "#CTL_SaveProjectAction_MenuText",
        popupText = "#CTL_SaveProjectAction_MenuText"
)
@ActionReference(path = "Menu/File/Projects", position = 40)
@NbBundle.Messages({
        "CTL_SaveProjectAction_MenuText=Save Project",
        "CTL_SaveProjectAction_ShortDescription=Save current project"
})
public class SaveProjectAction extends AbstractSnapAction implements Project.Listener {

    public SaveProjectAction() {
        Project.instance().addListener(this);
        setEnableState();
    }

    private void setEnableState() {
        setEnabled(Project.instance().IsProjectOpen());
    }

    public void projectChanged() {
        setEnableState();
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
        Project.instance().SaveProject();
    }
}
