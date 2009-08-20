package com.bc.ceres.binding.swing;

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.core.Assert;

/**
 * Represents a problem of a {@link com.bc.ceres.binding.swing.Binding} which may occur
 * when transferring data from a Swing component into the the bound property
 * ({@link com.bc.ceres.binding.ValueModel ValueModel}).
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.10
 */
public class BindingProblem {
    private final Binding binding;
    private final BindingException cause;

    /**
     * Constructor.
     *
     * @param binding The binding which has this problem.
     * @param cause   The cause of the problem.
     */
    public BindingProblem(Binding binding, BindingException cause) {
        Assert.notNull(binding, "binding");
        Assert.notNull(cause, "cause");
        this.binding = binding;
        this.cause = cause;
    }

    /**
     * @return The binding which has this problem.
     */
    public Binding getBinding() {
        return binding;
    }

    /**
     * @return The cause of the problem.
     */
    public BindingException getCause() {
        return cause;
    }

    @Override
    public int hashCode() {
        return getBinding().hashCode() + getCause().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof BindingProblem) {
            BindingProblem other = (BindingProblem) obj;
            return this.getBinding() == other.getBinding()
                    && this.getCause().equals(other.getCause());
        }
        return false;
    }
}
