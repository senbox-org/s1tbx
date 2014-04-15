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
import com.bc.ceres.core.runtime.Module;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * The <code>CommandGroup</code> is a group of commands represented by a menu item group or a tool bar.
 * As of BEAM 5, <code>CommandGroup</code>s can also be used to collect group items. There can be either
 * (forward declarations of) contained command IDs or other commands group IDs or separators.
 *
 * @author Norman Fomferra
 */
public class CommandGroup extends Command {

    public static final String ACTION_KEY_GROUP_ITEMS = "_groupItems";

    private static final String ELEMENT_NAME_ITEMS = "items";
    private static final String ELEMENT_NAME_ACTION_ID = "action";
    private static final String ELEMENT_NAME_ACTION_GROUP_ID = "actionGroup";
    private static final String ELEMENT_NAME_SEPARATOR = "separator";

    public CommandGroup() {
        super(CommandGroup.class.getName());
    }

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

    public String[] getGroupItems() {
        return getProperty(ACTION_KEY_GROUP_ITEMS, new String[0]);
    }

    public void setGroupItems(String[] groupItems) {
        setProperty(ACTION_KEY_GROUP_ITEMS, groupItems.clone());
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        super.configure(config);
        ConfigurationElement itemsElement = config.getChild(ELEMENT_NAME_ITEMS);
        if (itemsElement == null) {
            return;
        }
        ConfigurationElement[] children = itemsElement.getChildren();
        String[] groupItems = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            ConfigurationElement child = children[i];
            String childName = child.getName();
            switch (childName) {
                case ELEMENT_NAME_ACTION_ID:
                case ELEMENT_NAME_ACTION_GROUP_ID:
                    groupItems[i] = child.getValue().trim();
                    break;
                case ELEMENT_NAME_SEPARATOR:
                    groupItems[i] = null;
                    break;
                default:
                    Module declaringModule = config.getDeclaringExtension().getDeclaringModule();
                    throw new CoreException(String.format("Module [%s]: '%s' is an unknown 'groupItems' element", declaringModule.getName(), childName));
            }
        }
        setProperty(ACTION_KEY_GROUP_ITEMS, groupItems);
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