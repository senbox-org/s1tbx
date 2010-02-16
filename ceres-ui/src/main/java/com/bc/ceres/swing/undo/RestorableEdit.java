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