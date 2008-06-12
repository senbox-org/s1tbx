/*
 * $Id: DockableComponent.java,v 1.1 2006/10/10 14:47:35 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
