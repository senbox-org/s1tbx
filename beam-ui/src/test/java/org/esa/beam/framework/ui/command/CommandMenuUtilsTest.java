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

import junit.framework.TestCase;

import javax.swing.JPopupMenu;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandMenuUtilsTest extends TestCase {

    public CommandMenuUtilsTest(String name) {
        super(name);
    }

    public void testAddContextDependingMenuItems() {
        DefaultCommandManager manager = new DefaultCommandManager();
        createCommand("com1", manager);
        createCommand("com2", manager);
        createCommand("com3", manager);
        manager.getCommandAt(0).setProperty(Command.ACTION_KEY_CONTEXT, "image");
        manager.getCommandAt(1).setProperty(Command.ACTION_KEY_CONTEXT, "notimage");
        manager.getCommandAt(2).setProperty(Command.ACTION_KEY_CONTEXT, "image");
        manager.getCommandAt(2).setEnabled(false);

        DefaultCommandUIFactory uiFactory = new DefaultCommandUIFactory();
        uiFactory.setCommandManager(manager);
        JPopupMenu menu = uiFactory.addContextDependentMenuItems("image", new JPopupMenu());
        assertEquals(2, menu.getComponentCount());

        menu = uiFactory.addContextDependentMenuItems("notimage", new JPopupMenu());
        assertEquals(1, menu.getComponentCount());
    }

    public void testAddContextDependingMenuItemsOnlyOneTime() {
        DefaultCommandManager manager = new DefaultCommandManager();
        createCommand("com1", manager);
        createCommand("com2", manager);
        createCommand("com3", manager);
        manager.getCommandAt(0).setProperty(Command.ACTION_KEY_CONTEXT, "band");
        manager.getCommandAt(1).setProperty(Command.ACTION_KEY_CONTEXT, new String[]{"band", "virtualBand"});
        manager.getCommandAt(2).setProperty(Command.ACTION_KEY_CONTEXT, "virtualBand");

        DefaultCommandUIFactory uiFactory = new DefaultCommandUIFactory();
        uiFactory.setCommandManager(manager);

        JPopupMenu bandMenu = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("virtualBand", bandMenu);
        assertEquals(2, bandMenu.getComponentCount());

        JPopupMenu virtualBandMenu = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("band", virtualBandMenu);
        assertEquals(2, virtualBandMenu.getComponentCount());

        JPopupMenu bandAndVbMenu = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("band", bandAndVbMenu);
        uiFactory.addContextDependentMenuItems("virtualBand", bandAndVbMenu);
        assertEquals(3, bandAndVbMenu.getComponentCount());
    }

    public void testPlaceAtContextTop() {
        DefaultCommandManager manager = new DefaultCommandManager();
        ExecCommand standardCommand1 = createCommand("com1", manager);
        ExecCommand standardCommand2 = createCommand("com2", manager);
        ExecCommand topCommand = createCommand("com3", manager);
        standardCommand1.setProperty(Command.ACTION_KEY_CONTEXT, "band");
        standardCommand2.setProperty(Command.ACTION_KEY_CONTEXT, "band");
        topCommand.setProperty(Command.ACTION_KEY_CONTEXT, "band");

        topCommand.setPlaceAtContextTop(true);

        DefaultCommandUIFactory uiFactory = new DefaultCommandUIFactory();
        uiFactory.setCommandManager(manager);

        JPopupMenu popup = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("band", popup);
        assertEquals(3, popup.getComponentCount());
        String[] expectedOrder = new String[]{"com3", "com1", "com2",};
        assertEquals(expectedOrder[0], popup.getComponent(0).getName());
        assertEquals(expectedOrder[1], popup.getComponent(1).getName());
        assertEquals(expectedOrder[2], popup.getComponent(2).getName());
    }

    public void testSort() {
        DefaultCommandManager manager = new DefaultCommandManager();
        Command command;

        createCommand("a1", manager);

        command = createCommand("test2", manager);
        command.setPlaceAfter("test1");
        command.setPlaceBefore("test3");

        createCommand("a2", manager);

        command = createCommand("test4", manager);
        command.setPlaceAfter("test3");
        command.setPlaceBefore("test5");

        createCommand("a3", manager);

        command = createCommand("test3", manager);
        command.setPlaceAfter("test2");
        command.setPlaceBefore("test4");

        createCommand("a4", manager);

        command = createCommand("test1", manager);
        command.setPlaceBefore("test2");

        createCommand("a5", manager);

        command = createCommand("test5", manager);
        command.setPlaceAfter("test4");

        createCommand("a6", manager);

        assertEquals(11, manager.getNumCommands());

        ArrayList<Command> commands = getCommands(manager);

        CommandMenuUtils.sortAccordingToPlaceBeforeAndPlaceAfter(commands);
        assertEquals(11, commands.size());

        int sequenceStart = 0;
        for (int i = 0; i < commands.size(); i++) {
            command = commands.get(i);
            if ("test1".equals(command.getCommandID())) {
                sequenceStart = i;
            }
        }

        assertEquals("test1", commands.get(sequenceStart++).getCommandID());
        assertEquals("test2", commands.get(sequenceStart++).getCommandID());
        assertEquals("test3", commands.get(sequenceStart++).getCommandID());
        assertEquals("test4", commands.get(sequenceStart++).getCommandID());
        assertEquals("test5", commands.get(sequenceStart).getCommandID());
    }

    private ArrayList<Command> getCommands(DefaultCommandManager manager) {
        ArrayList<Command> commands = new ArrayList<>();
        for (int i = 0; i < manager.getNumCommands(); i++) {
            Command commandAt = manager.getCommandAt(i);
            assertNotNull(commandAt);
            commands.add(commandAt);
        }
        return commands;
    }

    public void testSortAlphabetically() {
        DefaultCommandManager manager = new DefaultCommandManager();

        createCommand("f", manager);
        createCommand("a", manager);
        createCommand("d", manager);
        createCommand("c", manager);
        createCommand("e", manager);
        createCommand("b", manager);

        ArrayList<Command> commands = getCommands(manager);
        assertEquals(6, commands.size());
        CommandMenuUtils.sortChildrenAlphabetically(commands);
        assertEquals(6, commands.size());

        int index = 0;
        assertEquals("a", commands.get(index++).getCommandID());
        assertEquals("b", commands.get(index++).getCommandID());
        assertEquals("c", commands.get(index++).getCommandID());
        assertEquals("d", commands.get(index++).getCommandID());
        assertEquals("e", commands.get(index++).getCommandID());
        assertEquals("f", commands.get(index).getCommandID());
    }

    public void testCorrectOrdering() {
        ExecCommand beforeCommand = new ExecCommand();
        beforeCommand.setCommandID("before_open");
        ExecCommand openCommand = new ExecCommand();
        openCommand.setCommandID("open");
        ExecCommand reopenCommand = new ExecCommand();
        reopenCommand.setCommandID("reopen");
        ExecCommand afterCommand = new ExecCommand();
        afterCommand.setCommandID("after_reopen");
        beforeCommand.setPlaceBefore(openCommand.getCommandID());
        openCommand.setPlaceAfter(beforeCommand.getCommandID());
        reopenCommand.setPlaceAfter(openCommand.getCommandID());
        afterCommand.setPlaceAfter(reopenCommand.getCommandID());

        ExecCommand betweenCommand = new ExecCommand("between");
        betweenCommand.setPlaceAfter(openCommand.getCommandID());
        betweenCommand.setPlaceBefore(reopenCommand.getCommandID());

        Command[] commands = new Command[]{
                reopenCommand,
                openCommand,
                afterCommand,
                beforeCommand,
                betweenCommand,
        };
        List<Command> sortedCommands = new ArrayList<>(Arrays.asList(commands));
        CommandMenuUtils.sortAccordingToPlaceBeforeAndPlaceAfter(sortedCommands);

        Command[] expectedOrder = new Command[]{
                beforeCommand, openCommand, betweenCommand, reopenCommand, afterCommand
        };
        for (int i = 0; i < expectedOrder.length; i++) {
            assertSame("index=" + i, expectedOrder[i], sortedCommands.get(i));
        }
    }

    private static ExecCommand createCommand(String commandId, DefaultCommandManager manager) {
        ExecCommand command = new ExecCommand();
        command.setCommandID(commandId);
        manager.addCommand(command);
        return command;
    }


}
