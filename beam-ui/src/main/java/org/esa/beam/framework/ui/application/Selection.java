package org.esa.beam.framework.ui.application;

/**
 * Interface for a selection.
 */
public interface Selection {

    Selection NULL = new Selection(){
        public boolean isEmpty() {
            return true;
        }

        public int getElementCount() {
            return 0;
        }

        public Object getFirstElement() {
            return null;
        }

        public Object getElement(int index) {
            return null;
        }
    };

    /**
     * @return whether this selection is empty.
     */
    boolean isEmpty();

    /**
     * @return the number of elements in this selection.
     */
    int getElementCount();

    /**
     * @return the first element in this selection, or null  if the selection is empty.
     */
    Object getFirstElement();

    /**
     * @param index The element index.
     * @return The elements in this selection at the given index.
     */
    Object getElement(int index);
}
