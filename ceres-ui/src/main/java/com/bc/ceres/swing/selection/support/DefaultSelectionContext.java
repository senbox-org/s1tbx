package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.AbstractSelectionContext;

/**
 * A default implementation of the {@link com.bc.ceres.swing.selection.SelectionContext SelectionContext} interface.
 * This class is actually only useful for testing purposes. Real world implementations
 * of a {@code SelectionContext} will most likely adapt to the selections
 * emitted by dedicated GUI components.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class DefaultSelectionContext extends AbstractSelectionContext {
    private Selection selection;

    public DefaultSelectionContext() {
        this(null);
    }

    public DefaultSelectionContext(Object selectionSource) {
        super(selectionSource);
        selection = Selection.EMPTY;
    }

    @Override
    public Selection getSelection() {
        return selection;
    }

    @Override
    public void setSelection(Selection selection) {
        if (!this.selection.equals(selection)) {
            this.selection = selection;
            fireSelectionChange(selection);
        }
    }

}