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


// @todo 1 nf/nf - place class API docu here

/**
 * The <code>CommandAdapter</code> is a ...
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class CommandAdapter implements CommandListener {

    public CommandAdapter() {
    }

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    public void actionPerformed(CommandEvent event) {
    }

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether this
     * item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    public void updateState(CommandEvent event) {
    }
}
