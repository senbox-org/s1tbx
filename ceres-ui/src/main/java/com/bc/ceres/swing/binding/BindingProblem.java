package com.bc.ceres.swing.binding;

import com.bc.ceres.binding.BindingException;

/**
 * Represents a problem of a {@link com.bc.ceres.swing.binding.Binding} which may occur
 * when transferring data from a Swing component into the the bound
 * {@link com.bc.ceres.binding.Property Property}.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.10
 */
public interface BindingProblem {
    /**
     * @return The binding which has (or had) this problem.
     */
    Binding getBinding();

    /**
     * @return The cause of the problem.
     */
    BindingException getCause();
}
