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

import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.util.Guardian;

import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>DefaultCommandUIFactory</code> is used to create menu items and tool bar buttons for a given
 * {@link Command}. It is also used to add context dependent menu items to a menu.
 */
public class DefaultCommandUIFactory implements CommandUIFactory {

    private CommandManager _commandManager;
    private boolean _showingDisabledMenuItems;

    /**
     * Constructs a new factory.
     */
    public DefaultCommandUIFactory() {
        _showingDisabledMenuItems = true;
    }

    /**
     * Gets the command manager for this factory.
     *
     * @return the command manager, can be <code>null</code>.
     */
    public CommandManager getCommandManager() {
        return _commandManager;
    }

    /**
     * Sets the command manager for this factory.
     *
     * @param commandManager the command manager, can be <code>null</code>.
     */
    public void setCommandManager(CommandManager commandManager) {
        _commandManager = commandManager;
    }

    /**
     * Gets whether or not disabled menut items should be added to a menu in the <code>{@link
     * #addContextDependentMenuItems}</code> method.
     *
     * @return <code>true</code> if so
     */
    public boolean isShowingDisabledMenuItems() {
        return _showingDisabledMenuItems;
    }

    /**
     * Sets whether or not disabled menut items should be added to a menu in the <code>{@link
     * #addContextDependentMenuItems}</code> method.
     *
     * @param showingDisabledMenuItems <code>true</code> if so
     */
    public void setShowingDisabledMenuItems(boolean showingDisabledMenuItems) {
        _showingDisabledMenuItems = showingDisabledMenuItems;
    }

    /**
     * Creates a menu item for the given executable command.
     *
     * @param command the executable command, must not be <code>null</code>
     *
     * @return the menu item
     */
    public JMenuItem createMenuItem(ExecCommand command) {
        JMenuItem menuItem;
        if (command.isToggle()) {
            JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem(command.getAction());
            installMutualExclusiveInterest(checkBoxMenuItem, command);
            menuItem = checkBoxMenuItem;
        } else {
            menuItem = new JMenuItem(command.getAction());
        }
        menuItem.setName(command.getCommandID());
        KeyStroke keyStroke = command.getAccelerator();
        if (keyStroke != null) {
            menuItem.setAccelerator(keyStroke);
        }
        // Ensure that the menu item has some text, so that is guranteed to be visible for the user
        if (menuItem.getText() == null) {
            menuItem.setText(command.getCommandID());
        }
        enableHelp(menuItem, command);
        return menuItem;
    }

    /**
     * Creates a menu item for the given tool command. <i>The method is not implemented.</i>
     *
     * @param command the tool command, must not be <code>null</code>
     *
     * @return currently always <code>null</code!
     */
    public JMenuItem createMenuItem(ToolCommand command) {
        // @todo 3 nf/nf - not used
        return null;
    }

    /**
     * Creates a menu item for the given command group.
     *
     * @param commandGroup the command group, must not be <code>null</code>
     *
     * @return the menu item
     */
    public JMenuItem createMenuItem(CommandGroup commandGroup) {
        JMenu menu = new JMenu(commandGroup.getAction());
        menu.setName(commandGroup.getCommandID());
        // Ensure that the menu has some text, so that is guranteed to be visible for the user
        if (menu.getText() == null) {
            menu.setText(commandGroup.getCommandID());
        }
        enableHelp(menu, commandGroup);
        return menu;
    }

    /**
     * Creates a tool bar button for the given executable command.
     *
     * @param command the executable command, must not be <code>null</code>
     *
     * @return the tool bar button
     */
    public AbstractButton createToolBarButton(ExecCommand command) {
        AbstractButton button = ToolButtonFactory.createButton(command.getAction(), command.isToggle());
        button.setName(command.getCommandID());
        button.setSelected(command.isSelected());
        installMutualExclusiveInterest(button, command);
        return button;
    }

    /**
     * Creates a tool bar button for the given tool command.
     *
     * @param command the tool command, must not be <code>null</code>
     *
     * @return the tool bar button
     */
    public AbstractButton createToolBarButton(ToolCommand command) {
        AbstractButton button = ToolButtonFactory.createButton(command.getAction(), true);
        button.setName(command.getCommandID());
        button.setSelected(command.isSelected());
        installMutualExclusiveInterest(button, command);
        return button;
    }

    /**
     * Creates a tool bar button for the given command group.
     *
     * @param commandGroup the command group, must not be <code>null</code>
     *
     * @return the tool bar button
     */
    public AbstractButton createToolBarButton(CommandGroup commandGroup) {
        AbstractButton button = ToolButtonFactory.createButton(commandGroup.getAction(), false);
        button.setName(commandGroup.getCommandID());
        return button;
    }

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
    public JPopupMenu addContextDependentMenuItems(String context, JPopupMenu popup) {
        Guardian.assertNotNullOrEmpty("context", context);
        Guardian.assertNotNull("popup", popup);
        if (_commandManager == null) {
            return popup;
        }

        final List existingCommands = new ArrayList();
        final Component[] components = popup.getComponents();
        for (int i = 0; i < components.length; i++) {
            final Component component = components[i];
            if (component instanceof JMenuItem) {
                final JMenuItem jMenuItem = (JMenuItem) component;
                final String commandID = jMenuItem.getName();
                if (!existingCommands.contains(commandID)) {
                    existingCommands.add(commandID);
                }
            }
        }

        final int numCommands = _commandManager.getNumCommands();
        for (int i = 0; i < numCommands; i++) {
            final Command command = _commandManager.getCommandAt(i);
            final String commandID = command.getCommandID();
            if (command.containsContext(context) &&
                (command.isEnabled() || _showingDisabledMenuItems) &&
                !existingCommands.contains(commandID)) {
                existingCommands.add(commandID);
                final JMenuItem item = command.createMenuItem();
                String popupText = command.getPopupText();
                if (popupText != null && popupText.length() > 0) {
                    item.setText(popupText);
                }
                if (item != null) {
                    if (command.isPlaceAtContextTop()) {
                        popup.insert(item, 0);
                        if (command.isSeparatorAfter()) {
                            popup.insert(new JPopupMenu.Separator(), 1);
                        }
                    } else {
                        popup.add(item);
                    }
                }
            }
        }
        return popup;
    }


    private void enableHelp(Component menu, Command command) {
        HelpSys.enableHelp(menu, command.getHelpId());
    }

    private void installMutualExclusiveInterest(AbstractButton button, SelectableCommand command) {
        final MutualExclusiveInterestHandler selectionChangeListener = new MutualExclusiveInterestHandler(button,
                                                                                                          command);
        command.getAction().addPropertyChangeListener(selectionChangeListener);
        button.addActionListener(selectionChangeListener);
        button.setSelected(command.isSelected());
    }

    private static class MutualExclusiveInterestHandler implements ActionListener, PropertyChangeListener {

        private final AbstractButton _button;
        private final SelectableCommand _command;

        public MutualExclusiveInterestHandler(AbstractButton button, SelectableCommand command) {
            _button = button;
            _command = command;
        }

        public void actionPerformed(ActionEvent event) {
            //System.out.println("actionPerformed: " + event);
            _command.setSelected(_button.isSelected());
        }

        public void propertyChange(PropertyChangeEvent event) {
            //System.out.println("propertyChange: " + event);
            if (event.getPropertyName().equals(SelectableCommand.SELECTED_ACTION_KEY)) {
                _button.setSelected((Boolean) event.getNewValue());
            }
        }
    }
}
