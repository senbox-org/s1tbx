/*
 * $Id: SaveAction.java,v 1.2 2006/11/21 09:05:56 olga Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions.session;

import org.esa.beam.visat.VisatApp;

import java.io.File;


public class SaveSessionAction extends AbstractSaveSessionAction {

    public SaveSessionAction() {
        super("Save Session");
    }

    @Override
    protected File getSessionFile(VisatApp app) {
        File sessionFile = app.getSessionFile();
        if (sessionFile == null) {
            sessionFile = app.showFileSaveDialog(getTitle(), false,
                                                 OpenSessionAction.SESSION_FILE_FILTER,
                                                 OpenSessionAction.SESSION_FILE_FILTER.getDefaultExtension(),
                                                 System.getProperty("user.name", "noname"),
                                                 OpenSessionAction.LAST_SESSION_DIR_KEY);
        }
        return sessionFile;
    }
}