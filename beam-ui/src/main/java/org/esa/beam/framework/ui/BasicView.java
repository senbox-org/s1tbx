/*
 * $Id: BasicView.java,v 1.1 2006/10/10 14:47:38 norman Exp $
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
package org.esa.beam.framework.ui;

import javax.swing.JPanel;

import org.esa.beam.framework.ui.command.CommandUIFactory;

/**
 * The base class for application view panes. It provides support for a context menu and command handling.
 *
 * @see org.esa.beam.framework.ui.command.CommandManager
 * @see org.esa.beam.framework.ui.command.CommandUIFactory
 * @see org.esa.beam.framework.ui.PopupMenuFactory
 */
public abstract class BasicView extends JPanel implements PopupMenuFactory {

    private CommandUIFactory _commandUIFactory;

    /**
     * Creates a new <code>BasicView</code> with a double buffer and a flow layout.
     */
    public BasicView() {
    }

    /**
     * Gets the command UI factory used to create the context dependent menu items for the context menu associated with
     * this view.
     *
     * @return the command UI factory
     */
    public CommandUIFactory getCommandUIFactory() {
        return _commandUIFactory;
    }

    /**
     * Sets the command UI factory used to create the context dependent menu items for the context menu associated with
     * this view.
     *
     * @param commandUIFactory the command UI factory
     */
    public void setCommandUIFactory(CommandUIFactory commandUIFactory) {
        _commandUIFactory = commandUIFactory;
    }

    /**
     * Releases all of the resources used by this view, its subcomponents, and all of its owned children.
     */
    public void dispose() {
        _commandUIFactory = null;
    }
}
