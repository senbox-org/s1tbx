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


/**
 * A command manager is used to create, register and deregister a list of commands. It also provides a set operations
 * which are applied to all commands in the list.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface CommandManager {

    /**
     * Creates a new executable command for the given unique command ID and the given command listener.
     *
     * @param commandID a unique command ID
     * @param listener  the command listener which executes the command and updates its state
     *
     * @return a new executable command
     *
     * @see #createToolCommand
     * @see #createCommandGroup
     */
    ExecCommand createExecCommand(String commandID, CommandListener listener);

    /**
     * Creates a new tool command for the given unique command ID and the given tool.
     *
     * @param commandID a unique command ID
     * @param listener  the command state listener used to update the tool command's state
     * @param tool      the tool which executes the command and updates its state
     *
     * @return a new tool command
     *
     * @see #createExecCommand
     * @see #createCommandGroup
     */
    ToolCommand createToolCommand(String commandID, CommandStateListener listener, Interactor tool);

    /**
     * Creates a new command group command for the given unique command ID and the given command state listener.
     *
     * @param commandID a unique command ID
     * @param listener  the command state listener used to update the command group state
     *
     * @return a new command group
     *
     * @see #createExecCommand
     * @see #createToolCommand
     */
    CommandGroup createCommandGroup(String commandID, CommandStateListener listener);


    /**
     * Returns the number of commands in this <code>DefaultCommandManager</code>. If this manager contains more than
     * <tt>Integer.MAX_VALUE</tt> elements, returns <tt>Integer.MAX_VALUE</tt> :-)
     *
     * @return the number of commands in this <code>DefaultCommandManager</code>.
     */
    int getNumCommands();

    /**
     * Returns the element at the specified position in this <code>DefaultCommandManager</code>.
     *
     * @param index index of command to return.
     *
     * @return the <code>Command</code> at the specified position in this <code>DefaultCommandManager</code>.
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= getNumCommands()).
     */
    Command getCommandAt(int index);

    /**
     * Gets the command associated with the given command-COMMAND_ID or <code>null</code> if a command with the given command-ID
     * has not been registered (so far).
     */
    Command getCommand(String commandID);

    /**
     * Gets the executable command associated with the given command-ID or <code>null</code> if an executable command
     * with the given command-ID has not been registered (so far).
     */
    ExecCommand getExecCommand(String commandID);

    /**
     * Gets the tool command associated with the given command-ID or <code>null</code> if a tool command with the given
     * command-ID has not been registered (so far).
     */
    ToolCommand getToolCommand(String commandID);

    /**
     * Gets the command group associated with the given command-ID or <code>null</code> if an command group with the
     * given command-ID has not been registered (so far).
     */
    CommandGroup getCommandGroup(String commandID);

    /**
     * Calls the <code>updateState</code> method of all registered commands.
     */
    void updateState();

    /**
     * Updates the component tree of all commands since the Java look-and-feel has changed.
     */
    void updateComponentTreeUI();

    /**
     * Adds a new command to this command manager.
     *
     * @param command the command to be added
     *
     * @throws IllegalArgumentException if the command ID property of the command has not been set, or if an command
     *                                  with the same command ID has alreay been registered
     */
    void addCommand(Command command);

    /**
     * Removes an existing command from this command manager.
     *
     * @param command the command to be removed
     */
    void removeCommand(Command command);
}
