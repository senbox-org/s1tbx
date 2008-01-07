package org.esa.beam.framework.ui.application;

/**
 * Interface for a selection.
 */
public interface Selection {

    /**
     * @return whether this selection is empty.
     */
    boolean isEmpty();

    /**
     * @return the first element in this selection, or null if the selection is empty.
     */
    Object getFirstElement();

    /**
     * @return The elements as an array. If the selection is empty, an empty array is returned.
     */
    Object[] getElements();

    /**
     * @return The number of elements.
     */
    int getElementCount();
}
