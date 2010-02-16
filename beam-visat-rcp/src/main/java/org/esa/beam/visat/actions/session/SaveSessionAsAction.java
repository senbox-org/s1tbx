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

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import java.awt.Container;
import java.io.File;
import java.util.ArrayList;


/**
 * Saves a VISAT session with a new filename.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class SaveSessionAsAction extends ExecCommand {
    public static final String ID = "saveSessionAs";
    
    @Override
    public final void actionPerformed(final CommandEvent event) {
        final SaveSessionAction action = (SaveSessionAction) VisatApp.getApp().getCommandManager().getCommand(SaveSessionAction.ID);
        action.saveSession(true);
    }

    @Override
    public final void updateState(final CommandEvent event) {
        final VisatApp app = VisatApp.getApp();
        setEnabled(app.getSessionFile() != null);
    }
}