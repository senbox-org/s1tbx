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

import com.bc.ceres.swing.figure.Interactor;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>DefaultCommandManager</code> provides a default implementation for the <code>CommandManager</code>
 * interface.
 * <p/>
 * <p>It also provides a simple mechanism to configure <code>Command</code> items with the key/value pairs stored in a
 * resource bundle.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 * @see Command
 * @see java.util.ResourceBundle
 */
public class DefaultCommandManager implements CommandManager {

    /**
     * Stores all commands registered in this applications. Provided for fast key based access.
     */
    private final Map<String, Command> commandMap = new HashMap<String, Command>();

    /**
     * Stores all commands registered in this applications. Provided for command order and index based access.
     */
    private final List<Command> commandList = new ArrayList<Command>();

    /**
     * Creates a new executable command for the given unique command ID and the given command listener.
     * Finally the created command is added to internal list of registered commands.
     *
     * @param commandID a unique command ID
     * @param listener  the command listener which executes the command and updates its state
     *
     * @return a new executable command
     *
     * @see #createToolCommand
     * @see #createCommandGroup
     * @see #addCommand(Command)
     */
    @Override
    public ExecCommand createExecCommand(String commandID, CommandListener listener) {
        Guardian.assertNotNullOrEmpty("commandID", commandID);
        ExecCommand command = new ExecCommand(commandID, listener);
        addCommand(command);
        return command;
    }

    /**
     * Creates a new tool command for the given unique command ID an the given tool.
     * Finally the created command is added to internal list of registered commands.
     *
     * @param commandID a unique command ID
     * @param listener  the command state listener used to update the tool command's state
     * @param tool      the tool which executes the command and updates its state
     *
     * @return a new tool command
     *
     * @see #createExecCommand
     * @see #createCommandGroup
     * @see #addCommand(Command)
     */
    @Override
    public ToolCommand createToolCommand(String commandID, CommandStateListener listener, Interactor tool) {
        Guardian.assertNotNullOrEmpty("commandID", commandID);
        Guardian.assertNotNull("tool", tool);
        ToolCommand command = new ToolCommand(commandID, listener, tool);
        addCommand(command);
        return command;
    }

    /**
     * Creates a new command group command for the given unique command ID an the given command state listener.
     * Finally the created command group is added to internal list of registered commands.
     *
     * @param commandGroupID a unique command group ID
     * @param listener       the command state listener used to update the command group state
     *
     * @return a new command group
     *
     * @see #createExecCommand
     * @see #createToolCommand
     * @see #addCommand(Command)
     */
    @Override
    public CommandGroup createCommandGroup(String commandGroupID, CommandStateListener listener) {
        Guardian.assertNotNullOrEmpty("commandGroupID", commandGroupID);
        CommandGroup commandGroup = new CommandGroup(commandGroupID, listener);
        addCommand(commandGroup);
        return commandGroup;
    }

    /**
     * Returns the number of commands in this <code>DefaultCommandManager</code>. If this manager contains more than
     * <tt>Integer.MAX_VALUE</tt> elements, returns <tt>Integer.MAX_VALUE</tt> :-)
     *
     * @return the number of commands in this <code>DefaultCommandManager</code>.
     */
    @Override
    public int getNumCommands() {
        return commandList.size();
    }


    /**
     * Returns the element at the specified position in this <code>DefaultCommandManager</code>.
     *
     * @param index index of command to return.
     *
     * @return the <code>Command</code> at the specified position in this <code>DefaultCommandManager</code>.
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= getNumCommands()).
     */
    @Override
    public Command getCommandAt(int index) {
        return commandList.get(index);
    }


    /**
     * Gets the command associated with the given command-COMMAND_ID or <code>null</code> if a command with the given command-ID
     * has not been registered (so far).
     */
    @Override
    public Command getCommand(String commandID) {
        return commandMap.get(commandID);
    }

    /**
     * Gets the command associated with the given command-ID or <code>null</code> if an command with the given
     * command-ID has not been registered.
     */
    @Override
    public ExecCommand getExecCommand(String commandID) {
        final Command command = getCommand(commandID);
        if (command instanceof ExecCommand) {
            return (ExecCommand) command;
        }
        return null;
    }

    /**
     * Gets the tool command associated with the given command-ID or <code>null</code> if a tool command with the given
     * command-ID has not been registered.
     */
    @Override
    public ToolCommand getToolCommand(String commandID) {
        final Command command = getCommand(commandID);
        if (command instanceof ToolCommand) {
            return (ToolCommand) command;
        }
        return null;
    }

    /**
     * Gets the command group associated with the given command-ID or <code>null</code> if an command group with the
     * given command-ID has not been registered.
     */
    @Override
    public CommandGroup getCommandGroup(String commandID) {
        Command command = getCommand(commandID);
        if (command instanceof CommandGroup) {
            return (CommandGroup) command;
        }
        return null;
    }

    /**
     * Calls the <code>updateState</code> method of all registered commands.
     */
    @Override
    public void updateState() {
        final int n = getNumCommands();
        for (int i = 0; i < n; i++) {
            getCommandAt(i).updateState();
        }
    }

    /**
     * Updates the component tree of all commands since the Java look-and-feel has changed.
     */
    @Override
    public void updateComponentTreeUI() {
        final int n = getNumCommands();
        for (int i = 0; i < n; i++) {
            getCommandAt(i).updateComponentTreeUI();
        }
    }

    /**
     * Adds a new command to this command manager.
     *
     * @param command the command to be added
     *
     * @throws IllegalArgumentException if the command ID property of the command has not been set, or if an command
     *                                  with the same command ID has alreay been registered
     */
    @Override
    public void addCommand(Command command) {
        if (commandMap.containsKey(command.getCommandID())) {
            throw new IllegalArgumentException(
                    "a command named '" + command.getCommandID() + "' is already registered");
        }
        commandMap.put(command.getCommandID(), command);
        commandList.add(command);
        Debug.trace("DefaultCommandManager: added command '" + command + "'");
    }

    /**
     * Removes an existing command from this command manager.
     *
     * @param command the command to be removed
     */
    @Override
    public void removeCommand(Command command) {
        String commandKey = command.getCommandID();
        commandMap.remove(commandKey);
        commandList.remove(command);
    }

}
