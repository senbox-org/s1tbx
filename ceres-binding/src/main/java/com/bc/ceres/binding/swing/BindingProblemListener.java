package com.bc.ceres.binding.swing;

/**
 * A listener used to observe problems which may occur in a {@link BindingContext},
 * e.g. when a user edits a Swing component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.10
 */
public interface BindingProblemListener {
    /**
     * Called when a problem occurred in a {@link BindingContext}.
     * @param problem The problem.
     */
    void problemOccurred(BindingProblem problem);
}
