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

package org.esa.beam.framework.ui;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * A factory for pop-up menues.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface PopupMenuFactory {

    /**
     * Creates the popup menu for the given component. This method is called by the <code>PopupMenuHandler</code>
     * registered on the given component.
     *
     * @param component the source component
     *
     * @see PopupMenuFactory
     * @see PopupMenuHandler
     */
    JPopupMenu createPopupMenu(Component component);

    /**
     * Creates the popup menu for the given mouse event. This method is called by the <code>PopupMenuHandler</code>
     * registered on the event fired component.
     *
     * @param event the fired mouse event
     *
     * @see PopupMenuFactory
     * @see PopupMenuHandler
     */
    JPopupMenu createPopupMenu(MouseEvent event);

    /**
     * Creates the popup menu for the given key event.
     * This method is called by the <code>PopupMenuHandler</code> registered
     * on the event fired component.
     *
     * @param event the fired key event
     * @see PopupMenuFactory
     * @see PopupMenuHandler
     */
//    JPopupMenu createPopupMenu(KeyEvent event);
}
