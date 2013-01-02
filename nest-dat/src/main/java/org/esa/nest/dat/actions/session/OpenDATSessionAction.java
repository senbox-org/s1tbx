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
package org.esa.nest.dat.actions.session;

import org.esa.beam.visat.actions.session.OpenSessionAction;
import org.esa.beam.visat.actions.session.RestoredSession;
import org.esa.nest.dat.toolviews.Projects.Project;

import java.io.File;


/**
 * Opens a session.
 *
 */
public class OpenDATSessionAction extends OpenSessionAction {

    public static final String ID = "openDATSession";

    protected void restoreProject(final RestoredSession restoredSession) {
        final File projectFile = restoredSession.getProjectFile();
        if(projectFile != null) {
            Project.instance().LoadProject(projectFile);
        }
    }

}