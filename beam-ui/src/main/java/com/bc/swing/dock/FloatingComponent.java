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

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.WindowListener;

import javax.swing.Icon;

/**
 * Represents a {@link com.bc.swing.dock.DockableComponent} component in the "floating" state.
 * <p>Usually this interface will be implemented by classes which extend
 * some kind of window, e.g. a JDialog or JFrame.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface FloatingComponent {

    String getTitle();

    void setTitle(String title);

    Icon getIcon();

    void setIcon(Icon title);

    Component getContent();

    void setContent(Component component);

    DockableComponent getOriginator();

    void setOriginator(DockableComponent originator);

    Rectangle getBounds();

    void setBounds(Rectangle bounds);

    void show();

    void close();

    void addWindowListener(WindowListener l);

    void removeWindowListener(WindowListener l);
}
