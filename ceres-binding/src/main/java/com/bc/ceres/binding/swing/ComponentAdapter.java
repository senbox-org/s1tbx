package com.bc.ceres.binding.swing;

import javax.swing.JComponent;

public abstract class ComponentAdapter {
    private Binding binding;

    public Binding getBinding() {
        return binding;
    }

    public final void setBinding(Binding binding) {
        if (this.binding != null) {
            throw new IllegalStateException("this.binding != null");
        }
        this.binding = binding;
    }

    public abstract JComponent getPrimaryComponent();

    public abstract void bindComponents();

    public abstract void unbindComponents();

    public abstract void adjustComponents();
}
