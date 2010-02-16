package com.bc.ceres.swing.selection;

/**
 * An event indicating that a selection or selection context change has occurred.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class SelectionChangeEvent extends java.util.EventObject {
    private final SelectionContext selectionContext;
    private final Selection selection;

    /**
     * Constructs a selection event.
     *
     * @param source           The object that originated the event.
     * @param selectionContext The selection context in which the selection event took place.
     * @param selection        The selection.
     */
    public SelectionChangeEvent(Object source, SelectionContext selectionContext, Selection selection) {
        super(source);
        this.selectionContext = selectionContext;
        this.selection = selection;
    }

    /**
     * @return The selection context in which the selection event took place.
     */
    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    /**
     * @return The selection.
     */
    public Selection getSelection() {
        return selection;
    }
}

