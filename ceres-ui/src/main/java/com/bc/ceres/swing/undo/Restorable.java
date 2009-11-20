package com.bc.ceres.swing.undo;

// todo - why is AbstractUndoableEdit a Serializable?
// todo - make Restorable a Serializable for the same reason?
// todo - make the memento a Serializable for the same reason?

/**
 * An object implementing this interface can create a memento by which it's
 * state can be restored after the object has been changed.
 */
public interface Restorable {
    Object createMemento();

    void setMemento(Object memento);
}
