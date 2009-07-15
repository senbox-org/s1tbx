package com.bc.ceres.binding.swing;

import com.bc.ceres.core.Assert;
import com.bc.ceres.binding.BindingException;

public class BindingProblem {
    private final Binding binding;
    private final BindingException cause;

    public BindingProblem(Binding binding, BindingException cause) {
        Assert.notNull(binding, "binding");
        Assert.notNull(cause, "cause");
        this.binding = binding;
        this.cause = cause;
    }

    public Binding getBinding() {
        return binding;
    }

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
