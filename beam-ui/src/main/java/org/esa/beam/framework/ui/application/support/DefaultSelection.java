package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.Selection;

/**
 * Interface for a selection.
*/
public class DefaultSelection implements Selection {
    public static final Selection EMPTY = new DefaultSelection(null);

    private final Object[] elements;

    public DefaultSelection(Object element) {
        this.elements = element != null ? new Object[]{element} : new Object[0];
    }

    public DefaultSelection(Object[] elements) {
        this.elements = elements != null ? elements.clone() : new Object[0];
    }

    public boolean isEmpty() {
        return elements.length == 0;
    }

    public Object getFirstElement() {
        return elements.length > 0 ?  elements[0] : null;
    }

    public int getElementCount() {
        return elements.length;
    }

    public Object getElement(int index) {
        return elements[index];
    }

    public Object[] getElements() {
        return elements.clone();
    }
}