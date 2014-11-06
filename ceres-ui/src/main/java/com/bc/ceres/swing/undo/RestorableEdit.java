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

import com.bc.ceres.swing.undo.Restorable;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A very simple {@code UndoableEdit} which is acts upon a {@link Restorable}
 * representing the changed object and the memento of that object before it was changed.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class RestorableEdit extends AbstractUndoableEdit {
    private Restorable changedObject;
    private Object memento;
    private String presentationName;

    public RestorableEdit(Restorable changedObject, Object memento) {
        this(changedObject, memento, "");
    }

    public RestorableEdit(Restorable changedObject, Object memento, String presentationName) {
        this.changedObject = changedObject;
        this.memento = memento;
        this.presentationName = presentationName;
    }

    public Restorable getChangedObject() {
        return changedObject;
    }

    @Override
    public String getPresentationName() {
        return presentationName;
    }

    @Override
    public void die() {
        super.die();
        changedObject = null;
        memento = null;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        rememberMemento();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        rememberMemento();
    }

    protected void rememberMemento() {
        Object newMemento = changedObject.createMemento();
        changedObject.setMemento(memento);
        memento = newMemento;
    }
}