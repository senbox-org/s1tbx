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

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.ConfigurationElement;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * A command which also has a 'selected' state.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public abstract class SelectableCommand extends Command {

    public static final String SELECTED_ACTION_KEY = "_selected";

    public SelectableCommand() {
        super();
    }

    public SelectableCommand(String commandID) {
        super(commandID);
    }

    public SelectableCommand(String commandID, CommandListener listener) {
        super(commandID);
        addCommandListener(listener);
    }

    public boolean isSelected() {
        return getProperty(SELECTED_ACTION_KEY, false);
    }

    public void setSelected(boolean selected) {
        setProperty(SELECTED_ACTION_KEY, selected);
    }

    /**
     * Adds a command listener.
     *
     * @param l the command listener
     */
    public void addCommandListener(CommandListener l) {
        addEventListener(CommandListener.class, l);
    }

    /**
     * Removes a command listener.
     *
     * @param l the command listener
     */
    public void removeCommandListener(CommandListener l) {
        removeEventListener(CommandListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on the 'action performed' event type. The
     * event instance is lazily created using the parameters passed into the fire method.
     */
    protected void fireActionPerformed(ActionEvent actionEvent, Object argument) {
        actionPerformed(new CommandEvent(this, actionEvent, null, argument));
        if (getEventListenerList() == null) {
            return;
        }
        // Guaranteed to return a non-null array
        Object[] listeners = getEventListenerList().getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        CommandEvent commandEvent = null;
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CommandListener.class) {
                // Lazily create the event:
                if (commandEvent == null) {
                    commandEvent = new CommandEvent(this, actionEvent, null, argument);
                }
                ((CommandListener) listeners[i + 1]).actionPerformed(commandEvent);
            }
        }
    }

    /**
     * Configures this command with the properties (if any) found in the given resource bundle. Overrides the base class
     * implementation in order to configure the following extra properties:<p> <ld>
     * <li><code>command.</code><i>command-ID</i><code>.selected = true</code> or <code>false</code></li> </ld>
     *
     * @param resourceBundle the resource bundle from which the properties are received
     *
     * @throws IllegalArgumentException if the resource bundle is null
     */
    @Override
    public void configure(ResourceBundle resourceBundle) {
        super.configure(resourceBundle);
        Boolean resBoolean;

        resBoolean = getResourceBoolean(resourceBundle, "selected");
        if (resBoolean != null) {
            setSelected(resBoolean);
        }
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        Boolean resBoolean = getConfigBoolean(config, "selected");
        if (resBoolean != null) {
            setSelected(resBoolean);
        }

    }


    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    public void actionPerformed(CommandEvent event) {
    }


    @Override
    protected Action createAction() {
        return new AbstractAction() {

            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent actionEvent) {
                fireActionPerformed(actionEvent, null);
            }
        };
    }
}

