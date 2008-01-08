package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.Selection;

/**
 * Interface for a selection.
 */
public class DefaultSelection implements Selection {
    public static final DefaultSelection EMPTY = new DefaultSelection(null);

    private final Object[] elements;

    public DefaultSelection(Object element) {
        this.elements = element != null ? new Object[]{element} : null;
    }

    public DefaultSelection(Object[] elements) {
        this.elements = (elements != null && elements.length > 0) ? elements.clone() : null;
    }

    public boolean isEmpty() {
        return elements == null;
    }

    public Object getFirstElement() {
        return elements != null ? elements[0] : null;
    }

    public Object[] getElements() {
        return elements != null ? elements.clone() : new Object[0];
    }

    public int getElementCount() {
        return elements != null ? elements.length : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Selection) {
            final Selection other = (Selection) obj;
            if (isEmpty() && other.isEmpty()) {
                return true;
            }
            if (isEmpty() || other.isEmpty()) {
                return false;
            }
            if (getElementCount() != other.getElementCount()) {
                return false;
            }
            final Object[] otherElements = other.getElements();
            for (int i = 0; i < elements.length; i++) {
                final Object thisElement = elements[i];
                final Object otherElement = otherElements[i];
                if (!(otherElement != null && otherElement.equals(thisElement) ||
                        thisElement != null && thisElement.equals(otherElement))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = getElementCount();
        if (elements != null) {
            for (Object element : elements) {
                hash += (element != null) ? element.hashCode() : 17;
            }
        }
        return hash;
    }
}