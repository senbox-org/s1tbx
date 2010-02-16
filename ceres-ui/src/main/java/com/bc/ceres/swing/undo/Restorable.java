package com.bc.ceres.swing.undo;

// todo - why is AbstractUndoableEdit a Serializable?
// todo - make Restorable a Serializable for the same reason?
// todo - make the memento a Serializable for the same reason?

/**
 * An object implementing this interface can create a memento by which it's
 * state can be restored after the object has been changed.
 * <p/>
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
     * <p/>
     * This method will always (at least) accept the memento objects
     * created by the {@link #createMemento()} method.
     *
     * @param memento A memento object.
     */
    void setMemento(Object memento);
}
