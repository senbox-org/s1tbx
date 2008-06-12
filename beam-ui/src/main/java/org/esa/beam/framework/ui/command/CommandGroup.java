/*
 * $Id: CommandGroup.java,v 1.3 2007/04/23 13:51:00 marcoz Exp $
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
package org.esa.beam.framework.ui.command;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenuItem;

// @todo 1 nf/nf - place class API docu here

/**
 * The <code>CommandGroup</code> is a ...
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
    public JMenuItem createMenuItem() {
        return getCommandUIFactory().createMenuItem(this);
    }

    /**
     * Creates an appropriate tool bar button for this command.
     */
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