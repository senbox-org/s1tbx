package com.bc.ceres.swing.binding;

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
     * Called when a problem has been reported on a {@link Binding} of a {@link BindingContext}.
     *
     * @param newProblem The new problem.
     * @param oldProblem The old problem, may be {@code null} if no problem existed before.
     */
    void problemReported(BindingProblem newProblem, BindingProblem oldProblem);

    /**
     * Called when a problem has been cleared in a {@link Binding} of a {@link BindingContext}.
     *
     * @param oldProblem The old problem.
     */
    void problemCleared(BindingProblem oldProblem);
}
