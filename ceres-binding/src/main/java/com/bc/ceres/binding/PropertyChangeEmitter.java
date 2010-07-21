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

package com.bc.ceres.binding;

import java.beans.PropertyChangeListener;

/**
 * Something that emits property change events.
 */
public interface PropertyChangeEmitter {

    /**
     * Adds a property change listener to this emitter.
     *
     * @param l The listener.
     */
    void addPropertyChangeListener(PropertyChangeListener l);

    /**
     * Adds a dedicated property change listener to this emitter.
     *
     * @param name The property name.
     * @param l    The listener.
     */
    void addPropertyChangeListener(String name, PropertyChangeListener l);

    /**
     * Removes a property change listener from this emitter.
     *
     * @param l The listener.
     */
    void removePropertyChangeListener(PropertyChangeListener l);

    /**
     * Removes a dedicated property change listener from this emitter.
     *
     * @param name The property name.
     * @param l    The listener.
     */
    void removePropertyChangeListener(String name, PropertyChangeListener l);
}
