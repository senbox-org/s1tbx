package org.esa.beam.framework.ui;

public interface Disposable {
    /**
     * Releases all of the resources used by this view, its subcomponents, and all of its owned children.
     */
    void dispose();
}
