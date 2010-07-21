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
 * The <code>CommandListener</code> is a ...
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface CommandListener extends CommandStateListener {

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    void actionPerformed(CommandEvent event);
}
