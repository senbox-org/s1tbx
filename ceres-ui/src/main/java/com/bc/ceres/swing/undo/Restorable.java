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

package com.bc.ceres.swing.undo;


/**
 * An object implementing this interface can create a memento by which it's
 * state can be restored after the object has been changed.
 * <p>
 * The interface is used to realize the GoF Memento Design Pattern.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Restorable {
    /**
     * Creates a {@code memento} which contains all state information used to restore this object at a
     * later point in time by calling {@link #setMemento(Object) setMemento(memento)}.
     *
     * @return A memento, or {@code null} if a memento cannot be created.
     */
    Object createMemento();

    /**
     * Restores this object's state from the given memento object.
     * <p>
     * This method will always (at least) accept the memento objects
     * created by the {@link #createMemento()} method.
     *
     * @param memento A memento object.
     */
    void setMemento(Object memento);
}
