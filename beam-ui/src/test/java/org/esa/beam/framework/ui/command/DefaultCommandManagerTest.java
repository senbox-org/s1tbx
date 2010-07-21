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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DefaultCommandManagerTest extends TestCase {

    private DefaultCommandManager _manager;

    public DefaultCommandManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultCommandManagerTest.class);
    }

    @Override
    public void setUp() {
        _manager = new DefaultCommandManager();
    }

    @Override
    public void tearDown() {
        _manager = null;
    }

    public void testCreateOneCommand() {
        assertEquals(0, _manager.getNumCommands());
        addCommand("test");
        assertEquals(1, _manager.getNumCommands());

        final Command command = _manager.getExecCommand("test");
        assertNotNull(command);
        final Command commandAt = _manager.getCommandAt(0);
        assertSame(command, commandAt);
    }

    public void testCreateMoreCommands() {
        assertEquals(0, _manager.getNumCommands());
        addCommand("test1");
        addCommand("test2");
        addCommand("test3");
        addCommand("test4");

        assertEquals(4, _manager.getNumCommands());

        Command command = null;
        command = _manager.getExecCommand("test2");
        assertNotNull(command);
        Command commandAt = null;
        commandAt = _manager.getCommandAt(1);
        assertSame(command, commandAt);

        command = null;
        command = _manager.getExecCommand("test1");
        assertNotNull(command);
        commandAt = null;
        commandAt = _manager.getCommandAt(0);
        assertSame(command, commandAt);

        command = null;
        command = _manager.getExecCommand("test3");
        assertNotNull(command);
        commandAt = null;
        commandAt = _manager.getCommandAt(2);
        assertSame(command, commandAt);

        command = null;
        command = _manager.getExecCommand("test4");
        assertNotNull(command);
        commandAt = null;
        commandAt = _manager.getCommandAt(3);
        assertSame(command, commandAt);
    }

    private void addCommand(String commandId) {
        ExecCommand command1 = new ExecCommand();
        command1.setCommandID(commandId);
        _manager.addCommand(command1);
    }

    public void testCreateCommandWithConstraintsAndListener() {

        final CommandListener listener = new CommandListener() {

            public void actionPerformed(CommandEvent event) {
            }

            public void updateState(CommandEvent event) {
            }
        };

        ExecCommand command = createCommand("test");
        command.addCommandListener(listener);
        _manager.addCommand(command);

        final Command commandAt = _manager.getCommandAt(0);
        final CommandListener[] listeners = (CommandListener[]) commandAt.getEventListenerList().getListeners(
                CommandListener.class);
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);
    }

    /**
     * Index out of bounds exception wenn der index out of range ist.
     */
    public void testGetCommandAt_IndexOutOfBoundsException() {
        addCommand("test");
        assertNotNull(_manager.getCommandAt(0));
        try {
            _manager.getCommandAt(1);
            fail("IndexOutOfBoundsException not expected");
        } catch (IndexOutOfBoundsException e) {
            // Exception expected
        }
    }

    private static ExecCommand createCommand(String commandId) {
        ExecCommand command = new ExecCommand();
        command.setCommandID(commandId);
        return command;
    }

    /**
     * Die properties werden beim erzeugen des commands gleich initialisiert
     */
//    public void testCreateCommandWithConstraints() {
//        //loads the TEST.properties for this test
//        final ResourceBundle bundle = ResourceBundle.getBundle("org.esa.beam.framework.swing.command.TEST");
//        final CommandConstraints constraints = new CommandConstraints();
//        constraints.addValue(Action.PROCESSOR_NAME, bundle.getString("action.test.text"));
//        constraints.addValue(Action.MNEMONIC_KEY, bundle.getString("action.test.mnemonic"));
//        constraints.addValue(Action.SHORT_DESCRIPTION, bundle.getString("action.test.shortdescr"));
//        constraints.addValue(Action.LONG_DESCRIPTION, bundle.getString("action.test.longdescr"));
//        constraints.addValue(Action.ACCELERATOR_KEY, bundle.getString("action.test.accelerator"));
//        constraints.addValue(Action.SMALL_ICON, bundle.getString("action.test.smallicon"));
//        constraints.addValue(ExecCommand.LARGE_ICON_ACTION_KEY, bundle.getString("action.test.largeicon"));
//
//        _manager.createExecCommand("test", constraints, null);
//
//        final Command testCommand = _manager.getExecCommand("test");
//        assertNotNull(testCommand);
//        //for all tested properties take a look at TEST.propoerties
//        assertEquals("Testaction", testCommand.getText());
//        assertEquals(65, testCommand.getMnemonic()); // 65 is equivalent to Char 'A'
//        assertNotNull(testCommand.getAccelerator());
//        assertEquals(KeyStroke.getKeyStroke("control A"), testCommand.getAccelerator());
//        assertSame(KeyStroke.getKeyStroke("control A"), testCommand.getAccelerator());
//        assertEquals("Testaction do nothing", testCommand.getShortDescription());
//        assertEquals("Testaction is for test only", testCommand.getLongDescription());
//        assertNotNull(testCommand.getLargeIcon());
//        assertNotNull(testCommand.getSmallIcon());
//    }

//    public void testCreateCommandGroup() {
//        fail("fail");
//    }

//    Was wollen wir noch

}
