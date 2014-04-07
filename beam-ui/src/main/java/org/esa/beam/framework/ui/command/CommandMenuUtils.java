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


import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The <code>CommandMenuUtils</code> is a helper class
 * to build up menus.
 *
 * @author Norman Fomferra
 */
public class CommandMenuUtils {

    private CommandMenuUtils() {
    }

    /**
     * Inserts the given <code>command</command> into the <code>menu</code>.
     *
     * @param menu           The  menu to add the given <code>command</code> to.
     * @param command        The command to insert.
     * @param commandManager The command manager.
     */
    public static void insertCommandMenuItem(JMenu menu, Command command, CommandManager commandManager) {
        Command commandGroup = getCommandForComponent(menu, commandManager);
        Boolean sortChildren;
        if (commandGroup != null) {
            sortChildren = commandGroup.getSortChildren();
        } else {
            sortChildren = null;
        }
        insertCommandMenuItem(menu.getPopupMenu(), command, commandManager, sortChildren != null ? sortChildren : false);
    }


    private static void insertCommandMenuItem(final JPopupMenu popupMenu, final Command command, final CommandManager manager, boolean sortChildren) {
        List<Command> commands = getCommands(popupMenu, command, manager);
        if (sortChildren) {
            sortChildrenAlphabetically(commands);
        } else {
            sortAccordingToPlaceBeforeAndPlaceAfter(commands);
        }

        Map<String, Component> popupMenus = new HashMap<>();
        int count = popupMenu.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component component = popupMenu.getComponent(i);
            if (component instanceof JMenu) {
                popupMenus.put(component.getName(), component);
            }
        }

