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


import javax.swing.JMenu;

/**
 * Implements a strategy how new commands are inserted into an existing <code>JMenu</code>.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface CommandMenuInserter {
    /**
     * Inserts the given <code>command</command> into the existing <code>menu</code>.
     *
     * @param newCommand The new command to insert into <code>menu</code>.
     * @param menu       The menu to add the <code>newCommand</code> to.
     */
    void insertCommandIntoMenu(Command newCommand, JMenu menu);
}
