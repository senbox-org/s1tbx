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
