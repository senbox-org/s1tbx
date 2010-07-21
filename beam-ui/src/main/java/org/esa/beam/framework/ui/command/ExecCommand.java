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
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

// @todo 1 nf/nf - place class API docu here

/**
 * The <code>ExecCommand</code> is an ...
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class ExecCommand extends SelectableCommand implements ConfigurableExtension {

    public static final String TOGGLE_ACTION_KEY = "_toggle";

    public ExecCommand() {
        super();
    }

    public ExecCommand(String commandID) {
        super(commandID);
    }

    public ExecCommand(String commandID, CommandListener listener) {
        super(commandID);
        addCommandListener(listener);
    }

    public boolean isToggle() {
        return getProperty(TOGGLE_ACTION_KEY, false);
    }

    public void setToggle(boolean toggle) {
        setProperty(TOGGLE_ACTION_KEY, toggle);
    }

    /**
     * Executes this command.
     */
    public void execute() {
        execute(null);
    }

    /**
     * Executes this command with the given command-specific argument.
     */
    public void execute(Object argument) {
        fireActionPerformed(null, argument);
    }


    /**
     * Creates a menu item (a <code>JMenuItem</code> or <code>JCheckBoxMenuItem</code> instance) for this command
     * group.
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
    @Override
    public void addCommandListener(CommandListener l) {
        addEventListener(CommandListener.class, l);
    }

    /**
     * Removes a command listener.
     *
     * @param l the command listener
     */
    @Override
    public void removeCommandListener(CommandListener l) {
        removeEventListener(CommandListener.class, l);
    }

    /**
     * Notify all listeners that have registered interest for notification on the 'action performed' event type. The
     * event instance is lazily created using the parameters passed into the fire method.
     */
    @Override
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
     * Configures this command with the properties (if any) found in the given recource bundle. Overrides the base class
     * implementation in order to configure the following extra properties:<p> <ld>
     * <li><code>command.</code><i>command-ID</i><code>.toggle = true</code> or <code>false</code></li> </ld>
     *
     * @param resourceBundle the resource bundle from which the properties are received
     *
     * @throws IllegalArgumentException if the recource bundle is null
     */
    @Override
    public void configure(ResourceBundle resourceBundle) {
        super.configure(resourceBundle);
        Boolean resBoolean;

        resBoolean = getResourceBoolean(resourceBundle, "toggle");
        if (resBoolean != null) {
            setToggle(resBoolean);
        }
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);

        Boolean confBoolean = getConfigBoolean(config, "toggle");
        if (confBoolean != null) {
            setToggle(confBoolean);
        }
    }
}

