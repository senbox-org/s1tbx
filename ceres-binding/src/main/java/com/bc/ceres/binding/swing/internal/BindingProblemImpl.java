package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.binding.swing.BindingProblem;
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.core.Assert;

class BindingProblemImpl implements BindingProblem {
    private final Binding binding;
    private final BindingException cause;

    public BindingProblemImpl(Binding binding, BindingException cause) {
        Assert.notNull(binding, "binding");
        Assert.notNull(cause, "cause");
        this.binding = binding;
        this.cause = cause;
    }

    @Override
    public Binding getBinding() {
        return binding;
    }

    @Override
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
        } else if (obj instanceof BindingProblemImpl) {
            BindingProblem other = (BindingProblem) obj;
            return this.getBinding() == other.getBinding()
                    && this.getCause().equals(other.getCause());
        }
        return false;
    }
}
