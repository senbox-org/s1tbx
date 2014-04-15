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


import com.bc.ceres.core.Assert;
import org.esa.beam.util.logging.BeamLogManager;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Default implementation for the <code>CommandMenuInserter</code> interface.
 *
 * @author Norman Fomferra
 * @since BEAM 5 (complete rewrite of insertion algorithm)
 */
public class DefaultCommandMenuInserter implements CommandMenuInserter {

    private static final Comparator<Command> NAME_COMPARATOR = new CommandNameComparator();

    private final CommandManager commandManager;

    public DefaultCommandMenuInserter(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void insertCommandIntoMenu(Command newCommand, JMenu menu) {
        insertCommandIntoMenu(newCommand, menu, commandManager);
    }

    private static void insertCommandIntoMenu(Command newCommand, JMenu menu, CommandManager commandManager) {

        JMenuItem newMenuItem = newCommand.createMenuItem();
        if (newMenuItem == null) {
            return;
        }

        Map<String, Command> commandMap = new HashMap<>();
        Map<String, JMenuItem> menuItemMap = new HashMap<>();

        commandMap.put(newCommand.getCommandID(), newCommand);
        menuItemMap.put(newCommand.getCommandID(), newMenuItem);

        JPopupMenu popupMenu = menu.getPopupMenu();

        int componentCount = popupMenu.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            Component component = popupMenu.getComponent(i);
            if (component instanceof JMenuItem) {
                String commandID = component.getName();
                if (commandID != null) {
                    Command command = commandManager.getCommand(commandID);
                    if (command != null) {
                        commandMap.put(commandID, command);
                        menuItemMap.put(commandID, (JMenuItem) component);
                    }
                }
            }
        }

        BeamLogManager.getSystemLogger().fine(String.format("Inserting command '%s' into menu '%s' with %d item(s)", newCommand.getCommandID(), menu.getName(), componentCount));

        ArrayList<Command> commands = new ArrayList<>(commandMap.values());
        if (isSortedByName(menu, commandManager)) {
            sortCommandsByName(commands);
        } else {
            sortCommandsByAnchor(commands);
        }

        repopulatePopMenu(popupMenu, commands, menuItemMap);
    }

    private static void repopulatePopMenu(JPopupMenu popupMenu, ArrayList<Command> commands, Map<String, JMenuItem> menuItemMap) {
        popupMenu.removeAll();

        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            JMenuItem menuItem = menuItemMap.get(command.getCommandID());
            Assert.state(menuItem != null);
            int componentCount = popupMenu.getComponentCount();
            if (command.isSeparatorBefore() && componentCount > 0 && !(popupMenu.getComponent(componentCount - 1) instanceof JSeparator)) {
                popupMenu.addSeparator();
            }
            popupMenu.add(menuItem);
            if (command.isSeparatorAfter() && i < commands.size() - 1) {
                popupMenu.addSeparator();
            }
        }
    }

    private static boolean isSortedByName(JMenu menu, CommandManager commandManager) {
        Command commandGroup = getCommandForMenu(menu, commandManager);
        return commandGroup != null && commandGroup.getSortChildren();
    }

    private static Command getCommandForMenu(JMenu menu, CommandManager manager) {
        final String name = menu.getName();
        return name != null ? manager.getCommand(name) : null;
    }

    static void sortCommandsByName(List<Command> commands) {
        Collections.sort(commands, NAME_COMPARATOR);
    }

    static void sortCommandsByAnchor(List<Command> commands) {

        ArrayList<Command> firstCommands = new ArrayList<>(commands.size());
        ArrayList<Command> lastCommands = new ArrayList<>(commands.size());
        ArrayList<Command> betweenCommands = new ArrayList<>(commands);
        for (Command command : commands) {
            if (command.getPlaceFirst()) {
                firstCommands.add(command);
                betweenCommands.remove(command);
            } else if (command.getPlaceLast() || (command.getPlaceBefore() == null && command.getPlaceAfter() == null)) {
                lastCommands.add(command);
                betweenCommands.remove(command);
            }
        }

        Assert.state(firstCommands.size() + lastCommands.size() + betweenCommands.size() == commands.size());

        Collections.sort(firstCommands, NAME_COMPARATOR);
        Collections.sort(betweenCommands, NAME_COMPARATOR);
        Collections.sort(lastCommands, NAME_COMPARATOR);

        HashMap<String, Command> commandMap = new HashMap<>(betweenCommands.size() * 2 + 1);
        for (Command command : commands) {
            commandMap.put(command.getCommandID(), command);
        }

        LinkedList<Command> sortedCommands = new LinkedList<>();
        sortedCommands.addAll(firstCommands);
        sortedCommands.addAll(betweenCommands);
        sortedCommands.addAll(lastCommands);

        boolean change = true;
        int iteration;
        for (iteration = 0; change && iteration < commands.size(); iteration++) {

            change = false;
            for (Command command : commands) {

                String placeBefore = command.getPlaceBefore();
                if (placeBefore != null) {
                    int index = sortedCommands.indexOf(command);
                    Assert.state(index >= 0);
                    Command subsequentCommand = commandMap.get(placeBefore);
                    if (subsequentCommand != null) {
                        int subsequentIndex = sortedCommands.indexOf(subsequentCommand);
                        Assert.state(subsequentIndex >= 0);
                        if (subsequentIndex < index) {
                            sortedCommands.remove(command);
                            sortedCommands.add(subsequentIndex, command);
                            change = true;
                        }
                    }
                }

                String placeAfter = command.getPlaceAfter();
                if (placeAfter != null) {
                    int index = sortedCommands.indexOf(command);
                    Assert.state(index >= 0);
                    Command precedingCommand = commandMap.get(placeAfter);
                    if (precedingCommand != null) {
                        int precedingIndex = sortedCommands.indexOf(precedingCommand);
                        Assert.state(precedingIndex >= 0);
                        if (precedingIndex > index) {
                            sortedCommands.add(precedingIndex + 1, command);
                            sortedCommands.remove(index);
                            change = true;
                        }
                    }
                }
            }
        }

        if (iteration > 1 && iteration == commands.size()) {
            BeamLogManager.getSystemLogger().warning(String.format("Sorting updated command list took %d iterations!", iteration));
        }

        commands.clear();
        commands.addAll(sortedCommands);
    }

    private static class CommandNameComparator implements Comparator<Command> {
        @Override
        public int compare(Command command1, Command command2) {
            if (command1.getText() == null || command2.getText() == null) {
                return command1.getCommandID().compareTo(command2.getCommandID());
            }
            return command1.getText().compareTo(command2.getText());
        }
    }

}
