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

import java.awt.event.ActionEvent;

import javax.swing.event.ChangeEvent;

// @todo 1 nf/nf - place class API docu here

/**
 * The <code>CommandEvent</code> is a ...
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class CommandEvent extends ChangeEvent {

    private Command _command;
    private ActionEvent _actionEvent;
    private Object _contextObject;
    private Object _argument;

    public CommandEvent(Command command, ActionEvent actionEvent, Object contextObject, Object argument) {
        super(actionEvent != null ? actionEvent.getSource() : command);
        _command = command;
        _actionEvent = actionEvent;
        _argument = argument;
    }

    public ActionEvent getActionEvent() {
        return _actionEvent;
    }

    public Command getCommand() {
        return _command;
    }

    public SelectableCommand getSelectableCommand() {
        return (_command instanceof SelectableCommand) ? (SelectableCommand) _command : null;
    }

    public CommandGroup getCommandGroup() {
        return (_command instanceof CommandGroup) ? (CommandGroup) _command : null;
    }

    public Object getContextObject() {
        return _contextObject;
    }

    public Object getArgument() {
        return _argument;
    }

    @Override
    public String toString() {
        return getClass().getName()
               + "[_command="
               + _command
               + ",_actionEvent="
               + _actionEvent
               + ",_argument="
               + _argument
               + "]";
    }
}
