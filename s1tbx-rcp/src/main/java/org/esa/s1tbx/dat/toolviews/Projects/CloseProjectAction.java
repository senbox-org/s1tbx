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
package org.esa.s1tbx.dat.toolviews.Projects;

import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.framework.ui.command.ExecCommand;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This action opens a project.
 *
 * @author lveci
 * @version $Revision: 1.3 $ $Date: 2011-04-08 18:23:59 $
 */
@ActionID(category = "tool", id = "CloseProjectAction" )
@ActionRegistration(
        displayName = "#CTL_CloseProjectAction_MenuText",
        popupText = "#CTL_CloseProjectAction_MenuText",
        iconBase = "org/esa/s1tbx/dat/icons/project.png",
        lazy = true
)
@ActionReferences({
        //@ActionReference(path = "Menu/File/Projects", position = 60),
        //@ActionReference(path = "Toolbars/Projects", position = 60)
})
@NbBundle.Messages({
        "CTL_CloseProjectAction_MenuText=Close Project",
        "CTL_CloseProjectAction_ShortDescription=Close current project"
})
public class CloseProjectAction extends AbstractAction {

    @Override
    public void actionPerformed(final ActionEvent event) {

        Project.instance().CloseProject();
    }

    public void updateState() {
        setEnabled(Project.instance().IsProjectOpen());
    }
}