        popupMenu.removeAll();
        for (Command command1 : commands) {
            insertCommandMenuItem(popupMenu, command1, popupMenu.getComponentCount());
        }
        count = popupMenu.getComponentCount();
        for (int i = 0; i < count; i++) {
            Component component = popupMenu.getComponent(i);
            if (component instanceof JMenu) {
                String name = component.getName();
                Object o = popupMenus.get(name);
                if (o != null) {
                    popupMenu.remove(i);
                    popupMenu.insert((Component) o, i);
                }
            }
        }
    }

    private static void insertCommandMenuItem(final JPopupMenu popupMenu, final Command command, final int pos) {
        JMenuItem menuItem = command.createMenuItem();
        if (menuItem == null) {
            return;
        }

        int insertPos = pos;

        if (command.isSeparatorBefore() && insertPos > 0) {
            if (insertPos == popupMenu.getComponentCount()) {
                Component c = popupMenu.getComponent(insertPos - 1);
                if (!(c instanceof JSeparator)) {
                    popupMenu.addSeparator();
                    insertPos++;
                }
            } else {
                Component c = popupMenu.getComponent(insertPos);
                if (!(c instanceof JSeparator)) {
                    popupMenu.insert(new JPopupMenu.Separator(), insertPos);
                    insertPos++;
                }
            }
        }

        if (insertPos >= popupMenu.getComponentCount()) {
            popupMenu.add(menuItem);
        } else {
            popupMenu.insert(menuItem, insertPos);
        }
        insertPos++;

        if (command.isSeparatorAfter()) {
            if (insertPos == popupMenu.getComponentCount()) {
                popupMenu.addSeparator();
            } else {
                Component c = popupMenu.getComponent(insertPos);
                if (!(c instanceof JSeparator)) {
                    popupMenu.insert(new JPopupMenu.Separator(), insertPos);
                }
            }
        }
    }


    static void sortAccordingToPlaceBeforeAndPlaceAfter(List<Command> commands) {

        Map<String, CommandWrapper> wrappersMap = new HashMap<>(2 * commands.size() + 1);
        for (Command command : commands) {
            CommandWrapper wrapper = new CommandWrapper(command);
            wrappersMap.put(wrapper.getName(), wrapper);
        }
        for (final Command command : commands) {
            String placeBefore = command.getPlaceBefore();
            String placeAfter = command.getPlaceAfter();
            if (placeAfter != null || placeBefore != null) {
                CommandWrapper commandWrapper = wrappersMap.get(command.getCommandID());
                if (placeAfter != null) {
                    CommandWrapper beforeCommand = wrappersMap.get(placeAfter);
                    if (beforeCommand != null) {
                        commandWrapper.addBefore(beforeCommand);
                        beforeCommand.addAfter(commandWrapper);
                    }
                }
                if (placeBefore != null) {
                    CommandWrapper afterCommand = wrappersMap.get(placeBefore);
                    if (afterCommand != null) {
                        commandWrapper.addAfter(afterCommand);
                        afterCommand.addBefore(commandWrapper);
                    }
                }
            }
        }

        List<Command> sortedCommandsList = new ArrayList<>(commands.size());
        while (!wrappersMap.isEmpty()) {
            for (Command command : commands) {
                CommandWrapper wrapper = wrappersMap.get(command.getCommandID());
                if (wrapper != null) {
                    while (!wrapper.beforeWrappers.isEmpty()) {
                        List linksBefore = wrapper.beforeWrappers;
                        for (Object aLinksBefore : linksBefore) {
                            CommandWrapper cw = (CommandWrapper) aLinksBefore;
                            Object o = wrappersMap.get(cw.getName());
                            if (o != null) {
                                wrapper = (CommandWrapper) o;
                                break;
                            }
                        }
                    }
                    sortedCommandsList.add(wrapper.command);
                    wrappersMap.remove(wrapper.getName());
                    appendSorted(sortedCommandsList, wrapper, wrappersMap);
                }
            }
        }
        commands.clear();
        commands.addAll(sortedCommandsList);
    }

    private static void appendSorted(final List<Command> commandsList,
                                     final CommandWrapper wrapper,
                                     final Map<String, CommandWrapper> wrappers) {
        if (!wrapper.afterWrappers.isEmpty()) {
            final List<CommandWrapper> afterWrappersList = wrapper.afterWrappers;
            final CommandWrapper[] afterWrappers = afterWrappersList.toArray(
                    new CommandWrapper[afterWrappersList.size()]);
            for (CommandWrapper afterWrapper : afterWrappers) {
                for (int j = 0; j < afterWrapper.afterWrappers.size(); j++) {
                    CommandWrapper ownAfterWrapper = afterWrapper.afterWrappers.get(j);
                    if (afterWrappersList.contains(ownAfterWrapper)) {
                        afterWrappersList.remove(afterWrapper);
                        final int insertIndex = afterWrappersList.indexOf(ownAfterWrapper);
                        afterWrappersList.add(insertIndex, afterWrapper);
                    }
                }
            }
            for (CommandWrapper anAfterWrapper : afterWrappersList) {
                final CommandWrapper cwFromMap = wrappers.get(anAfterWrapper.getName());
                if (cwFromMap != null) {
                    commandsList.add(cwFromMap.command);
                    wrappers.remove(cwFromMap.getName());
                }
            }
            for (CommandWrapper anAfterWrapper : afterWrappersList) {
                appendSorted(commandsList, anAfterWrapper, wrappers);
            }
        }
    }

    private static List<Command> getCommands(final JPopupMenu popupMenu, final Command command, final CommandManager manager) {
        List<Command> commands = getCommandsFromMenu(popupMenu, manager);
        commands.add(command);
        return commands;
    }

    private static List<Command> getCommandsFromMenu(JPopupMenu popupMenu, CommandManager manager) {
        int count = popupMenu.getComponentCount();
        List<Command> commands = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final Component component = popupMenu.getComponent(i);
            Command menuCommand = getCommandForComponent(component, manager);
            if (menuCommand != null) {
                commands.add(menuCommand);
            }
        }
        return commands;
    }

    private static Command getCommandForComponent(Component component, CommandManager manager) {
        Command menuCommand = null;
        if (component instanceof JMenuItem) {
            final JMenuItem item = (JMenuItem) component;
            final String name = item.getName();
            menuCommand = manager.getCommand(name);
        }
        return menuCommand;
    }

    static void sortChildrenAlphabetically(List<Command> commands) {
        Collections.sort(commands, new Comparator<Command>() {
            @Override
            public int compare(Command o1, Command o2) {
                if (o1.getText() == null || o2.getText() == null) {
                    return o1.getCommandID().compareTo(o2.getCommandID());
                }
                return o1.getText().compareTo(o2.getText());
            }
        });
    }


    private static final class CommandWrapper {

        private final Command command;
        private final List<CommandWrapper> beforeWrappers;
        private final List<CommandWrapper> afterWrappers;

        private CommandWrapper(final Command command) {
            this.command = command;
            beforeWrappers = new ArrayList<>();
            afterWrappers = new ArrayList<>();
        }

        private String getName() {
            return command.getCommandID();
        }

        private void addBefore(final CommandWrapper before) {
            if (before == null) {
                return;
            }
            if (!beforeWrappers.contains(before)) {
                beforeWrappers.add(before);
            }
        }

        private void addAfter(final CommandWrapper after) {
            if (after == null) {
                return;
            }
            if (!afterWrappers.contains(after)) {
                afterWrappers.add(after);
            }
        }
    }
}
