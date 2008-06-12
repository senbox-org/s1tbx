/*
 * $Id: CommandMenuUtils.java,v 1.1 2006/10/10 14:47:36 norman Exp $
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


import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.esa.beam.framework.ui.UIUtils;

// @todo 1 nf/nf - place class API docu here

/**
 * The <code>CommandMenuUtils</code> is a ...
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class CommandMenuUtils {

    protected CommandMenuUtils() {
    }

    public static void insertCommandMenuItem(final JPopupMenu popupMenu, final Command command,
                                             final CommandManager manager) {
        Command[] commands = getCommands(popupMenu, manager, command);
        commands = sort(commands);

        final HashMap popupMenues = new HashMap();
        int count = popupMenu.getComponentCount();
        for (int i = 0; i < count; i++) {
            final Component component = popupMenu.getComponent(i);
            if (component instanceof JMenu) {
                popupMenues.put(component.getName(), component);
            }
        }

        popupMenu.removeAll();
        for (int i = 0; i < commands.length; i++) {
            insertCommandMenuItem(popupMenu, commands[i], popupMenu.getComponentCount());
        }

        // se - i dont understand for which purpose is this iteration
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

    public static int findMenuInsertPosition(final JPopupMenu popupMenu, final Command command,
                                             final CommandManager manager) {
        int pbi = popupMenu.getComponentCount();
        int pai = -1;
        String placeBefore = command.getPlaceBefore();
        if (placeBefore != null) {
            pbi = UIUtils.findMenuItemPosition(popupMenu, placeBefore);
        }
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

    static Command[] sort(final Command[] commands) {
//        final List keyList = new ArrayList();
//        final List commandList = new ArrayList();
//
//        for (int i = 0; i < commands.length; i++) {
//            Command unsortedCommand = commands[i];
//            if (i == 0) {
//                keyList.add(unsortedCommand.getCommandID());
//                commandList.add(unsortedCommand);
//            } else {
//                final String placeBefore = unsortedCommand.getPlaceBefore();
//                final int beforeIndex;
//                if (keyList.contains(placeBefore)) {
//                    beforeIndex = keyList.indexOf(placeBefore);
//                } else {
//                    beforeIndex = -1;
//                }
//                final String placeAfter = unsortedCommand.getPlaceBefore();
//                final int afterIndex;
//                if (keyList.contains(placeAfter)) {
//                    afterIndex = keyList.indexOf(placeAfter);
//                } else {
//                    afterIndex = -1;
//                }
//                if
//            }
//        }
//        return (Command[]) commandList.toArray(new Command[0]);
        final List sortedCommandsList = new ArrayList();
        if (commands != null) {
            final Map wrappersMap = new HashMap();
            for (int i = 0; i < commands.length; i++) {
                final Command command = commands[i];
                final CommandWrapper wrapper = new CommandWrapper(command);
                wrappersMap.put(wrapper.getName(), wrapper);
            }
            for (int i = 0; i < commands.length; i++) {
                final Command command = commands[i];
                final String placeBefore = command.getPlaceBefore();
                final String placeAfter = command.getPlaceAfter();
                if (placeAfter != null || placeBefore != null) {
                    final CommandWrapper commandWrapper = (CommandWrapper) wrappersMap.get(command.getCommandID());
                    if (placeAfter != null) {
                        final CommandWrapper beforeCommand = (CommandWrapper) wrappersMap.get(placeAfter);
                        if (beforeCommand != null) {
                            commandWrapper.addBefore(beforeCommand);
                            beforeCommand.addAfter(commandWrapper);
                        }
                    }
                    if (placeBefore != null) {
                        final CommandWrapper afterCommand = (CommandWrapper) wrappersMap.get(placeBefore);
                        if (afterCommand != null) {
                            commandWrapper.addAfter(afterCommand);
                            afterCommand.addBefore(commandWrapper);
                        }
                    }
                }
            }
            while (!wrappersMap.isEmpty()) {
                CommandWrapper wrapper = (CommandWrapper) wrappersMap.values().iterator().next();
                while (!wrapper.beforeWrappers.isEmpty()) {
                    final List linksBefore = wrapper.beforeWrappers;
                    for (int i = 0; i < linksBefore.size(); i++) {
                        final CommandWrapper cw = (CommandWrapper) linksBefore.get(i);
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
        }
        return (Command[]) sortedCommandsList.toArray(new Command[sortedCommandsList.size()]);
    }

    private static void appendSorted(final List commandsList, final CommandWrapper wrapper, final Map wrappers) {
        if (!wrapper.afterWrappers.isEmpty()) {
            final List afterWrappersList = wrapper.afterWrappers;
            final CommandWrapper[] afterWrappers = (CommandWrapper[]) afterWrappersList.toArray(new CommandWrapper[0]);
            for (int i = 0; i < afterWrappers.length; i++) {
                CommandWrapper afterWrapper = afterWrappers[i];
                for (int j = 0; j < afterWrapper.afterWrappers.size(); j++) {
                    CommandWrapper ownAfterWrapper = (CommandWrapper) afterWrapper.afterWrappers.get(j);
                    if (afterWrappersList.contains(ownAfterWrapper)) {
                        afterWrappersList.remove(afterWrapper);
                        final int insertIndex = afterWrappersList.indexOf(ownAfterWrapper);
                        afterWrappersList.add(insertIndex, afterWrapper);
                    }
                }
            }
            for (int i = 0; i < afterWrappersList.size(); i++) {
                final CommandWrapper cw = (CommandWrapper) afterWrappersList.get(i);
                final CommandWrapper cwFromMap = (CommandWrapper) wrappers.get(cw.getName());
                if (cwFromMap != null) {
                    commandsList.add(cwFromMap.command);
                    wrappers.remove(cwFromMap.getName());
                }
            }
            for (int i = 0; i < afterWrappersList.size(); i++) {
                final CommandWrapper cw = (CommandWrapper) afterWrappersList.get(i);
                appendSorted(commandsList, cw, wrappers);
            }
        }
    }

    private static Command[] getCommands(final JPopupMenu popupMenu, final CommandManager manager,
                                         final Command command) {
        final List commands = new ArrayList();
        final int count = popupMenu.getComponentCount();
        for (int i = 0; i < count; i++) {
            final Component component = popupMenu.getComponent(i);
            if (!(component instanceof JMenuItem)) {
                continue;
            }
            final JMenuItem item = (JMenuItem) component;
            final String name = item.getName();
            final Command menuCommand = manager.getCommand(name);
            if (menuCommand != null) {
                commands.add(menuCommand);
            }
        }
        commands.add(command);
        return (Command[]) commands.toArray(new Command[commands.size()]);
    }

    private static final class CommandWrapper {

        private final Command command;
        private final List beforeWrappers;
        private final List afterWrappers;

        private CommandWrapper(final Command command) {
            this.command = command;
            beforeWrappers = new ArrayList();
            afterWrappers = new ArrayList();
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
