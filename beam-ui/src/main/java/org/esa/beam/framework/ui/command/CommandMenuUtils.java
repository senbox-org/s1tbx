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


import org.esa.beam.framework.ui.UIUtils;

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
 * @version $Revision$  $Date$
 */
public class CommandMenuUtils {

    protected CommandMenuUtils() {
    }

    /**
     * Inserts the given <code>command</command> at the end of the <code>popupMenu</code>.
     *
     * @param popupMenu The popup menu to add the given <code>command</code> to.
     * @param command   The command to insert.
     * @param manager   The command manager.
     *
     * @deprecated since BEAM 4.10. Use {@link CommandMenuUtils#insertCommandMenuItem(Boolean, JPopupMenu, Command, CommandManager)} instead.
     */
    public static void insertCommandMenuItem0(final JPopupMenu popupMenu, final Command command,
                                              final CommandManager manager) {
    }

    /**
     * Inserts the given <code>command</command> at the end of the <code>popupMenu</code>.
     *
     * @param sortChildren Whether to sort the children alphabetically.
     * @param popupMenu    The popup menu to add the given <code>command</code> to.
     * @param command      The command to insert.
     * @param manager      The command manager.
     */
    public static void insertCommandMenuItem(Boolean sortChildren, final JPopupMenu popupMenu, final Command command,
                                             final CommandManager manager) {
        Command[] commands = getCommands(popupMenu, manager, command);
        if (sortChildren != null && sortChildren) {
            commands = sortChildrenAlphabetically(commands);
        } else {
            commands = sortAccordingToPlaceBeforeAndPlaceAfter(commands);
        }

        final Map<String, Component> popupMenues = new HashMap<String, Component>();
        int count = popupMenu.getComponentCount();
        for (int i = 0; i < count; i++) {
            final Component component = popupMenu.getComponent(i);
            if (component instanceof JMenu) {
                popupMenues.put(component.getName(), component);
            }
        }

        popupMenu.removeAll();
        for (Command command1 : commands) {
            insertCommandMenuItem(popupMenu, command1, popupMenu.getComponentCount());
        }
        count = popupMenu.getComponentCount();
        for (int i = 0; i < count; i++) {
            final Component component = popupMenu.getComponent(i);
            if (component instanceof JMenu) {
                final String name = component.getName();
                final Object o = popupMenues.get(name);
                if (o != null) {
                    popupMenu.remove(i);
                    popupMenu.insert((Component) o, i);
                }
            }
        }
    }

    static Command[] sortChildrenAlphabetically(Command[] commands) {
        final List<Command> sortedCommandsList = new ArrayList<Command>();
        Collections.addAll(sortedCommandsList, commands);
        Collections.sort(sortedCommandsList, new Comparator<Command>() {
            @Override
            public int compare(Command o1, Command o2) {
                if(o1.getText() == null || o2.getText() == null) {
                    return o1.getCommandID().compareTo(o2.getCommandID());
                }
                return o1.getText().compareTo(o2.getText());
            }
        });
        commands = sortedCommandsList.toArray(new Command[sortedCommandsList.size()]);
        return commands;
    }

    /**
     * Finds the insert position of the given <code>command</command> within the <code>popupMenu</code>.
     *
     * @param popupMenu The popup menu.
     * @param command   The command to insert.
     * @param manager   The command manager.
     */
    public static int findMenuInsertPosition(final JPopupMenu popupMenu, final Command command,
                                             final CommandManager manager) {
        int pbi = popupMenu.getComponentCount();
        String placeBefore = command.getPlaceBefore();
        if (placeBefore != null) {
            pbi = UIUtils.findMenuItemPosition(popupMenu, placeBefore);
        }
        int pai = -1;
        String placeAfter = command.getPlaceAfter();
        if (placeAfter != null) {
            pai = UIUtils.findMenuItemPosition(popupMenu, placeAfter) + 1;
        }
        final int componentCount = popupMenu.getComponentCount();
        for (int i = 0; i < componentCount; i++) {
            final Component component = popupMenu.getComponent(i);
            if (!(component instanceof JMenuItem)) {
                continue;
            }
            final JMenuItem item = (JMenuItem) component;
            final String name = item.getName();
            final Command menuCommand = manager.getCommand(name);
            if (menuCommand == null) {
                continue;
            }
            placeBefore = menuCommand.getPlaceBefore();
            if (command.getCommandID().equals(placeBefore)) {
                if (pbi > i) {
                    pbi = i + 1;
                }
            }
            placeAfter = menuCommand.getPlaceAfter();
            if (command.getCommandID().equals(placeAfter)) {
                if (pai < i) {
                    pai = i;
                }
            }
        }
        int insertPos = -1;
        if (pbi >= pai) {
            insertPos = pai;
        }
        if (insertPos == -1) {
            insertPos = popupMenu.getComponentCount();
        }
        return insertPos;
    }

