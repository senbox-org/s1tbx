/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.framework.ui.command;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;

/**
 * The <code>CommandGroup</code> is a group of commands represented by a menu item group.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class CommandGroup extends Command {

    public CommandGroup(String commandGroupID, CommandStateListener listener) {
        super(commandGroupID);
        addCommandStateListener(listener);
    }

    /**
     * Creates a menu item (a <code>JMenu</code> instance) for this command group.
	 */
    @Override
    public JMenuItem createMenuItem() {
        return getCommandUIFactory().createMenuItem(this);
    }

    /**
     * Creates an appropriate tool bar button for this command.
     */
    @Override
    public AbstractButton createToolBarButton() {
        return getCommandUIFactory().createToolBarButton(this);
    }

    /**
     * Adds a command listener.
     *
     * @param l the command listener
     */
    public void addCommandStateListener(CommandStateListener l) {
        addEventListener(CommandStateListener.class, l);
    }

    /**
     * Removes a command listener.
     *
     * @param l the command listener
     */
    public void removeCommandStateListener(CommandStateListener l) {
        removeEventListener(CommandStateListener.class, l);
    }
    
    
    @Override
    protected Action createAction() {
        return new AbstractAction() {

            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent actionEvent) {
            }
        };
    }
}