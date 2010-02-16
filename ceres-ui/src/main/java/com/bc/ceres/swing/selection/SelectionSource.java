package com.bc.ceres.swing.selection;

/**
 * A selection source holds a current selection and reports selection change events
 * by emitting them to interested {@link SelectionChangeListener selection change listener}s.
 * <p/>
 * This interface may be implemented by clients.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface SelectionSource extends SelectionChangeEmitter {
    /**
     * @return The current selection. The selection may be empty, but never {@code null}.
     *
     * @see Selection#EMPTY
     * @see Selection#isEmpty()
     */
    Selection getSelection();
}
