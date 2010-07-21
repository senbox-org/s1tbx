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
package com.bc.swing.dock;

/**
 * A component which can either be "docked" in a parent component or
 * be displayed "floating" in a separate window.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface DockableComponent {

    /**
     * Tests if this component is docked or floating.
     * @return true if docked, false if floating
     */
    boolean isDocked();

    /**
     * Sets the docked state of this component.
     * @param docked true if it shall dock, false if shall float
     */
    void setDocked(boolean docked);
}