    /**
     * Inserts the given <code>command</command> to the <code>popupMenu</code> at the given <code>position</code>.
     *
     * @param popupMenu The popup menu to add the given <code>command</code> to.
     * @param command   The command to insert.
     * @param pos       the position where to insert the <code>command</code>.
     */
    public static void insertCommandMenuItem(final JPopupMenu popupMenu, final Command command, final int pos) {
        final JMenuItem menuItem = command.createMenuItem();
        if (menuItem == null) {
            return;
        }

        int insertPos = pos;

        if (command.isSeparatorBefore() && insertPos > 0) {
            if (insertPos == popupMenu.getComponentCount()) {
                final Component c = popupMenu.getComponent(insertPos - 1);
                if (!(c instanceof JSeparator)) {
                    popupMenu.addSeparator();
                    insertPos++;
                }
            } else {
                final Component c = popupMenu.getComponent(insertPos);
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
                final Component c = popupMenu.getComponent(insertPos);
                if (!(c instanceof JSeparator)) {
                    popupMenu.insert(new JPopupMenu.Separator(), insertPos);
                }
            }
        }
    }

    static Command[] sortAccordingToPlaceBeforeAndPlaceAfter(final Command[] commands) {

        final List<Command> sortedCommandsList = new ArrayList<Command>();
        final Map<String, CommandWrapper> wrappersMap = new HashMap<String, CommandWrapper>();
        for (final Command command : commands) {
            final CommandWrapper wrapper = new CommandWrapper(command);
            wrappersMap.put(wrapper.getName(), wrapper);
        }
        for (final Command command : commands) {
            final String placeBefore = command.getPlaceBefore();
            final String placeAfter = command.getPlaceAfter();
            if (placeAfter != null || placeBefore != null) {
                final CommandWrapper commandWrapper = wrappersMap.get(command.getCommandID());
                if (placeAfter != null) {
                    final CommandWrapper beforeCommand = wrappersMap.get(placeAfter);
                    if (beforeCommand != null) {
                        commandWrapper.addBefore(beforeCommand);
                        beforeCommand.addAfter(commandWrapper);
                    }
                }
                if (placeBefore != null) {
                    final CommandWrapper afterCommand = wrappersMap.get(placeBefore);
                    if (afterCommand != null) {
                        commandWrapper.addAfter(afterCommand);
                        afterCommand.addBefore(commandWrapper);
                    }
                }
            }
        }
        while (!wrappersMap.isEmpty()) {
            CommandWrapper wrapper = wrappersMap.values().iterator().next();
            while (!wrapper.beforeWrappers.isEmpty()) {
                final List linksBefore = wrapper.beforeWrappers;
                for (Object aLinksBefore : linksBefore) {
                    final CommandWrapper cw = (CommandWrapper) aLinksBefore;
                    final Object o = wrappersMap.get(cw.getName());
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
        return sortedCommandsList.toArray(new Command[sortedCommandsList.size()]);
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

    private static Command[] getCommands(final JPopupMenu popupMenu, final CommandManager manager,
                                         final Command command) {
        final List<Command> commands = new ArrayList<Command>();
        final int count = popupMenu.getComponentCount();
        for (int i = 0; i < count; i++) {
            final Component component = popupMenu.getComponent(i);
            Command menuCommand = getCommandForComponent(manager, component);
            if (menuCommand != null) {
                commands.add(menuCommand);
            }
        }
        commands.add(command);
        return commands.toArray(new Command[commands.size()]);
    }

    private static Command getCommandForComponent(CommandManager manager, Component component) {
        Command menuCommand = null;
        if (component instanceof JMenuItem) {
            final JMenuItem item = (JMenuItem) component;
            final String name = item.getName();
            menuCommand = manager.getCommand(name);
        }
        return menuCommand;
    }

    public static void insertCommandMenuItem(JMenu menu, Command command, CommandManager commandManager) {
        Command commandGroup = getCommandForComponent(commandManager, menu);
        Boolean sortChildren;
        if(commandGroup == null) {
            sortChildren = false;
        } else {
            sortChildren = commandGroup.getSortChildren();
        }
        insertCommandMenuItem(sortChildren, menu.getPopupMenu(), command, commandManager);
    }

    private static final class CommandWrapper {

        private final Command command;
        private final List<CommandWrapper> beforeWrappers;
        private final List<CommandWrapper> afterWrappers;

        private CommandWrapper(final Command command) {
            this.command = command;
            beforeWrappers = new ArrayList<CommandWrapper>();
            afterWrappers = new ArrayList<CommandWrapper>();
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
