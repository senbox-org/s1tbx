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

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * The <code>CommandUIFactory</code> is used to create menu items and tool bar buttons for different types of a
 * <code>{@link org.esa.beam.framework.ui.command.Command}</code> instance. It is also used to add context dependent
 * menu items to a menu.
 */
public interface CommandUIFactory {

    /**
     * Gets the command manager for this factory.
     *
     * @return the command manager, can be <code>null</code>.
     */
    CommandManager getCommandManager();

    /**
     * Sets the command manager for this factory.
     *
     * @param commandManager the command manager, can be <code>null</code>.
     */
    void setCommandManager(CommandManager commandManager);

    /**
     * Gets whether or not disabled menut items should be added to a menu in the <code>{@link
     * #addContextDependentMenuItems}</code> method.
     *
     * @return <code>true</code> if so
     */
    boolean isShowingDisabledMenuItems();

    /**
     * Sets whether or not disabled menut items should be added to a menu in the <code>{@link
     * #addContextDependentMenuItems}</code> method.
     *
     * @param showingDisabledMenuItems <code>true</code> if so
     */
    void setShowingDisabledMenuItems(boolean showingDisabledMenuItems);

    /**
     * Creates a menu item for the given executable command.
     *
     * @param command the executable command, must not be <code>null</code>
     *
     * @return the menu item
     */
    JMenuItem createMenuItem(ExecCommand command);

    /**
     * Creates a menu item for the given tool command. <i>The method is not implemented.</i>
     *
     * @param command the tool command, must not be <code>null</code>
     *
     * @return currently always <code>null</code!
     */
    JMenuItem createMenuItem(ToolCommand command);

    /**
     * Creates a menu item for the given command group.
     *
     * @param commandGroup the command group, must not be <code>null</code>
     *
     * @return the menu item
     */
    JMenuItem createMenuItem(CommandGroup commandGroup);

    /**
     * Creates a tool bar button for the given executable command.
     *
     * @param command the executable command, must not be <code>null</code>
     *
     * @return the tool bar button
     */
    AbstractButton createToolBarButton(ExecCommand command);

    /**
     * Creates a tool bar button for the given tool command.
     *
     * @param command the tool command, must not be <code>null</code>
     *
     * @return the tool bar button
     */
    AbstractButton createToolBarButton(ToolCommand command);

    /**
     * Creates a tool bar button for the given command group.
     *
     * @param commandGroup the command group, must not be <code>null</code>
     *
     * @return the tool bar button
     */
    AbstractButton createToolBarButton(CommandGroup commandGroup);

    /**
     * Adds menu items to the given popup menu. A command manager must have been set before this method can be used.
     *
     * @param context the context string
     * @param popup   the popup menu, must not be <code>null</code>
     *
     * @return the given popup menu, all context matching menu items added.
     *
     * @see #setCommandManager
     */
    JPopupMenu addContextDependentMenuItems(String context, JPopupMenu popup);
}
