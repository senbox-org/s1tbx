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

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.session.SaveSessionAction;
import org.esa.beam.visat.actions.session.SaveSessionAsAction;

/**
 * Saves a session with a new filename.
 *
 */
public class SaveDATSessionAsAction extends SaveSessionAsAction {
    public static final String ID = "saveDATSessionAs";
    
    @Override
    public final void actionPerformed(final CommandEvent event) {
        final SaveDATSessionAction action = (SaveDATSessionAction) VisatApp.getApp().getCommandManager().getCommand(SaveSessionAction.ID);
        action.saveSession(true);
    }
}