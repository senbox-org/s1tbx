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


import java.util.EventListener;


/**
 * The listener for receiving command events from <code>ExecCommand</code>s, <code>ToolCommand</code>s and
 * <code>CommandGroup</code>s.
 * <p/>
 * <p>This listener contains the <code>updateState</code> method which is called each time a command should check and
 * eventually update its state. When you create a command (or command group) you provide this listener.
 */
public interface CommandStateListener extends EventListener {

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether this
     * item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    void updateState(CommandEvent event);
}
